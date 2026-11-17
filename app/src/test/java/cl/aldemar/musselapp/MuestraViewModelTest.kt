package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CatalogoCultivo
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.ui.muestra.MuestraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pruebas mínimas de la guía de validación de formularios (Clase 8). */
class MuestraViewModelTest {

    private fun completarFormulario(vm: MuestraViewModel) {
        vm.cambiarCentro(CentroCultivo.disponibles.first())
        vm.cambiarTren("B")
        vm.cambiarLinea(4)
        vm.cambiarEstrato(3)
    }

    @Test
    fun guardarVacio_muestraTodosLosErrores() {
        val vm = MuestraViewModel()
        vm.guardar()
        val s = vm.uiState.value
        assertNotNull(s.errorCentro)
        assertNotNull(s.errorTren)
        assertNotNull(s.errorLinea)
        assertNotNull(s.errorEstrato)
        assertNull(s.mensaje)
        assertNull(s.parametrosValidos)
    }

    @Test
    fun guardarIncompleto_soloMarcaLosCamposFaltantes() {
        val vm = MuestraViewModel()
        vm.iniciar(CentroCultivo.disponibles.first())
        vm.cambiarTren("A")
        vm.guardar()
        val s = vm.uiState.value
        assertNull(s.errorCentro)
        assertNull(s.errorTren)
        assertNotNull(s.errorLinea)
        assertNotNull(s.errorEstrato)
        assertNull(s.parametrosValidos)
    }

    @Test
    fun corregirUnCampo_quitaSuError() {
        val vm = MuestraViewModel()
        vm.guardar()
        vm.cambiarLinea(2)
        assertNull(vm.uiState.value.errorLinea)
        assertNotNull(vm.uiState.value.errorTren)
    }

    @Test
    fun guardarCompleto_confirmaYEntregaParametros() {
        val vm = MuestraViewModel()
        completarFormulario(vm)
        vm.guardar()
        val s = vm.uiState.value
        assertNull(s.errorCentro)
        assertNull(s.errorTren)
        assertNull(s.errorLinea)
        assertNull(s.errorEstrato)
        assertEquals("Parámetros registrados correctamente", s.mensaje)
        val p = s.parametrosValidos!!
        assertEquals("dalcahue", p.centroId)
        assertEquals("B", p.tren)
        assertEquals(4, p.linea)
        assertEquals(3, p.profundidadM)
    }

    // Las dos siguientes fijan qué acepta el formulario: solo trenes y líneas que existen en el centro

    @Test
    fun trenQueNoExiste_marcaErrorSoloEnElTren() {
        val vm = MuestraViewModel()
        completarFormulario(vm)
        vm.cambiarTren("Z")
        vm.guardar()
        val s = vm.uiState.value
        assertNotNull(s.errorTren)
        assertNull(s.errorCentro)
        assertNull(s.errorLinea)
        assertNull(s.errorEstrato)
        assertNull(s.parametrosValidos)
    }

    @Test
    fun lineaQueNoExiste_marcaErrorSoloEnLaLinea() {
        val vm = MuestraViewModel()
        completarFormulario(vm)
        vm.cambiarLinea(13)
        vm.guardar()
        val s = vm.uiState.value
        assertNotNull(s.errorLinea)
        assertNull(s.errorTren)
        assertNull(s.parametrosValidos)

        vm.cambiarLinea(12)
        vm.guardar()
        assertEquals(12, vm.uiState.value.parametrosValidos?.linea)
    }

    // N8: las listas vienen de la base (CatalogoCultivo)

    @Test
    fun conCatalogoDeLaBase_validaContraSusTrenesYLineas() {
        val vm = MuestraViewModel()
        vm.usarCatalogo(CatalogoCultivo(mapOf("A" to listOf(1, 2), "D" to listOf(20))))
        vm.cambiarCentro(CentroCultivo.disponibles.first())
        vm.cambiarEstrato(3)

        // El tren D y su línea 20 no existían en la lista fija; ahora valen porque están en la base
        vm.cambiarTren("D")
        vm.cambiarLinea(20)
        vm.guardar()
        assertEquals(20, vm.uiState.value.parametrosValidos?.linea)

        // El tren B ya no es de este centro
        vm.cambiarTren("B")
        vm.guardar()
        assertNotNull(vm.uiState.value.errorTren)
        assertNull(vm.uiState.value.parametrosValidos)

        // La línea 20 es del tren D, no del A
        vm.cambiarTren("A")
        vm.guardar()
        assertNotNull(vm.uiState.value.errorLinea)
        assertNull(vm.uiState.value.errorTren)
    }

    @Test
    fun usarCatalogo_noCambiaLoQueYaEligioElOperario() {
        val vm = MuestraViewModel()
        completarFormulario(vm)
        vm.usarCatalogo(CatalogoCultivo(mapOf("A" to listOf(1))))
        val s = vm.uiState.value
        assertEquals("B", s.tren)
        assertEquals(4, s.linea)
        assertNull(s.errorTren)
    }

    @Test
    fun limpiar_conservaElCatalogoSiSigueEnElMismoCentro() {
        val vm = MuestraViewModel()
        val sesion = CentroCultivo.disponibles.first()
        val catalogo = CatalogoCultivo(mapOf("A" to listOf(1, 2)))
        vm.iniciar(sesion)
        vm.usarCatalogo(catalogo)
        vm.limpiar()
        assertEquals(catalogo, vm.uiState.value.catalogo)

        // Si limpiar devuelve el formulario a otro centro, el catálogo anterior ya no sirve
        vm.cambiarCentro(CentroCultivo.disponibles[1])
        vm.limpiar()
        assertEquals(sesion, vm.uiState.value.centro)
        assertEquals(CatalogoCultivo.porDefecto, vm.uiState.value.catalogo)
    }

    @Test
    fun limpiar_borraValoresErroresYMensajes() {
        val vm = MuestraViewModel()
        vm.guardar()
        completarFormulario(vm)
        vm.guardar()
        vm.limpiar()
        val s = vm.uiState.value
        assertNull(s.centro)
        assertNull(s.tren)
        assertNull(s.linea)
        assertNull(s.profundidadM)
        assertNull(s.errorCentro)
        assertNull(s.mensaje)
        assertNull(s.parametrosValidos)
    }

    @Test
    fun iniciar_noReemplazaUnCentroYaElegido() {
        val vm = MuestraViewModel()
        val otro = CentroCultivo.disponibles[1]
        vm.cambiarCentro(otro)
        vm.iniciar(CentroCultivo.disponibles.first())
        assertEquals(otro, vm.uiState.value.centro)
    }
}
