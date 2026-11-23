package cl.aldemar.musselapp.data.nube

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Estado de la conexión a internet del teléfono, observable desde la interfaz. */
class Conectividad(context: Context) {

    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _enLinea = MutableStateFlow(hayInternet())
    val enLinea: StateFlow<Boolean> = _enLinea

    init {
        try {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _enLinea.value = manager.getNetworkCapabilities(network).conInternet()
                }
                // Se perdió la red por defecto y no hay otra. No se consulta a Android: en este instante
                // todavía puede responder con la red que se acaba de cortar (por ejemplo, en modo avión)
                override fun onLost(network: Network) { _enLinea.value = false }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _enLinea.value = caps.conInternet()
                }
            })
        } catch (e: Exception) {
            // Sin permiso o sin servicio: se asume conexión y no se muestra el aviso
            _enLinea.value = true
        }
    }

    private fun hayInternet(): Boolean {
        val m = manager ?: return true
        return m.getNetworkCapabilities(m.activeNetwork).conInternet()
    }

    /** Conectado a una red que Android comprobó que llega a internet (no basta con tener señal). */
    private fun NetworkCapabilities?.conInternet(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
