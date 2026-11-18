package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra.VALIDADO
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.ui.detalle.DetalleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Detalle de una muestra: carga por id, se actualiza con el dictamen y maneja la foto y los avisos. */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private val repo = MuestrasEnMemoria(listOf(muestraDePrueba(7), muestraDePrueba(8)))

    @Test
    fun cargaLaMuestraPedida() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(7, repo)
        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals(7L, estado.muestra?.id)
    }

    @Test
    fun idInexistente_terminaDeCargarSinMuestra() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(99, repo)
        assertFalse(vm.uiState.value.cargando)
        assertNull(vm.uiState.value.muestra)
    }

    @Test
    fun seActualizaCuandoLlegaElDictamen() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(7, repo)
        repo.actualizarEstado(7, VALIDADO, null, Usuario("222222222", "Diego Muñoz", Rol.SUPERVISOR))
        assertEquals(VALIDADO, vm.uiState.value.muestra?.estado)
        assertEquals("Diego Muñoz", vm.uiState.value.muestra?.revisadaPor)
    }

    @Test
    fun muestraBorradaEnElPortal_desapareceDelDetalle() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(7, repo)
        repo.reemplazar(listOf(muestraDePrueba(8)))
        assertNull(vm.uiState.value.muestra)
    }

    @Test
    fun abrirYCerrarLaFotoAmpliada() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(7, repo)
        vm.alternarModalZoom(true)
        assertTrue(vm.uiState.value.modalZoomAbierto)
        vm.alternarModalZoom(false)
        assertFalse(vm.uiState.value.modalZoomAbierto)
    }

    @Test
    fun mensajeSeMuestraYSeLimpia() = runTest(main.dispatcher) {
        val vm = DetalleViewModel(7, repo)
        vm.mostrarMensaje("Ficha PDF generada")
        assertEquals("Ficha PDF generada", vm.uiState.value.mensajeToast)
        vm.limpiarMensaje()
        assertNull(vm.uiState.value.mensajeToast)
    }

    @Test
    fun factory_creaElViewModelConElIdYElRepositorio() = runTest(main.dispatcher) {
        val vm = DetalleViewModel.Factory(8, repo).create(DetalleViewModel::class.java)
        assertEquals(8L, vm.uiState.value.muestra?.id)
    }
}
