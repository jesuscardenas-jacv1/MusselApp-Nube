package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Qué pasó al sincronizar, para explicárselo al usuario. */
data class ResultadoSincronizacion(
    /** Muestras que llegaron a la nube. */
    val enviadas: Int,
    /** Muestras creadas o cambiadas en el backend que se trajeron al teléfono (también cuentan en [enviadas] al subirse). */
    val recibidas: Int = 0,
    /** El backend, que este teléfono ya había usado, ahora no responde. */
    val backendSinRespuesta: Boolean = false,
)

/** Acceso a las muestras registradas en el dispositivo. */
interface MuestraRepository {
    val muestras: StateFlow<List<Muestra>>

    /** Obtiene una muestra por su ID como flujo reactivo. */
    fun obtenerPorId(id: Long): Flow<Muestra?>

    /**
     * Registra el dictamen del [supervisor] (se identifica por su RUT).
     * Lanza una excepción si no se puede guardar; quien llama debe avisar al usuario.
     */
    suspend fun actualizarEstado(
        id: Long,
        nuevoEstado: EstadoMuestra,
        comentario: String?,
        supervisor: Usuario,
    )

    /** Agrega una nueva muestra al repositorio y devuelve su id. */
    suspend fun agregarMuestra(muestra: Muestra): Long

    /** Envía al servidor las muestras guardadas localmente y devuelve cuántas se enviaron. */
    suspend fun sincronizar(): Int

    /** Igual que [sincronizar], con el detalle para el mensaje del botón. */
    suspend fun sincronizarConDetalle(): ResultadoSincronizacion = ResultadoSincronizacion(sincronizar())

    /** Cuántas muestras siguen sin subir a la nube. */
    suspend fun contarPendientes(): Int = muestras.value.count { !it.sincronizada }
}
