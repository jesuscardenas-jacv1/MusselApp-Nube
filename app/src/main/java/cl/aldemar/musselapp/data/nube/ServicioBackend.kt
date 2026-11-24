package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.ValoresOperario

/**
 * Backend propio (Spring Boot con Swagger). Es opcional: si está apagado, la app sigue igual con Room y
 * Firestore, y ninguna muestra queda en cola por su culpa. Cuando está disponible, el teléfono hace de
 * puente: lo que cambia en el servidor llega a Room y a Firestore, y lo del portal llega al servidor.
 */
interface ServicioBackend {

    /** Muestras que cambiaron en el servidor desde la última vez; vacía si está apagado. Nunca lanza. */
    suspend fun cambios(): List<CambioBackend>

    /** Recuerda que este cambio ya se revisó y, si se importó, en qué muestra local quedó. */
    fun revisado(cambio: CambioBackend, idLocal: Long?)

    /** Envía las muestras que el servidor no tiene o tiene desactualizadas. Nunca lanza. */
    suspend fun enviar(muestras: List<Muestra>)

    /** Borra del servidor una muestra que el supervisor eliminó en el portal. Nunca lanza. */
    suspend fun eliminar(idLocal: Long)
}

/** Muestra tal como quedó en el servidor. */
data class CambioBackend(
    /** Identificador único de la muestra en cualquier servidor (el id de la tabla no sirve: cada computador numera por su cuenta). */
    val idCliente: String,
    /** Servidor del que viene y versión de la muestra en él. */
    val servidor: String,
    val version: Long,
    /** Id en Room si ya está en este teléfono; null si se creó en el servidor y hay que importarla. */
    val idLocal: Long?,
    val centroId: String,
    val tren: String,
    val linea: Int,
    val profundidadM: Int,
    val tallaMm: Int,
    val conteoIndM: Int,
    val cholgasIndM: Int?,
    val tramoCm: Int?,
    val observacion: String,
    val estado: EstadoMuestra,
    val registradaEn: Long,
    val operadorRut: String,
    val revisadaEn: Long?,
    val comentario: String?,
    /** Lo que registró el operario, si el supervisor corrigió los valores en el servidor. */
    val valoresOperario: ValoresOperario?,
)
