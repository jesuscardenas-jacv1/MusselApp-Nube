package cl.aldemar.musselapp.data.nube

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cl.aldemar.musselapp.MusselApplication
import cl.aldemar.musselapp.data.repository.MuestraRepository
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** Qué hace WorkManager después de un intento de envío. */
enum class ResultadoEnvio { LISTO, REINTENTAR, RENDIRSE }

/**
 * Envío de las muestras pendientes con WorkManager (Req. 5 del EA4 y paso 5 de la defensa):
 * WorkManager lo ejecuta cuando hay internet, aunque la app esté cerrada o el teléfono se
 * haya reiniciado. Complementa al reenvío que ya hace la app abierta al volver la conexión.
 *
 * La decisión está separada del Worker para probarla en la JVM, sin Android.
 */
object EnvioPendientes {

    /** Nombre único del trabajo: nunca hay dos envíos en cola a la vez. */
    const val NOMBRE = "envio-muestras-pendientes"

    /** Después de tantos intentos fallidos se deja para la próxima vez que haya pendientes nuevas o se abra la app. */
    const val MAX_INTENTOS = 8

    /** Espera antes del primer intento: si la app está abierta, su envío inmediato casi siempre gana. */
    private const val ESPERA_INICIAL_S = 30L

    /**
     * Sube lo pendiente y decide si el trabajo terminó, si se reintenta o si se deja.
     * [alFallar] recibe el error del envío (el Worker lo anota en el log).
     */
    suspend fun ejecutar(
        repositorio: MuestraRepository,
        intento: Int,
        alFallar: (Exception) -> Unit = {},
    ): ResultadoEnvio {
        try {
            repositorio.sincronizar()
        } catch (e: CancellationException) {
            throw e // WorkManager detuvo el trabajo (por ejemplo, se cortó internet): no es un error
        } catch (e: Exception) {
            alFallar(e)
        }
        // Si no se puede contar, se asume que queda algo: es mejor reintentar que perder una muestra
        val quedan = try {
            repositorio.contarPendientes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            1
        }
        return when {
            quedan == 0 -> ResultadoEnvio.LISTO
            intento + 1 >= MAX_INTENTOS -> ResultadoEnvio.RENDIRSE
            else -> ResultadoEnvio.REINTENTAR
        }
    }

    /**
     * Deja en cola un envío que corre apenas haya internet. Si ya hay uno en cola, se mantiene
     * (KEEP): ese envío sube todas las pendientes, incluidas las nuevas.
     */
    fun programar(contexto: Context) {
        val solicitud = OneTimeWorkRequestBuilder<EnvioPendientesWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.KEEP, solicitud)
    }
}

/** Worker que ejecuta [EnvioPendientes] con el repositorio de la app. */
class EnvioPendientesWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        // MusselApplication.onCreate crea el contenedor antes de que WorkManager ejecute cualquier trabajo
        val resultado = EnvioPendientes.ejecutar(MusselApplication.contenedor.muestraRepository, runAttemptCount) { e ->
            Log.w(TAG, "No se pudo sincronizar", e)
        }
        Log.i(TAG, "Intento ${runAttemptCount + 1}: $resultado")
        return when (resultado) {
            ResultadoEnvio.LISTO -> Result.success()
            ResultadoEnvio.REINTENTAR -> Result.retry()
            ResultadoEnvio.RENDIRSE -> Result.failure()
        }
    }

    private companion object {
        const val TAG = "EnvioPendientes"
    }
}
