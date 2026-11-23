package cl.aldemar.musselapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.ui.principal.ETIQUETA_CONTADORES
import cl.aldemar.musselapp.ui.principal.ETIQUETA_FLECHA_HISTORIAL
import cl.aldemar.musselapp.ui.principal.ResumenJornada
import cl.aldemar.musselapp.ui.principal.TarjetaHistorial
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cuadro del historial en la Principal: la flecha pliega y despliega los contadores por estado. */
@RunWith(AndroidJUnit4::class)
class HistorialPlegableUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var desplegado by mutableStateOf(true)

    private val resumen = ResumenJornada(
        totalMuestras = 11, tallaPromedioMm = 61, conteoPromedioIndM = 381, lineasMuestreadas = 11,
        lineasTotales = 36, validadas = 6, pendientes = 4, corregidas = 1, pendientesRevision = 4,
        pendientesSincronizar = 0, ultimaRegistradaEn = null,
    )

    private fun abrir() {
        compose.setContent {
            MusselAppTheme {
                TarjetaHistorial(resumen = resumen, desplegado = desplegado, onAlternar = { desplegado = !desplegado }, onVerTodo = {})
            }
        }
    }

    @Test
    fun laFlechaPliegaYDespliegaLosContadores() {
        abrir()
        compose.onNodeWithTag(ETIQUETA_CONTADORES).assertIsDisplayed()
        compose.onNodeWithText("PENDIENTES").assertIsDisplayed()

        compose.onNodeWithContentDescription("Ocultar los contadores del historial").performClick()
        compose.onNodeWithTag(ETIQUETA_CONTADORES).assertDoesNotExist()
        // El encabezado sigue a la vista
        compose.onNodeWithText("Historial de muestras").assertIsDisplayed()
        compose.onNodeWithText("11 registros capturados hoy").assertIsDisplayed()

        compose.onNodeWithTag(ETIQUETA_FLECHA_HISTORIAL).performClick()
        compose.onNodeWithTag(ETIQUETA_CONTADORES).assertIsDisplayed()
    }

    @Test
    fun plegadoDesdeAntes_abreSinContadores() {
        desplegado = false
        abrir()
        compose.onNodeWithTag(ETIQUETA_CONTADORES).assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar los contadores del historial").assertIsDisplayed()
    }
}
