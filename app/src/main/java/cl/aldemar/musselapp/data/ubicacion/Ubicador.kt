package cl.aldemar.musselapp.data.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import cl.aldemar.musselapp.data.model.Posicion
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** De dónde sale la posición del teléfono. Las pruebas usan un ubicador falso. */
fun interface Ubicador {
    /** Posición actual; null sin permiso, con el GPS apagado o sin señal. Nunca lanza. */
    suspend fun posicionActual(): Posicion?
}

/** Posición con los servicios de ubicación de Google (GPS + redes). */
class UbicadorGps(context: Context) : Ubicador {

    private val appContext = context.applicationContext
    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    fun tienePermiso(): Boolean = PERMISOS.any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // se comprueba en tienePermiso()
    override suspend fun posicionActual(): Posicion? {
        if (!tienePermiso()) return null
        return try {
            withTimeoutOrNull(TIEMPO_MAX_MS) {
                suspendCancellableCoroutine { cont ->
                    val cancelacion = CancellationTokenSource()
                    cont.invokeOnCancellation { cancelacion.cancel() }
                    cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
                        .addOnSuccessListener { l ->
                            val posicion = l?.let {
                                Posicion(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy.toDouble() else null)
                            }
                            if (cont.isActive) cont.resume(posicion)
                        }
                        .addOnFailureListener {
                            Log.w(TAG, "No se pudo leer la posición", it)
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la posición", e)
            null
        }
    }

    companion object {
        private const val TAG = "UbicadorGps"
        private const val TIEMPO_MAX_MS = 10_000L

        val PERMISOS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
