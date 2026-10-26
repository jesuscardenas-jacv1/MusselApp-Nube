package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.local.aModelo
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.NubeSync
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

/**
 * Repositorio de muestras sobre la base Room. Reemplaza a [FakeMuestraRepository]
 * en la app; el Fake queda para las pruebas y las vistas previas.
 */
class RoomMuestraRepository(
    private val db: MusselDatabase,
    alcance: CoroutineScope,
    private val nube: NubeSync? = null,
    private val alcanceNube: CoroutineScope = alcance,
    private val latenciaSincronizacionMs: Long = 1500,
    private val alRevisar: (Muestra, EstadoMuestra, String?) -> Unit = { _, _, _ -> },
) : MuestraRepository {

    private val muestraDao = db.muestraDao()
    private val usuarioDao = db.usuarioDao()

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
                    val local = muestras.value.firstOrNull { it.id == v.muestraId } ?: return@launch
                    if (local.estado != EstadoMuestra.PENDIENTE) return@launch
                    muestraDao.registrarRevision(v.muestraId, v.estado, v.comentario, DatosIniciales.RUT_SUPERVISOR, v.revisadaEn)
                    muestraDao.marcarSincronizada(v.muestraId)
                    alRevisar(local, v.estado, v.comentario)
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
                observacion = muestra.observacion,
                estado = muestra.estado,
                registradaEn = muestra.registradaEn,
                sincronizada = false,
                fotoRuta = muestra.fotoRuta,
            ),
        )
        // Envío inmediato en segundo plano; si falla, queda pendiente para "Sincronizar"
        if (nube != null) alcanceNube.launch { subirMuestra(nuevoId) }
    }

    private suspend fun subirMuestra(id: Long): Boolean {
        val m = muestraDao.observarPorId(id).first()?.aModelo() ?: return false
        val ok = nube?.subir(m) == true
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

    /** Acepta un RUT con o sin formato y devuelve el RUT normalizado si el usuario existe. */
    private suspend fun buscarRut(rut: String): String? =
        usuarioDao.buscarPorRut(RutValidator.normalizar(rut))?.rut
}
