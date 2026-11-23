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
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.ui.principal.ETIQUETA_FLECHA_RECIENTES
import cl.aldemar.musselapp.ui.principal.ETIQUETA_LISTA_RECIENTES
import cl.aldemar.musselapp.ui.principal.MuestrasRecientes
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sección Muestras recientes de la Principal: la flecha del encabezado oculta y muestra la lista. */
@RunWith(AndroidJUnit4::class)
class RecientesPlegablesUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var desplegadas by mutableStateOf(true)

    private val muestras = listOf(
        Muestra(
            id = 7, centroId = "dalcahue", linea = 7, tren = "A", profundidadM = 1, tallaMm = 2,
            conteoIndM = 100, observacion = "", estado = EstadoMuestra.PENDIENTE,
            registradaEn = System.currentTimeMillis(), sincronizada = true, operador = "Carla Vera",
        ),
    )

    private fun abrir() {
        compose.setContent {
            MusselAppTheme {
                MuestrasRecientes(
                    muestras = muestras,
                    ultimaRegistradaEn = muestras.first().registradaEn,
                    desplegadas = desplegadas,
                    onAlternar = { desplegadas = !desplegadas },
                    onMuestraClick = {},
                )
            }
        }
    }

    @Test
    fun laFlechaOcultaYMuestraLaLista() {
        abrir()
        compose.onNodeWithTag(ETIQUETA_LISTA_RECIENTES).assertIsDisplayed()

        compose.onNodeWithContentDescription("Ocultar las muestras recientes").performClick()
        compose.onNodeWithTag(ETIQUETA_LISTA_RECIENTES).assertDoesNotExist()
        // El título queda a la vista para poder desplegarla de nuevo
        compose.onNodeWithText("Muestras recientes", useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithTag(ETIQUETA_FLECHA_RECIENTES).performClick()
        compose.onNodeWithTag(ETIQUETA_LISTA_RECIENTES).assertIsDisplayed()
    }

    @Test
    fun ocultasDesdeAntes_abreSinLista() {
        desplegadas = false
        abrir()
        compose.onNodeWithTag(ETIQUETA_LISTA_RECIENTES).assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar las muestras recientes").assertIsDisplayed()
    }
}
