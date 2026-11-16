package cl.aldemar.musselapp.data.clima

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Servicios de Open-Meteo (gratis, sin clave). El tiempo y el mar están en dos servidores
 * distintos, por eso cada llamada recibe su dirección completa.
 */
interface ClimaApi {

    @GET
    suspend fun tiempo(
        @Url url: String = URL_TIEMPO,
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") variables: String = "temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,weather_code",
        @Query("wind_speed_unit") unidadViento: String = "kn",
        @Query("timezone") zona: String = "auto",
    ): RespuestaTiempo

    @GET
    suspend fun mar(
        @Url url: String = URL_MAR,
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") variables: String = "wave_height,sea_surface_temperature,sea_level_height_msl",
        @Query("timezone") zona: String = "auto",
    ): RespuestaMar

    companion object {
        const val URL_BASE = "https://api.open-meteo.com/"
        const val URL_TIEMPO = "https://api.open-meteo.com/v1/forecast"
        const val URL_MAR = "https://marine-api.open-meteo.com/v1/marine"
    }
}

data class RespuestaTiempo(val current: TiempoActual?)

data class TiempoActual(
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("wind_speed_10m") val viento: Double?,
    @SerializedName("wind_gusts_10m") val racha: Double?,
    @SerializedName("wind_direction_10m") val direccion: Double?,
    @SerializedName("weather_code") val codigo: Int?,
)

data class RespuestaMar(val current: MarActual?)

data class MarActual(
    @SerializedName("wave_height") val ola: Double?,
    @SerializedName("sea_surface_temperature") val temperaturaAgua: Double?,
    @SerializedName("sea_level_height_msl") val nivelMar: Double?,
)
