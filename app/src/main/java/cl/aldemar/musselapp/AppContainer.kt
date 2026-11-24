package cl.aldemar.musselapp

import android.content.Context
import cl.aldemar.musselapp.data.clima.ClimaRepository
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.ubicacion.UbicadorGps
import cl.aldemar.musselapp.data.nube.AvisoRevision
import cl.aldemar.musselapp.data.nube.BackendSync
import cl.aldemar.musselapp.data.nube.Conectividad
import cl.aldemar.musselapp.data.nube.EnvioPendientes
import cl.aldemar.musselapp.data.nube.Notificador
import cl.aldemar.musselapp.data.nube.NubeCompuesta
import cl.aldemar.musselapp.data.nube.NubeSync
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.repository.AuthRepository
import cl.aldemar.musselapp.data.repository.BloqueoLogin
import cl.aldemar.musselapp.data.repository.CatalogoRepository
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

    private val servicioNube: ServicioNube by lazy {
        val firebase = NubeSync(appContext)
        if (BackendSync.ACTIVO) NubeCompuesta(primaria = BackendSync(appContext), secundaria = firebase) else firebase
    }

    val muestraRepository: MuestraRepository by lazy { RoomMuestraRepository(database, alcanceApp, nube = servicioNube, clima = ClimaRepository(),
            alRevisar = { muestra, estado, comentario ->
                notificador.avisarRevision(muestra, estado, comentario)
                avisos.update { it + AvisoRevision(muestra.codigo, muestra.codigoLinea, estado, comentario) }
            },
            alEliminar = { muestra ->
                notificador.avisarEliminacion(muestra)
                AlmacenFotos.borrar(appContext, muestra.fotoRuta)
            }) }

    /** Trenes y líneas de cada centro para el formulario de Nueva muestra. */
    val catalogoRepository: CatalogoRepository by lazy { CatalogoRepository(database.catalogoDao()) }

    /** Posición del teléfono al registrar una muestra (los ViewModels no tienen Context). */
    val ubicador: UbicadorGps by lazy { UbicadorGps(appContext) }

    val conectividad: Conectividad by lazy { Conectividad(appContext) }

    init {
        // Al volver la conexión se envían solas las muestras que quedaron pendientes (con la app abierta)
        alcanceApp.launch {
            conectividad.enLinea.collect { enLinea ->
                if (enLinea) runCatching { muestraRepository.sincronizar() }
            }
        }
        // Cada vez que aparece una muestra pendiente nueva (guardada, revisada o que quedó de antes al abrir la app),
        // se deja un envío en WorkManager: sale apenas haya internet, aunque se cierre la app
        alcanceApp.launch {
            var anteriores = emptySet<Long>()
            muestraRepository.muestras.collect { lista ->
                val pendientes = lista.filterNot { it.sincronizada }.mapTo(HashSet()) { it.id }
                if ((pendientes - anteriores).isNotEmpty()) runCatching { EnvioPendientes.programar(appContext) }
                anteriores = pendientes
            }
        }
    }

    val authRepository: AuthRepository by lazy {
        RoomAuthRepository(
            database.usuarioDao(),
            bloqueo = BloqueoLogin(PreferenciasIntentos(appContext)),
            esperarDatos = database::esperarDatosIniciales,
        )
    }

    init {
        // Abre la base y carga los datos iniciales mientras el usuario completa el login,
        // así las pantallas no arrancan con listas vacías ni el primer login choca con la semilla
        alcanceApp.launch { database.esperarDatosIniciales() }
    }

    /** Borra una foto de la app (los ViewModels no tienen Context). */
    fun borrarFoto(ruta: String?) = AlmacenFotos.borrar(appContext, ruta)
}
