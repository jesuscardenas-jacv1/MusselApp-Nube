package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Rol
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

/** Login por rol: limpieza del RUT y el PIN, validaciones y resultado del ingreso. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private fun vm() = LoginViewModel(FakeAuthRepository(latenciaMs = 0))

    private fun LoginViewModel.completar(rol: Rol, rut: String, pin: String) {
        onRolChange(rol)
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
    fun ingresarSinPerfil_noHaceNada() {
        val vm = vm()
        vm.ingresar()
        assertNull(vm.uiState.value.rutError)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun ingresarVacio_marcaAmbosCampos() {
        val vm = vm()
        vm.onRolChange(Rol.OPERARIO)
        vm.ingresar()
        assertEquals("Ingrese su RUT", vm.uiState.value.rutError)
        assertEquals("Ingrese su PIN", vm.uiState.value.passwordError)
    }

    @Test
    fun rutInvalido_seMarca_yEscribirQuitaElError() {
        val vm = vm()
        vm.completar(Rol.OPERARIO, "111111112", "1234")
        vm.ingresar()
        assertEquals("RUT inválido", vm.uiState.value.rutError)
        vm.onRutChange("111111111")
        assertNull(vm.uiState.value.rutError)
    }

    @Test
    fun credencialesCorrectas_ingresaYBorraElPin() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar(Rol.SUPERVISOR, "22.222.222-2", "1234")
        vm.ingresar()
        val estado = vm.uiState.value
        assertEquals("Diego Muñoz", estado.usuario?.nombre)
        assertEquals("", estado.password)
        assertFalse(estado.cargando)
    }

    @Test
    fun pinIncorrecto_muestraElMensajeDelRepositorio() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar(Rol.OPERARIO, "111111111", "9999")
        vm.ingresar()
        assertNull(vm.uiState.value.usuario)
        assertEquals("Contraseña incorrecta", vm.uiState.value.mensajeError)
        vm.onMensajeMostrado()
        assertNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun perfilEquivocado_noIngresa() = runTest(main.dispatcher) {
        val vm = vm()
        vm.completar(Rol.SUPERVISOR, "111111111", "1234")
        vm.ingresar()
        assertNull(vm.uiState.value.usuario)
        assertNotNull(vm.uiState.value.mensajeError)
    }

    @Test
    fun cambiarPerfil_descartaPinYErrores() {
        val vm = vm()
        vm.completar(Rol.OPERARIO, "111111111", "")
        vm.ingresar()
        vm.onTogglePasswordVisible()
        assertTrue(vm.uiState.value.passwordVisible)
        vm.onPasswordChange("12")
        vm.onCambiarPerfil()
        val estado = vm.uiState.value
        assertNull(estado.rol)
        assertEquals("", estado.password)
        assertFalse(estado.passwordVisible)
        assertNull(estado.passwordError)
        // El RUT se conserva para no volver a escribirlo
        assertEquals("111111111", estado.rut)
    }

    @Test
    fun cerrarSesion_vuelveAlInicio_conservandoElCentro() = runTest(main.dispatcher) {
        val vm = vm()
        val centro = CentroCultivo.disponibles.last()
        vm.onCentroChange(centro)
        vm.completar(Rol.OPERARIO, "111111111", "1234")
        vm.ingresar()
        assertNotNull(vm.uiState.value.usuario)
        vm.onCerrarSesion()
        val estado = vm.uiState.value
        assertNull(estado.usuario)
        assertNull(estado.rol)
        assertEquals("", estado.rut)
        assertEquals(centro, estado.centro)
    }
}
