package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra.CORREGIDO
import cl.aldemar.musselapp.data.model.EstadoMuestra.PENDIENTE
import cl.aldemar.musselapp.data.model.EstadoMuestra.VALIDADO
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.ui.revision.RevisionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Revisión del supervisor: cola de pendientes, selección y reglas del dictamen (Validar / Corregir). */
@OptIn(ExperimentalCoroutinesApi::class)
class RevisionViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private val supervisor = Usuario("222222222", "Diego Muñoz", Rol.SUPERVISOR)

    private val repo = MuestrasEnMemoria(
        listOf(
            muestraDePrueba(1, PENDIENTE),
            muestraDePrueba(2, VALIDADO),
            muestraDePrueba(3, PENDIENTE, centroId = "calbuco"),
            muestraDePrueba(4, CORREGIDO),
            muestraDePrueba(5, PENDIENTE),
        ),
    )

    @Test
    fun colaTieneSoloPendientes_ySeleccionaLaPrimera() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals(listOf(1L, 3L, 5L), estado.muestrasPendientes.map { it.id })
        assertEquals(1L, estado.muestraSeleccionadaId)
        assertEquals(1L, estado.muestraActual?.id)
    }

    @Test
    fun mostrarCentro_filtraLaCola() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.mostrarCentro("calbuco")
        assertEquals(listOf(3L), vm.uiState.value.muestrasPendientes.map { it.id })
        // La seleccionada (1) ya no está en la cola: pasa a la primera disponible
        assertEquals(3L, vm.uiState.value.muestraSeleccionadaId)
    }

    @Test
    fun seleccionarMuestra_limpiaLoEscrito() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.elegirEstado(CORREGIDO)
        vm.actualizarComentario("Revisar el conteo")
        vm.seleccionarMuestra(5)
        val estado = vm.uiState.value
        assertEquals(5L, estado.muestraSeleccionadaId)
        assertNull(estado.estadoElegido)
        assertEquals("", estado.comentarioTecnico)
    }

    @Test
    fun enfocarMuestra_dejaSeleccionadaLaQueVieneDelDetalle() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.elegirEstado(VALIDADO)
        vm.enfocarMuestra(1) // ya seleccionada: no se pierde lo elegido
        assertEquals(VALIDADO, vm.uiState.value.estadoElegido)
        vm.enfocarMuestra(5)
        assertEquals(5L, vm.uiState.value.muestraSeleccionadaId)
        assertNull(vm.uiState.value.estadoElegido)
    }

    @Test
    fun motivosRapidos_seAgreganAlComentario() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.agregarMotivoRapido("Talla irregular")
        vm.agregarMotivoRapido("Repetir conteo")
        assertEquals("Talla irregular. Repetir conteo", vm.uiState.value.comentarioTecnico)
    }

    @Test
    fun validarDictamen_sinElegir_pideElegir() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        assertFalse(vm.validarDictamen())
        assertEquals("Selecciona un dictamen: Validar o Corregir", vm.uiState.value.errorDictamen)
        vm.elegirEstado(VALIDADO)
        assertNull(vm.uiState.value.errorDictamen)
        assertTrue(vm.validarDictamen())
    }

    @Test
    fun corregirSinComentarioSuficiente_noSeGuarda() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.elegirEstado(CORREGIDO)
        vm.actualizarComentario("mal")
        assertFalse(vm.validarDictamen())
        assertNotNull(vm.uiState.value.errorComentario)
        vm.guardarDictamen(supervisor)
        assertNull(repo.ultimoDictamen)
        vm.actualizarComentario("Repetir el conteo del tramo")
        assertNull(vm.uiState.value.errorComentario)
        assertTrue(vm.validarDictamen())
    }

    @Test
    fun guardarSinElegir_noLlamaAlRepositorio() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.guardarDictamen(supervisor)
        assertNull(repo.ultimoDictamen)
        assertNotNull(vm.uiState.value.errorDictamen)
    }

    @Test
    fun validar_guardaElDictamen_yLaMuestraSaleDeLaCola() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        var exito = false
        vm.elegirEstado(VALIDADO)
        vm.guardarDictamen(supervisor) { exito = true }
        assertTrue(exito)
        assertEquals(Triple(1L, VALIDADO, null), repo.ultimoDictamen)
        assertEquals(supervisor, repo.ultimoSupervisor)
        val estado = vm.uiState.value
        assertFalse(estado.guardando)
        assertEquals("Dictamen registrado para la Muestra 001", estado.mensajeToast)
        assertEquals(listOf(3L, 5L), estado.muestrasPendientes.map { it.id })
        assertEquals(3L, estado.muestraSeleccionadaId)
        vm.limpiarMensaje()
        assertNull(vm.uiState.value.mensajeToast)
    }

    @Test
    fun corregir_guardaElComentarioSinEspacios() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.elegirEstado(CORREGIDO)
        vm.actualizarComentario("  Repetir el conteo del tramo  ")
        vm.guardarDictamen(supervisor)
        assertEquals(Triple(1L, CORREGIDO, "Repetir el conteo del tramo"), repo.ultimoDictamen)
    }

    @Test
    fun errorAlGuardar_avisaYConservaLoEscrito() = runTest(main.dispatcher) {
        repo.fallarAlDictaminar = true
        val vm = RevisionViewModel(repo)
        var exito = false
        vm.elegirEstado(CORREGIDO)
        vm.actualizarComentario("Repetir el conteo del tramo")
        vm.guardarDictamen(supervisor) { exito = true }
        val estado = vm.uiState.value
        assertFalse(exito)
        assertFalse(estado.guardando)
        assertEquals("No se pudo guardar el dictamen: disco lleno", estado.mensajeToast)
        assertEquals(CORREGIDO, estado.estadoElegido)
        assertEquals("Repetir el conteo del tramo", estado.comentarioTecnico)
    }

    @Test
    fun colaVacia_noHayMuestraQueDictaminar() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(MuestrasEnMemoria(listOf(muestraDePrueba(2, VALIDADO))))
        assertNull(vm.uiState.value.muestraActual)
        assertFalse(vm.validarDictamen())
        vm.elegirEstado(VALIDADO)
        vm.guardarDictamen(supervisor)
        assertNull(vm.uiState.value.mensajeToast)
    }

    @Test
    fun abrirYCerrarLaFotoAmpliada() = runTest(main.dispatcher) {
        val vm = RevisionViewModel(repo)
        vm.alternarModalZoom(true)
        assertTrue(vm.uiState.value.modalZoomAbierto)
        vm.alternarModalZoom(false)
        assertFalse(vm.uiState.value.modalZoomAbierto)
    }
}
