package cl.aldemar.musselapp.data.clima

import android.util.Log
import cl.aldemar.musselapp.data.model.Clima
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** De dónde sale el clima de una muestra. Las pruebas usan una fuente falsa. */
fun interface FuenteClima {
    /** Condiciones actuales en el punto; null si no hay internet o el servicio falla. Nunca lanza. */
    suspend fun obtener(lat: Double, lon: Double): Clima?
}

/** Clima desde Open-Meteo: pide el tiempo y el mar en paralelo. */
class ClimaRepository(
    private val api: ClimaApi = crearApi(),
    private val reloj: () -> Long = System::currentTimeMillis,
) : FuenteClima {

    override suspend fun obtener(lat: Double, lon: Double): Clima? =
        withTimeoutOrNull(TIEMPO_MAX_MS) {
            coroutineScope {
                val tiempo = async { intentar("tiempo") { api.tiempo(lat = lat, lon = lon).current } }
                val mar = async { intentar("mar") { api.mar(lat = lat, lon = lon).current } }
                combinar(tiempo.await(), mar.await(), reloj())
            }
        }

    private suspend fun <T> intentar(nombre: String, llamada: suspend () -> T?): T? =
        try {
            llamada()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener el $nombre", e)
            null
        }

    companion object {
        private const val TAG = "ClimaRepository"
        private const val TIEMPO_MAX_MS = 8_000L

        /** Une las dos respuestas; null si ninguna trajo datos. */
        fun combinar(tiempo: TiempoActual?, mar: MarActual?, ahora: Long): Clima? {
            if (tiempo == null && mar == null) return null
            return Clima(
                tempAireC = tiempo?.temperatura,
                vientoKn = tiempo?.viento,
                rachaKn = tiempo?.racha,
                vientoDir = tiempo?.direccion?.roundToInt(),
                codigo = tiempo?.codigo,
                olaM = mar?.ola,
                tempAguaC = mar?.temperaturaAgua,
                mareaM = mar?.nivelMar,
                obtenidoEn = ahora,
            )
        }

        fun crearApi(): ClimaApi = Retrofit.Builder()
            .baseUrl(ClimaApi.URL_BASE)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClimaApi::class.java)
    }
}
