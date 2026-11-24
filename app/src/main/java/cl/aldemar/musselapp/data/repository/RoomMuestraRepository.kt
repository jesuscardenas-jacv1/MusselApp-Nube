package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.clima.FuenteClima
import cl.aldemar.musselapp.data.local.aModelo
import cl.aldemar.musselapp.data.local.conClima
import cl.aldemar.musselapp.data.local.conPosicion
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.ProteccionDictamen
import cl.aldemar.musselapp.data.nube.CambioBackend
import cl.aldemar.musselapp.data.nube.ServicioBackend
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
    /** Muestra que el supervisor borró en el portal y ya se borró de Room (para avisar y borrar su foto). */
    private val alEliminar: (Muestra) -> Unit = {},
    /** Clima de la muestra; null en las pruebas que no usan red. */
    private val clima: FuenteClima? = null,
    private val reloj: () -> Long = System::currentTimeMillis,
    /** Backend propio, opcional; null si no se usa. */
    private val backend: ServicioBackend? = null,
) : MuestraRepository {

    private val muestraDao = db.muestraDao()
    private val usuarioDao = db.usuarioDao()

    /** Los dictámenes de la nube se aplican uno a la vez. */
    private val aplicandoDictamen = Mutex()

    /** El backend se consulta de a uno: dos sincronizaciones a la vez importarían dos veces lo mismo. */
    private val conBackend = Mutex()

    /** Lista viva: Room vuelve a emitir cada vez que cambia una tabla de la consulta. */
    override val muestras: StateFlow<List<Muestra>> =
        muestraDao.observarTodas()
            .map { filas -> filas.map { it.aModelo() } }
            .stateIn(alcance, SharingStarted.Eagerly, emptyList())

    init {
        // Dictámenes del supervisor llegados desde la nube: se guardan en Room y la UI se actualiza sola
        nube?.escucharVeredictos(alEliminar = { id -> eliminarDesdeNube(id) }) { v ->
            alcanceNube.launch {
                try {
                    // De a uno: si la nube repite el mismo dictamen, el segundo ya encuentra la muestra revisada
                    aplicandoDictamen.withLock {
                        // Se consulta Room y no la lista en memoria: al abrir la app, la nube puede entregar
                        // su snapshot antes de que Room emita la primera lista, y el dictamen se perdería
                        val local = muestraDao.observarPorId(v.muestraId).first()?.aModelo() ?: return@launch
                        // Además de las pendientes, se acepta el dictamen de la nube sobre una muestra revisada
                        // en este teléfono que aún no se subió: el de la nube llegó primero y es el que vale
                        if (!ProteccionDictamen.debeAplicarRemoto(local.estado, local.sincronizada, v.estado)) return@launch
                        muestraDao.registrarRevision(v.muestraId, v.estado, v.comentario, DatosIniciales.RUT_SUPERVISOR, v.revisadaEn)
                        // Si el supervisor corrigió valores en el portal, el teléfono muestra los mismos
                        v.corregidos?.let { muestraDao.aplicarCorreccion(v.muestraId, it.conteoIndM, it.tallaMm, it.cholgasIndM) }
                        muestraDao.marcarSincronizada(v.muestraId)
                        alRevisar(local, v.estado, v.comentario)
                    }
                    enviarAlBackend()
                } catch (e: Exception) {
                    android.util.Log.w("RoomMuestraRepo", "No se pudo aplicar el dictamen", e)
                }
            }
        }
    }

    /**
     * El supervisor borró la muestra en el portal: se borra también del teléfono y se avisa.
     * Va por el mismo candado que los dictámenes para no cruzarse con uno que esté llegando.
     */
    private fun eliminarDesdeNube(id: Long) {
        alcanceNube.launch {
            try {
                aplicandoDictamen.withLock {
                    val local = muestraDao.observarPorId(id).first()?.aModelo() ?: return@launch
                    if (muestraDao.eliminar(id) > 0) alEliminar(local)
                }
                backend?.eliminar(id)
            } catch (e: Exception) {
                android.util.Log.w("RoomMuestraRepo", "No se pudo borrar la muestra eliminada en el portal", e)
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
        // El dictamen hecho en el teléfono se sube de inmediato para que el portal lo vea y no acepte otro.
        // Igual que al guardar, no se espera a la red: si falla, la muestra queda pendiente para "Sincronizar"
        if (nube != null) alcanceNube.launch { subirMuestra(id); enviarAlBackend() }
    }

    /** El id de [muestra] se ignora: lo genera la base al insertar. */
    override suspend fun agregarMuestra(muestra: Muestra): Long {
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
                tramoCm = muestra.tramoCm,
                observacion = muestra.observacion,
                estado = muestra.estado,
                registradaEn = muestra.registradaEn,
                sincronizada = false,
                fotoRuta = muestra.fotoRuta,
            ).conPosicion(muestra.posicion).conClima(muestra.clima),
        )
        // Guardar nunca espera a la red: el clima y el envío van en segundo plano.
        // Si fallan, la muestra queda pendiente para "Sincronizar"
        if (nube != null || clima != null) alcanceNube.launch { subirMuestra(nuevoId); enviarAlBackend() }
        return nuevoId
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
        // Primero se trae lo del backend: si cambió algo, queda pendiente y se sube a Firestore enseguida
        traerDelBackend()
        val enviadas = muestraDao.pendientesDeSincronizar().count { subirMuestra(it) }
        enviarAlBackend()
        return enviadas
    }

    /** Le envía al backend lo que le falta. No afecta la cola: si está apagado, no pasa nada. */
    private suspend fun enviarAlBackend() {
        val servicio = backend ?: return
        conBackend.withLock { servicio.enviar(muestraDao.observarTodas().first().map { it.aModelo() }) }
    }

    /** Aplica en Room lo que cambió en el backend (por ejemplo, un POST o un PUT hecho en Swagger). */
    private suspend fun traerDelBackend() {
        val servicio = backend ?: return
        conBackend.withLock {
            for (cambio in servicio.cambios()) {
                val idLocal = aplicandoDictamen.withLock {
                    if (cambio.idLocal == null) importar(cambio) else aplicarCambio(cambio, cambio.idLocal)
                }
                servicio.revisado(cambio, idLocal)
            }
        }
    }

    /** Muestra creada en el servidor. Queda pendiente para que también llegue a Firestore y al portal. */
    private suspend fun importar(c: CambioBackend): Long? {
        val lineaId = db.catalogoDao().buscarLineaId(c.centroId, c.tren, c.linea) ?: return null
        val registro = c.valoresOperario
        val id = muestraDao.insertar(
            MuestraEntity(
                lineaId = lineaId,
                operadorRut = buscarRut(c.operadorRut) ?: DatosIniciales.RUT_OPERARIO,
                profundidadM = c.profundidadM,
                tallaMm = registro?.tallaMm ?: c.tallaMm,
                conteoIndM = registro?.conteoIndM ?: c.conteoIndM,
                cholgasIndM = if (registro != null) registro.cholgasIndM else c.cholgasIndM,
                tramoCm = c.tramoCm,
                observacion = c.observacion,
                estado = EstadoMuestra.PENDIENTE,
                registradaEn = c.registradaEn,
                sincronizada = false,
                fotoRuta = null,
            ),
        )
        aplicarDictamen(c, id)
        return id
    }

    /**
     * Cambio en el servidor de una muestra de este teléfono. Solo se toca mientras siga pendiente: si ya
     * tiene dictamen, vale el primero y lo que diga el backend no lo pisa.
     */
    private suspend fun aplicarCambio(c: CambioBackend, id: Long): Long? {
        val local = muestraDao.observarPorId(id).first()?.aModelo() ?: return null
        // Mismo id pero otra muestra (por ejemplo, si se borró la base del teléfono): no se toca
        if (local.registradaEn != c.registradaEn || local.estado != EstadoMuestra.PENDIENTE) return id
        val conteo = c.valoresOperario?.conteoIndM ?: c.conteoIndM
        val talla = c.valoresOperario?.tallaMm ?: c.tallaMm
        val cholgas = if (c.valoresOperario != null) c.valoresOperario.cholgasIndM else c.cholgasIndM
        val cambia = local.conteoIndM != conteo || local.tallaMm != talla || local.cholgasIndM != cholgas ||
            local.profundidadM != c.profundidadM || local.tramoCm != c.tramoCm || local.observacion != c.observacion
        if (cambia) muestraDao.actualizarDesdeRemoto(id, c.profundidadM, talla, conteo, cholgas, c.tramoCm, c.observacion)
        if (aplicarDictamen(c, id)) alRevisar(local, c.estado, c.comentario)
        return id
    }

    private suspend fun aplicarDictamen(c: CambioBackend, id: Long): Boolean {
        if (c.estado == EstadoMuestra.PENDIENTE) return false
        // registrarRevision la deja sin sincronizar: el dictamen se sube a Firestore y el portal lo ve
        muestraDao.registrarRevision(id, c.estado, c.comentario, DatosIniciales.RUT_SUPERVISOR, c.revisadaEn ?: reloj())
        if (c.valoresOperario != null) muestraDao.aplicarCorreccion(id, c.conteoIndM, c.tallaMm, c.cholgasIndM)
        return true
    }

    // Se consulta Room y no la lista en memoria, que puede ir un paso atrás de la base
    override suspend fun contarPendientes(): Int = muestraDao.pendientesDeSincronizar().size

    companion object {
        /** Tiempo durante el cual todavía se pide el clima de una muestra guardada sin internet. */
        const val PLAZO_CLIMA_MS = 2 * 60 * 60 * 1000L
    }

    /** Acepta un RUT con o sin formato y devuelve el RUT normalizado si el usuario existe. */
    private suspend fun buscarRut(rut: String): String? =
        usuarioDao.buscarPorRut(RutValidator.normalizar(rut))?.rut
}
