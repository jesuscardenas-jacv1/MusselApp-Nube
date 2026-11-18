package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.repository.MuestraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * viewModelScope corre en Dispatchers.Main, que en la JVM de las pruebas no existe.
 * Esta regla lo reemplaza por un dispatcher de prueba durante cada test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDispatcherPrincipal(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Repositorio en memoria para probar los ViewModels: las muestras se fijan en cada prueba,
 * responde sin demoras y puede fallar a pedido para probar los mensajes de error.
 */
class MuestrasEnMemoria(iniciales: List<Muestra> = emptyList()) : MuestraRepository {

    private val _muestras = MutableStateFlow(iniciales)
    override val muestras: StateFlow<List<Muestra>> = _muestras

    var fallarAlSincronizar = false
    var fallarAlDictaminar = false
    var llamadasSincronizar = 0
        private set

    /** Último dictamen recibido: id, estado, comentario y supervisor. */
    var ultimoDictamen: Triple<Long, EstadoMuestra, String?>? = null
        private set
    var ultimoSupervisor: Usuario? = null
        private set

    override fun obtenerPorId(id: Long): Flow<Muestra?> = _muestras.map { lista -> lista.find { it.id == id } }

    override suspend fun actualizarEstado(id: Long, nuevoEstado: EstadoMuestra, comentario: String?, supervisor: Usuario) {
        if (fallarAlDictaminar) error("disco lleno")
        ultimoDictamen = Triple(id, nuevoEstado, comentario)
        ultimoSupervisor = supervisor
        _muestras.update { lista ->
            lista.map {
                if (it.id == id) {
                    it.copy(estado = nuevoEstado, comentarioSupervisor = comentario, revisadaPor = supervisor.nombre, sincronizada = false)
                } else it
            }
        }
    }

    override suspend fun agregarMuestra(muestra: Muestra) = _muestras.update { listOf(muestra) + it }

    override suspend fun sincronizar(): Int {
        llamadasSincronizar++
        if (fallarAlSincronizar) error("sin conexión")
        val pendientes = _muestras.value.count { !it.sincronizada }
        _muestras.update { lista -> lista.map { it.copy(sincronizada = true) } }
        return pendientes
    }

    /** Cambia las muestras como si llegaran nuevas desde la base. */
    fun reemplazar(lista: List<Muestra>) {
        _muestras.value = lista
    }
}

private const val DIA_MS = 24 * 60 * 60 * 1000L

/** Muestra mínima para las pruebas; registrada ahora salvo que se indique otra fecha. */
fun muestraDePrueba(
    id: Long,
    estado: EstadoMuestra = EstadoMuestra.PENDIENTE,
    centroId: String = "dalcahue",
    tren: String = "A",
    linea: Int = 1,
    talla: Int = 60,
    conteo: Int = 400,
    sincronizada: Boolean = true,
    diasAtras: Int = 0,
    registradaEn: Long = System.currentTimeMillis() - diasAtras * DIA_MS,
) = Muestra(
    id = id, centroId = centroId, linea = linea, tren = tren, profundidadM = 3,
    tallaMm = talla, conteoIndM = conteo, observacion = "Mytilus chilensis", estado = estado,
    registradaEn = registradaEn, sincronizada = sincronizada, operador = "Carla Vera",
)
