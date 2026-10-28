package cl.aldemar.musselapp

import android.content.Context
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.nube.AvisoRevision
import cl.aldemar.musselapp.data.nube.Conectividad
import cl.aldemar.musselapp.data.nube.Notificador
import cl.aldemar.musselapp.data.nube.NubeSync
import cl.aldemar.musselapp.data.repository.AuthRepository
import cl.aldemar.musselapp.data.repository.BloqueoLogin
import cl.aldemar.musselapp.data.repository.PreferenciasIntentos
import cl.aldemar.musselapp.data.repository.MuestraRepository
import cl.aldemar.musselapp.data.repository.RoomAuthRepository
import cl.aldemar.musselapp.data.repository.RoomMuestraRepository
import cl.aldemar.musselapp.util.AlmacenFotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Contenedor manual de dependencias: crea una sola vez la base de datos
 * y los repositorios que usan todas las pantallas.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Alcance de la app para trabajos que deben sobrevivir a las pantallas (ej. datos iniciales). */
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: MusselDatabase by lazy { MusselDatabase.crear(context, alcanceApp) }

    /** Dictámenes recién llegados; la pantalla los muestra como aviso hasta que el operario los cierra. */
    val avisos = MutableStateFlow<List<AvisoRevision>>(emptyList())

    /** El aviso pidió abrir el Historial; la navegación lo consume. */
    val irAHistorial = MutableStateFlow(false)

    private val notificador by lazy { Notificador(appContext) }

    val muestraRepository: MuestraRepository by lazy { RoomMuestraRepository(database, alcanceApp, nube = NubeSync(appContext),
            alRevisar = { muestra, estado, comentario ->
                notificador.avisarRevision(muestra, estado, comentario)
                avisos.update { it + AvisoRevision(muestra.nombre, muestra.codigoLinea, estado, comentario) }
            }) }

    val conectividad: Conectividad by lazy { Conectividad(appContext) }

    init {
        // Al volver la conexión se envían solas las muestras que quedaron pendientes
        alcanceApp.launch {
            conectividad.enLinea.collect { enLinea ->
                if (enLinea) runCatching { muestraRepository.sincronizar() }
            }
        }
    }

    val authRepository: AuthRepository by lazy {
        RoomAuthRepository(database.usuarioDao(), bloqueo = BloqueoLogin(PreferenciasIntentos(appContext)))
    }

    /** Borra una foto de la app (los ViewModels no tienen Context). */
    fun borrarFoto(ruta: String?) = AlmacenFotos.borrar(appContext, ruta)
}
