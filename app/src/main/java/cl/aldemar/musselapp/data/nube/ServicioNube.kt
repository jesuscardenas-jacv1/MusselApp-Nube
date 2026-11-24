package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra

/**
 * Muestra creada o modificada fuera del teléfono (por ejemplo desde Swagger en el backend Spring Boot)
 * que se sincroniza hacia Room al consultar el servidor.
 */
data class CambioRemoto(
    val idRemoto: String,
    /** Id en Room si esta muestra ya pertenece a este teléfono; null si fue creada externamente. */
    val idLocalExistente: Long?,
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
    val operador: String,
    val operadorRut: String,
    val revisadaPor: String?,
    val revisadaEn: Long?,
    val comentarioSupervisor: String?,
)

/**
 * Lo que el repositorio necesita de la nube. [NubeSync] lo implementa sobre Firestore,
 * [BackendSync] sobre el backend Spring Boot con Retrofit, [NubeCompuesta] combina ambos,
 * y las pruebas usan implementaciones falsas para no depender de la red.
 */
interface ServicioNube {

    /**
     * Sube (o actualiza) la muestra, con su dictamen si se revisó en el teléfono.
     * Devuelve true si el servidor la confirmó. Devuelve false si no hubo conexión, y también si la
     * nube ya tenía un dictamen distinto: ese dictamen se entrega por [escucharVeredictos] y la muestra
     * queda pendiente hasta que el teléfono lo adopte.
     */
    suspend fun subir(m: Muestra): Boolean

    /**
     * Escucha en vivo los dictámenes del supervisor para las muestras de este teléfono.
     * [alEliminar] recibe el id local de cada muestra de este teléfono que el supervisor borró en el portal.
     */
    fun escucharVeredictos(alEliminar: (Long) -> Unit = {}, alAplicar: (VeredictoNube) -> Unit)

    /**
     * Consulta cambios hechos desde fuera del teléfono (por ejemplo un POST o PUT desde Swagger UI).
     * Por defecto no devuelve cambios (Firestore usa [escucharVeredictos]).
     */
    suspend fun descargarCambios(): List<CambioRemoto> = emptyList()

    /** Registra qué id local en Room quedó asociado a un id remoto importado, para evitar duplicados. */
    fun registrarIdLocal(idRemoto: String, idLocal: Long) {}
}
