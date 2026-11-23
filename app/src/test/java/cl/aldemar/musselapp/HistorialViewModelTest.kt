package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra.CORREGIDO
import cl.aldemar.musselapp.data.model.EstadoMuestra.PENDIENTE
import cl.aldemar.musselapp.data.model.EstadoMuestra.VALIDADO
import cl.aldemar.musselapp.ui.historial.FiltroEstado
import cl.aldemar.musselapp.ui.historial.HistorialViewModel
import cl.aldemar.musselapp.ui.historial.Orden
import cl.aldemar.musselapp.ui.historial.Periodo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Historial: filtros, búsqueda, orden, centro de la sesión y sincronización. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private val ahora = ahoraDeHoy()

    private val repo = MuestrasEnMemoria(
        listOf(
            muestraDePrueba(1, VALIDADO, tren = "A", linea = 1, talla = 60, registradaEn = ahora - 3 * 60_000),
            muestraDePrueba(2, PENDIENTE, tren = "B", linea = 4, talla = 70, registradaEn = ahora - 2 * 60_000, sincronizada = false),
            muestraDePrueba(3, CORREGIDO, tren = "B", linea = 4, talla = 50, registradaEn = ahora - 60_000),
            muestraDePrueba(4, VALIDADO, centroId = "quellon", tren = "C", linea = 7, registradaEn = ahora),
            muestraDePrueba(5, VALIDADO, tren = "A", linea = 2, diasAtras = 10),
        ),
    )

    private fun TestScope.vm() = HistorialViewModel(repo).also { vm ->
        backgroundScope.launch(main.dispatcher) { vm.uiState.collect {} }
    }

    private fun HistorialViewModel.ids() = uiState.value.muestras.map { it.id }

    @Test
    fun porDefecto_muestraLaJornadaDeHoy_recientesPrimero() = runTest(main.dispatcher) {
        val vm = vm()
        assertEquals(listOf(4L, 3L, 2L, 1L), vm.ids())
        assertEquals(1, vm.uiState.value.pendientesSincronizar)
    }

    @Test
    fun periodoTodo_incluyeDiasAnteriores() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onPeriodoChange(Periodo.TODO)
        assertEquals(5, vm.uiState.value.muestras.size)
        assertEquals(Periodo.TODO, vm.uiState.value.criterios.periodo)
    }

    @Test
    fun filtroPorEstado_yConteosDeCadaPestana() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onFiltroChange(FiltroEstado.VALIDADOS)
        assertEquals(listOf(4L, 1L), vm.ids())
        val conteos = vm.uiState.value.conteos
        assertEquals(4, conteos[FiltroEstado.TODOS])
        assertEquals(2, conteos[FiltroEstado.VALIDADOS])
        assertEquals(1, conteos[FiltroEstado.PENDIENTES])
        assertEquals(1, conteos[FiltroEstado.CORREGIDOS])
    }

    @Test
    fun busquedaPorTrenYLinea() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onBusquedaChange("tren B")
        assertEquals(listOf(3L, 2L), vm.ids())
        assertEquals(60.0, vm.uiState.value.tallaPromedioMm!!, 0.01)
    }

    @Test
    fun alternarOrden_muestraLasAntiguasPrimero() = runTest(main.dispatcher) {
        val vm = vm()
        vm.onAlternarOrden()
        assertEquals(Orden.ANTIGUAS, vm.uiState.value.criterios.orden)
        assertEquals(listOf(1L, 2L, 3L, 4L), vm.ids())
        vm.onAlternarOrden()
        assertEquals(Orden.RECIENTES, vm.uiState.value.criterios.orden)
    }

    @Test
    fun restablecerFiltros_conservaElCentroDeLaSesion() = runTest(main.dispatcher) {
        val vm = vm()
        vm.mostrarCentro("dalcahue")
        vm.onFiltroChange(FiltroEstado.PENDIENTES)
        vm.onBusquedaChange("tren B")
        assertEquals(listOf(2L), vm.ids())
        vm.onRestablecerFiltros()
        val criterios = vm.uiState.value.criterios
        assertEquals("dalcahue", criterios.centroId)
        assertEquals("", criterios.busqueda)
        assertEquals(FiltroEstado.TODOS, criterios.filtro)
        // La muestra de Quellón sigue fuera: es de otro centro
        assertEquals(listOf(3L, 2L, 1L), vm.ids())
    }

    @Test
    fun sincronizar_avisaCuantasSeEnviaron() = runTest(main.dispatcher) {
        val vm = vm()
        vm.sincronizar()
        assertEquals("1 muestra sincronizada", vm.uiState.value.mensaje)
        assertEquals(0, vm.uiState.value.pendientesSincronizar)
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensaje)
    }

    @Test
    fun sincronizar_sinInternet_avisaQueSigueEnCola() = runTest(main.dispatcher) {
        repo.sinInternet = true
        val vm = vm()
        vm.sincronizar()
        assertEquals("No se pudo enviar: 1 muestra en cola. Se enviará sola al volver internet", vm.uiState.value.mensaje)
        assertEquals(1, vm.uiState.value.pendientesSincronizar)
    }

    @Test
    fun sincronizar_conError_avisaYTermina() = runTest(main.dispatcher) {
        repo.fallarAlSincronizar = true
        val vm = vm()
        vm.sincronizar()
        assertFalse(vm.uiState.value.sincronizando)
        assertEquals("No se pudo sincronizar. Las muestras siguen guardadas en el equipo", vm.uiState.value.mensaje)
    }
}
