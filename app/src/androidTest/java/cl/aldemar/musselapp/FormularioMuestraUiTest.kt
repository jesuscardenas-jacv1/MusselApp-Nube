package cl.aldemar.musselapp

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.aldemar.musselapp.data.model.CatalogoCultivo
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.ui.muestra.MuestraScreen
import cl.aldemar.musselapp.ui.muestra.MuestraViewModel
import cl.aldemar.musselapp.ui.muestra.ParametrosMuestra
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de interfaz (Compose UI Test) del formulario de Nueva muestra: con el formulario vacío
 * el botón muestra los errores de cada campo; completo, avanza a la captura con los datos elegidos.
 */
@RunWith(AndroidJUnit4::class)
class FormularioMuestraUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val vm = MuestraViewModel()
    private var continuoCon: ParametrosMuestra? = null

    /** La pantalla pide la ubicación al abrirse: se concede antes para que no aparezca el diálogo del sistema. */
    @Before
    fun concederUbicacion() {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val paquete = instrumentacion.targetContext.packageName
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).forEach {
            instrumentacion.uiAutomation.grantRuntimePermission(paquete, it)
        }
    }

    private fun abrirFormulario() {
        compose.setContent {
            MusselAppTheme {
                MuestraScreen(vm = vm, onContinuar = { continuoCon = it })
            }
        }
    }

    private val errores = listOf(
        "Seleccione el centro de cultivo",
        "Seleccione el tren de cultivo",
        "Seleccione la línea de siembra",
        "Seleccione el estrato de profundidad",
    )

    @Test
    fun formularioVacio_alContinuarMuestraLosCuatroErrores() {
        abrirFormulario()
        errores.forEach { compose.onAllNodesWithText(it).assertCountEquals(0) }

        compose.onNodeWithText("Continuar a captura").performClick()

        errores.forEach { compose.onNodeWithText(it).assertExists() }
        assertNull(continuoCon)
    }

    @Test
    fun formularioCompleto_avanzaALaCapturaConLosDatosElegidos() {
        abrirFormulario()
        compose.onNodeWithText("Continuar a captura").performClick()

        val centro = CentroCultivo.disponibles.first()
        val tren = CatalogoCultivo.porDefecto.trenes.first()
        val linea = CatalogoCultivo.porDefecto.lineasDe(tren).first()
        compose.runOnIdle {
            vm.cambiarCentro(centro)
            vm.cambiarTren(tren)
            vm.cambiarLinea(linea)
            vm.cambiarEstrato(3)
        }
        // Elegir cada dato quita su error
        errores.forEach { compose.onAllNodesWithText(it).assertCountEquals(0) }
        compose.onNodeWithText(centro.nombre).assertExists()

        compose.onNodeWithText("Continuar a captura").performClick()

        compose.runOnIdle {
            assertEquals(ParametrosMuestra(centro.id, tren, linea, 3), continuoCon)
        }
    }

    @Test
    fun limpiarFormulario_vuelveAlEstadoInicial() {
        abrirFormulario()
        compose.runOnIdle { vm.cambiarCentro(CentroCultivo.disponibles.first()) }
        compose.onNodeWithText(CentroCultivo.disponibles.first().nombre).assertExists()

        compose.onNodeWithText("Limpiar formulario").performClick()

        compose.onNodeWithText("Selecciona un centro").assertExists()
    }
}
