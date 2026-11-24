package cl.aldemar.musselapp.data.nube

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Cliente Retrofit para el backend propio en Spring Boot (`backend/`).
 * En dispositivo físico por USB usar `adb reverse tcp:8080 tcp:8080` con [URL_BASE].
 */
interface BackendApi {

    @GET("api/muestras")
    suspend fun listarMuestras(): List<MuestraRemotaDto>

    @POST("api/muestras")
    suspend fun crearOActualizarMuestra(
        @Header("X-Rol") rol: String,
        @Body muestra: MuestraEnviarDto,
    ): MuestraRemotaDto

    @PUT("api/muestras/{id}")
    suspend fun actualizarMuestra(
        @Path("id") id: Long,
        @Header("X-Rol") rol: String,
        @Body muestra: MuestraEnviarDto,
    ): MuestraRemotaDto

    @PUT("api/muestras/{id}/foto")
    suspend fun subirFoto(
        @Path("id") id: Long,
        @Header("X-Rol") rol: String,
        @Body foto: FotoEnviarDto,
    ): FotoRemotaDto

    @POST("api/muestras/{id}/revisiones")
    suspend fun dictaminar(
        @Path("id") id: Long,
        @Header("X-Rol") rol: String,
        @Body revision: RevisionEnviarDto,
    ): MuestraRemotaDto

    @GET("api/reportes/resumen")
    suspend fun resumenReportes(): ReporteResumenDto

    companion object {
        /** Apunta al backend Spring Boot local vía `adb reverse tcp:8080 tcp:8080`. */
        const val URL_BASE = "http://127.0.0.1:8080/"
    }
}

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
    val codigo: String,
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
    val operador: String?,
    val operadorRut: String?,
    val revisadaPor: String?,
    val revisadaEn: Long?,
    val comentarioSupervisor: String?,
    val tieneFoto: Boolean = false,
    val actualizadaEn: Long = 0L,
)

data class FotoEnviarDto(
    val nombreArchivo: String,
    val tipoMime: String,
    val contenidoBase64: String,
)

data class FotoRemotaDto(
    val id: Long,
    val muestraId: Long,
    val nombreArchivo: String,
    val tipoMime: String,
    val tamanoBytes: Long,
)

data class RevisionEnviarDto(
    val estado: String,
    val comentario: String?,
    val supervisorNombre: String?,
    val supervisorRut: String?,
)

data class ReporteResumenDto(
    val totalMuestras: Long,
    val pendientes: Long,
    val validadas: Long,
    val corregidas: Long,
    val promedioConteoIndM: Double,
    val promedioTallaMm: Double,
)
