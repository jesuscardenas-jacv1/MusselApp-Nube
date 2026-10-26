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
                override fun onAvailable(network: Network) { _enLinea.value = true }
                override fun onLost(network: Network) { _enLinea.value = hayInternet() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _enLinea.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        } catch (e: Exception) {
            // Sin permiso o sin servicio: se asume conexión y no se muestra el aviso
            _enLinea.value = true
        }
    }

    private fun hayInternet(): Boolean {
        val m = manager ?: return true
        val caps = m.getNetworkCapabilities(m.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
