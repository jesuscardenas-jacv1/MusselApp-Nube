package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.repository.FakeAuthRepository
import cl.aldemar.musselapp.ui.login.LoginViewModel
import cl.aldemar.musselapp.ui.muestra.MuestraViewModel
import cl.aldemar.musselapp.util.Fotos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reconteo, limpieza del formulario, PIN numérico y reducción de fotos. */
class MejorasFormularioTest {

    private val sesion = CentroCultivo.disponibles.first()
    private val otro = CentroCultivo.disponibles[1]

    @Test
    fun limpiar_vuelveAlCentroDeLaSesion() {
        val vm = MuestraViewModel()
        vm.iniciar(sesion)
        vm.cambiarCentro(otro)
        vm.cambiarTren("B")
        vm.limpiar()
        val s = vm.uiState.value
        assertEquals(sesion, s.centro)
        assertNull(s.tren)
    }

    @Test
    fun prefijar_copiaLaUbicacionDeLaMuestraOriginal() {
        val vm = MuestraViewModel()
        vm.iniciar(sesion)
        vm.prefijar(otro, "C", 7, 3)
        val s = vm.uiState.value
        assertEquals(otro, s.centro)
        assertEquals("C", s.tren)
        assertEquals(7, s.linea)
        assertEquals(3, s.profundidadM)
    }

    @Test
    fun prefijar_noPisaLoQueElOperarioYaEligio() {
        val vm = MuestraViewModel()
        vm.iniciar(sesion)
        vm.cambiarTren("A")
        vm.prefijar(otro, "C", 7, 3)
        val s = vm.uiState.value
        assertEquals(sesion, s.centro)
        assertEquals("A", s.tren)
        assertNull(s.linea)
    }

    @Test
    fun prefijar_dejaElFormularioValido() {
        val vm = MuestraViewModel()
        vm.prefijar(sesion, "B", 12, 3)
        vm.guardar()
        val parametros = vm.uiState.value.parametrosValidos
        assertEquals(sesion.id, parametros?.centroId)
        assertEquals(12, parametros?.linea)
    }

    @Test
    fun pin_soloAceptaDigitosYUnLargoMaximo() {
        val vm = LoginViewModel(FakeAuthRepository(latenciaMs = 0))
        vm.onPasswordChange("12a3-4")
        assertEquals("1234", vm.uiState.value.password)
        vm.onPasswordChange("1234567890123")
        assertEquals("12345678", vm.uiState.value.password)
    }

    @Test
    fun muestreo_reduceFotosGrandesYNoAgrandaLasChicas() {
        assertEquals(1, Fotos.muestreo(800, 600, 1280))
        assertEquals(2, Fotos.muestreo(4000, 3000, 1280)) // 4000/2 = 2000 ≥ 1280; 4000/4 = 1000 < 1280
        assertEquals(4, Fotos.muestreo(3000, 6000, 1280))
        assertEquals(1, Fotos.muestreo(1280, 960, 1280))
    }
}
