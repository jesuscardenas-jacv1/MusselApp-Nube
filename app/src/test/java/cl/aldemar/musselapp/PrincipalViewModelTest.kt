package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra.CORREGIDO
import cl.aldemar.musselapp.data.model.EstadoMuestra.PENDIENTE
import cl.aldemar.musselapp.data.model.EstadoMuestra.VALIDADO
import cl.aldemar.musselapp.ui.principal.PrincipalViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Pantalla Principal: resumen de la jornada, muestras recientes y sincronización manual. */
@OptIn(ExperimentalCoroutinesApi::class)
class PrincipalViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private val repo = MuestrasEnMemoria(
        listOf(
            muestraDePrueba(1, VALIDADO, linea = 1),
            muestraDePrueba(2, PENDIENTE, linea = 2, sincronizada = false),
            muestraDePrueba(3, CORREGIDO, linea = 2),
            muestraDePrueba(4, VALIDADO, centroId = "calbuco", linea = 5, sincronizada = false),
            // De días anteriores: no cuenta en el resumen de hoy, pero sí como pendiente de sincronizar
            muestraDePrueba(5, PENDIENTE, linea = 9, diasAtras = 3, sincronizada = false),
        ),
    )

    /** uiState se calcula solo mientras alguien lo observa (WhileSubscribed). */
    private fun TestScope.observar(vm: PrincipalViewModel) {
        backgroundScope.launch(main.dispatcher) { vm.uiState.collect {} }
    }

    @Test
    fun resumen_cuentaSoloLaJornadaDeHoy() = runTest(main.dispatcher) {
        val vm = PrincipalViewModel(repo)
        observar(vm)
        val resumen = vm.uiState.value.resumen
        assertEquals(4, resumen.totalMuestras)
        assertEquals(2, resumen.validadas)
        assertEquals(1, resumen.pendientes)
        assertEquals(1, resumen.corregidas)
        assertEquals(3, resumen.pendientesSincronizar)
    }

    @Test
    fun mostrarCentro_resumeSoloEseCentro_peroSincronizaTodos() = runTest(main.dispatcher) {
        val vm = PrincipalViewModel(repo)
        observar(vm)
        vm.mostrarCentro("calbuco")
        val estado = vm.uiState.value
        assertEquals(1, estado.resumen.totalMuestras)
        assertEquals(listOf(4L), estado.recientes.map { it.id })
        // La cola de envío es del teléfono, no del centro
        assertEquals(3, estado.resumen.pendientesSincronizar)
    }

    @Test
    fun recientes_sonLasTresMasNuevas() = runTest(main.dispatcher) {
        val ahora = System.currentTimeMillis()
        val repo = MuestrasEnMemoria((1L..5L).map { muestraDePrueba(it, registradaEn = ahora - (10 - it) * 60_000) })
        val vm = PrincipalViewModel(repo)
        observar(vm)
        assertEquals(listOf(5L, 4L, 3L), vm.uiState.value.recientes.map { it.id })
    }

    @Test
    fun sincronizar_enviaLasPendientesYAvisa() = runTest(main.dispatcher) {
        val vm = PrincipalViewModel(repo)
        observar(vm)
        vm.sincronizar()
        val estado = vm.uiState.value
        assertEquals("3 muestras sincronizadas", estado.mensaje)
        assertFalse(estado.sincronizando)
        assertEquals(0, estado.resumen.pendientesSincronizar)
    }

    @Test
    fun sincronizar_sinConexion_noDejaElIconoGirando() = runTest(main.dispatcher) {
        repo.fallarAlSincronizar = true
        val vm = PrincipalViewModel(repo)
        observar(vm)
        vm.sincronizar()
        val estado = vm.uiState.value
        assertFalse(estado.sincronizando)
        assertEquals("No se pudo sincronizar. Las muestras siguen guardadas en el equipo", estado.mensaje)
    }

    @Test
    fun onMensajeMostrado_limpiaElAviso() = runTest(main.dispatcher) {
        val vm = PrincipalViewModel(repo)
        observar(vm)
        vm.sincronizar()
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }
}
