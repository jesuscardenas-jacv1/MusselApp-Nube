package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.repository.FakeAuthRepository
import cl.aldemar.musselapp.ui.login.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Login (RUT, PIN y centro, sin elegir perfil): limpieza del RUT y el PIN, validaciones y resultado del ingreso. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private fun vm() = LoginViewModel(FakeAuthRepository(latenciaMs = 0))

    private fun LoginViewModel.completar(rut: String, pin: String) {
        onRutChange(rut)
        onPasswordChange(pin)
    }

    @Test
    fun rut_guardaSoloDigitosYK_conLargoMaximo() {
        val vm = vm()
        vm.onRutChange("11.111.111-1")
        assertEquals("111111111", vm.uiState.value.rut)
        vm.onRutChange("1234567890123")
        assertEquals(9, vm.uiState.value.rut.length)
        vm.onRutChange("12345678k")
        assertEquals("12345678K", vm.uiState.value.rut)
    }

    @Test
    fun rut_conKEnMedio_seIgnora() {
        val vm = vm()
        vm.onRutChange("1234")
        vm.onRutChange("12K34")
        assertEquals("1234", vm.uiState.value.rut)
    }

    @Test
    fun pin_soloDigitos_maximoOcho() {
        val vm = vm()
        vm.onPasswordChange("12a3-4567890")
        assertEquals("12345678", vm.uiState.value.password)
    }

    @Test
    fun ingresarVacio_marcaAmbosCampos() {
        val vm = vm()
        vm.ingresar()
        assertEquals("Ingrese su RUT", vm.uiState.value.rutError)
        assertEquals("Ingrese su PIN", vm.uiState.value.passwordError)
    }

    @Test
    fun rutInvalido_seMarca_yEscribirQuitaElError() {
        val vm = vm()
        vm.completar("111111112", "1234")
        vm.ingresar()
        assertEquals("RUT inválido", vm.uiState.value.rutError)
        vm.onRutChange("111111111")
        assertNull(vm.uiState.value.rutError)
    }

    @Test
    fun credencialesCorrectas_ingresaYBorraElPin() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar("22.222.222-2", "1234")
        vm.ingresar()
        val estado = vm.uiState.value
        assertEquals("Diego Muñoz", estado.usuario?.nombre)
        assertEquals("", estado.password)
        assertFalse(estado.cargando)
    }

    @Test
    fun pinIncorrecto_muestraElMensajeDelRepositorio() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar("111111111", "9999")
        vm.ingresar()
        assertNull(vm.uiState.value.usuario)
        assertEquals("Contraseña incorrecta", vm.uiState.value.mensajeError)
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun rutNoRegistrado_noIngresa() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar("12.345.678-5", "1234")
        vm.ingresar()
        assertNull(vm.uiState.value.usuario)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun alIngresar_esperaLaAnimacionDeBienvenida() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar("111111111", "1234")
        vm.ingresar()
        // Con el usuario listo, la pantalla principal espera a que termine la animación
        assertNotNull(vm.uiState.value.usuario)
        assertFalse(vm.uiState.value.transicionTerminada)
        vm.onTransicionTerminada()
        assertTrue(vm.uiState.value.transicionTerminada)
    }

    @Test
    fun cerrarSesion_vuelveAlInicio_conservandoElCentro() = runTest(main.dispatcher) {
        val vm = vm()
        val centro = CentroCultivo.disponibles.last()
        vm.onCentroChange(centro)
        vm.completar("111111111", "1234")
        vm.ingresar()
        assertNotNull(vm.uiState.value.usuario)
        vm.onCerrarSesion()
        val estado = vm.uiState.value
        assertNull(estado.usuario)
        assertFalse(estado.transicionTerminada)
        assertEquals("", estado.rut)
        assertEquals(centro, estado.centro)
    }
}
