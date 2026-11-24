package cl.aldemar.musselapp.data.nube

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cliente Retrofit del backend propio en Spring Boot (carpeta `backend/`).
 * Con el teléfono por USB: `adb reverse tcp:8080 tcp:8080`.
 */
interface BackendApi {

    @GET("api/servidor")
    suspend fun servidor(): ServidorDto

    @GET("api/muestras")
    suspend fun listarMuestras(@Query("idCliente") idCliente: String? = null): List<MuestraRemotaDto>

    /** Crea la muestra; si el servidor ya tiene ese idCliente la devuelve sin duplicarla ni pisarla. */
    @POST("api/muestras")
    suspend fun crearMuestra(@Header("X-Rol") rol: String, @Body muestra: MuestraEnviarDto): MuestraRemotaDto

    @PUT("api/muestras/{id}/foto")
    suspend fun subirFoto(@Path("id") id: Long, @Header("X-Rol") rol: String, @Body foto: FotoEnviarDto)

    @POST("api/muestras/{id}/revisiones")
    suspend fun dictaminar(@Path("id") id: Long, @Header("X-Rol") rol: String, @Body revision: RevisionEnviarDto): MuestraRemotaDto

    @DELETE("api/muestras/{id}")
    suspend fun eliminarMuestra(@Path("id") id: Long, @Header("X-Rol") rol: String): retrofit2.Response<Unit>

    companion object {
        const val URL_BASE = "http://127.0.0.1:8080/"
        const val OPERARIO = "OPERARIO"
        const val SUPERVISOR = "SUPERVISOR"
    }
}

data class ServidorDto(val id: String)

data class MuestraEnviarDto(
    val idCliente: String,
    val codigo: String,
    val centroId: String,
    val tren: String,
    val linea: Int,
    val profundidadM: Int,
    val tallaMm: Int,
    val conteoIndM: Int,
    val cholgasIndM: Int?,
    val tramoCm: Int?,
    val observacion: String,
    val registradaEn: Long,
    val operador: String,
    val operadorRut: String,
    val lat: Double?,
    val lon: Double?,
    val precisionM: Double?,
    val distanciaCentroM: Int?,
    val climaTempAireC: Double?,
    val climaVientoKn: Double?,
    val climaOlaM: Double?,
    val climaTempAguaC: Double?,
    val climaDescripcion: String?,
)

data class MuestraRemotaDto(
    val id: Long,
    val idCliente: String?,
    val centroId: String,
    val tren: String,
    val linea: Int,
    val profundidadM: Int,
    val tallaMm: Int,
    val conteoIndM: Int,
    val cholgasIndM: Int?,
    val tramoCm: Int?,
    val observacion: String?,
    val estado: String,
    val registradaEn: Long,
    val operadorRut: String?,
    val revisadaEn: Long?,
    val comentarioSupervisor: String?,
    val conteoOperario: Int?,
    val tallaOperario: Int?,
    val cholgasOperario: Int?,
    val tieneFoto: Boolean,
    val actualizadaEn: Long,
)

data class FotoEnviarDto(
    val nombreArchivo: String,
    val tipoMime: String,
    val contenidoBase64: String,
)

data class RevisionEnviarDto(
    val estado: String,
    val comentario: String?,
    val supervisorNombre: String?,
    val supervisorRut: String,
    /** Valores que corrigió el supervisor; null si dictaminó sin cambiarlos. */
    val conteoIndM: Int? = null,
    val tallaMm: Int? = null,
    val cholgasIndM: Int? = null,
)
