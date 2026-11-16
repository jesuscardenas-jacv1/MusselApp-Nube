package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.clima.FuenteClima
import cl.aldemar.musselapp.data.local.aModelo
import cl.aldemar.musselapp.data.local.conClima
import cl.aldemar.musselapp.data.local.conPosicion
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.util.RutValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repositorio de muestras sobre la base Room. Reemplaza a [FakeMuestraRepository]
 * en la app; el Fake queda para las pruebas y las vistas previas.
 */
class RoomMuestraRepository(
    private val db: MusselDatabase,
    alcance: CoroutineScope,
    /** Nube (Firestore en la app, una falsa en las pruebas); null para funcionar solo local. */
    private val nube: ServicioNube? = null,
    private val alcanceNube: CoroutineScope = alcance,
    private val latenciaSincronizacionMs: Long = 1500,
    private val alRevisar: (Muestra, EstadoMuestra, String?) -> Unit = { _, _, _ -> },
    /** Clima de la muestra; null en las pruebas que no usan red. */
    private val clima: FuenteClima? = null,
    private val reloj: () -> Long = System::currentTimeMillis,
) : MuestraRepository {

    private val muestraDao = db.muestraDao()
    private val usuarioDao = db.usuarioDao()

    /** Los dictámenes de la nube se aplican uno a la vez. */
    private val aplicandoDictamen = Mutex()

    /** Lista viva: Room vuelve a emitir cada vez que cambia una tabla de la consulta. */
    override val muestras: StateFlow<List<Muestra>> =
        muestraDao.observarTodas()
            .map { filas -> filas.map { it.aModelo() } }
            .stateIn(alcance, SharingStarted.Eagerly, emptyList())

    init {
        // Dictámenes del supervisor llegados desde la nube: se guardan en Room y la UI se actualiza sola
        nube?.escucharVeredictos { v ->
            alcanceNube.launch {
                try {
                    // De a uno: si la nube repite el mismo dictamen, el segundo ya encuentra la muestra revisada
                    aplicandoDictamen.withLock {
                        // Se consulta Room y no la lista en memoria: al abrir la app, la nube puede entregar
                        // su snapshot antes de que Room emita la primera lista, y el dictamen se perdería
                        val local = muestraDao.observarPorId(v.muestraId).first()?.aModelo() ?: return@launch
                        if (local.estado != EstadoMuestra.PENDIENTE) return@launch
                        muestraDao.registrarRevision(v.muestraId, v.estado, v.comentario, DatosIniciales.RUT_SUPERVISOR, v.revisadaEn)
                        muestraDao.marcarSincronizada(v.muestraId)
                        alRevisar(local, v.estado, v.comentario)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RoomMuestraRepo", "No se pudo aplicar el dictamen", e)
                }
            }
        }
    }

    override fun obtenerPorId(id: Long): Flow<Muestra?> =
        muestraDao.observarPorId(id).map { it?.aModelo() }

    override suspend fun actualizarEstado(
        id: Long,
        nuevoEstado: EstadoMuestra,
        comentario: String?,
        supervisor: Usuario,
    ) {
        // Se busca por RUT (único); el nombre puede repetirse entre usuarios
        val supervisorRut = buscarRut(supervisor.rut)
            ?: error("Supervisor no registrado: ${supervisor.rut}")
        muestraDao.registrarRevision(id, nuevoEstado, comentario, supervisorRut, System.currentTimeMillis())
    }

    /** El id de [muestra] se ignora: lo genera la base al insertar. */
    override suspend fun agregarMuestra(muestra: Muestra) {
        val lineaId = db.catalogoDao().buscarLineaId(muestra.centroId, muestra.tren, muestra.linea)
            ?: error("No existe la línea ${muestra.linea} del tren ${muestra.tren} en ${muestra.centroId}")
        val operadorRut = buscarRut(muestra.operadorRut)
            ?: error("Operario no registrado: ${muestra.operadorRut}")
        val nuevoId = muestraDao.insertar(
            MuestraEntity(
                lineaId = lineaId,
                operadorRut = operadorRut,
                profundidadM = muestra.profundidadM,
                tallaMm = muestra.tallaMm,
                conteoIndM = muestra.conteoIndM,
                cholgasIndM = muestra.cholgasIndM,
                observacion = muestra.observacion,
                estado = muestra.estado,
                registradaEn = muestra.registradaEn,
                sincronizada = false,
                fotoRuta = muestra.fotoRuta,
            ).conPosicion(muestra.posicion).conClima(muestra.clima),
        )
        // Guardar nunca espera a la red: el clima y el envío van en segundo plano.
        // Si fallan, la muestra queda pendiente para "Sincronizar"
        if (nube != null || clima != null) alcanceNube.launch { subirMuestra(nuevoId) }
    }

    /**
     * Pide el clima de una muestra que aún no lo tiene. Solo mientras es reciente: pasado el
     * plazo, el clima "actual" ya no es el del momento en que se tomó la muestra.
     */
    private suspend fun completarClima(m: Muestra): Muestra {
        val fuente = clima ?: return m
        if (m.clima != null || reloj() - m.registradaEn > PLAZO_CLIMA_MS) return m
        // Se consulta en la posición de la muestra; sin GPS, en el punto del centro
        val centro = CentroCultivo.porId(m.centroId)
        val lat = m.posicion?.lat ?: centro?.lat ?: return m
        val lon = m.posicion?.lon ?: centro?.lon ?: return m
        val c = fuente.obtener(lat, lon) ?: return m
        muestraDao.guardarClima(
            m.id, c.tempAireC, c.vientoKn, c.rachaKn, c.vientoDir, c.codigo, c.olaM, c.tempAguaC, c.mareaM, c.obtenidoEn,
        )
        return m.copy(clima = c)
    }

    private suspend fun subirMuestra(id: Long): Boolean {
        val guardada = muestraDao.observarPorId(id).first()?.aModelo() ?: return false
        val m = completarClima(guardada)
        if (nube == null) return false
        val ok = nube.subir(m)
        if (ok) muestraDao.marcarSincronizada(id)
        return ok
    }

    override suspend fun sincronizar(): Int {
        if (nube == null) {
            delay(latenciaSincronizacionMs) // Sin nube: simula el envío por el enlace satelital
            return muestraDao.marcarSincronizadas()
        }
        return muestraDao.pendientesDeSincronizar().count { subirMuestra(it) }
    }

    companion object {
        /** Tiempo durante el cual todavía se pide el clima de una muestra guardada sin internet. */
        const val PLAZO_CLIMA_MS = 2 * 60 * 60 * 1000L
    }

    /** Acepta un RUT con o sin formato y devuelve el RUT normalizado si el usuario existe. */
    private suspend fun buscarRut(rut: String): String? =
        usuarioDao.buscarPorRut(RutValidator.normalizar(rut))?.rut
}
