package cl.aldemar.musselapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.percentOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.ui.muestra.ConteoMarcas
import cl.aldemar.musselapp.ui.muestra.ConteoSobreImagen
import cl.aldemar.musselapp.ui.muestra.ETIQUETA_LIENZO
import cl.aldemar.musselapp.ui.muestra.ETIQUETA_TRAMO
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba de interfaz del conteo con marcas sobre la foto (B6), con una imagen en blanco. */
@RunWith(AndroidJUnit4::class)
class ConteoSobreFotoUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var conteo by mutableStateOf(ConteoMarcas())
    private var tramo by mutableStateOf<Int?>(null)
    private var usado: Int? = null

    private fun abrir(tramoCm: Int?) {
        tramo = tramoCm
        compose.setContent {
            MusselAppTheme {
                ConteoSobreImagen(
                    imagen = ImageBitmap(300, 200),
                    tramoCm = tramo,
                    conteo = conteo,
                    onCambiar = { conteo = it },
                    onTramoCambiado = { tramo = it },
                    onUsar = { usado = it },
                    onCerrar = {},
                )
            }
        }
    }

    private fun tocar(x: Float, y: Float) =
        compose.onNodeWithTag(ETIQUETA_LIENZO).performTouchInput { click(percentOffset(x, y)) }

    @Test
    fun tocar_agregaYQuitaMarcas() {
        abrir(tramoCm = 10)
        tocar(0.2f, 0.5f)
        tocar(0.8f, 0.5f)
        compose.onNodeWithText("Marcas: 2").assertExists()

        tocar(0.2f, 0.5f) // sobre la primera: se quita
        compose.onNodeWithText("Marcas: 1").assertExists()
    }

    @Test
    fun deshacer_recuperaLaUltimaAccion() {
        abrir(tramoCm = 10)
        tocar(0.3f, 0.3f)
        tocar(0.7f, 0.7f)
        compose.onNodeWithText("Deshacer").performClick()
        compose.onNodeWithText("Marcas: 1").assertExists()
        compose.onNodeWithText("Limpiar").performClick()
        compose.onNodeWithText("Marcas: 0").assertExists()
    }

    @Test
    fun conTramo_usaLosIndividuosPorMetro() {
        abrir(tramoCm = 10)
        tocar(0.25f, 0.5f)
        tocar(0.75f, 0.5f)
        compose.onNodeWithText("Tramo de 10 cm → 20 ind/m").assertExists()

        compose.onNodeWithText("Usar 20 ind/m").assertIsEnabled().performClick()

        compose.runOnIdle { assertEquals(20, usado) }
    }

    @Test
    fun conTramoYaMedido_noSeVuelveAPedir_peroSePuedeCambiar() {
        abrir(tramoCm = 40)
        tocar(0.25f, 0.5f)
        tocar(0.75f, 0.5f)
        compose.onNodeWithTag(ETIQUETA_TRAMO).assertDoesNotExist()
        compose.onNodeWithText("Tramo de 40 cm → 5 ind/m").assertExists()

        compose.onNodeWithText("Cambiar tramo").performClick()
        compose.onNodeWithTag(ETIQUETA_TRAMO).performTextReplacement("20")

        compose.onNodeWithText("Tramo de 20 cm → 10 ind/m").assertExists()
        compose.runOnIdle { assertEquals(20, tramo) }
    }

    @Test
    fun sinTramo_noSePuedeUsarElConteo() {
        abrir(tramoCm = null)
        tocar(0.5f, 0.5f)
        compose.onNodeWithText("Escribe el tramo para pasar las marcas a ind/m").assertExists()
        compose.onNodeWithText("Usar el conteo").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertNull(usado) }
    }

    @Test
    fun sinTramo_seEscribeEnElConteoYSePuedeUsar() {
        abrir(tramoCm = null)
        tocar(0.25f, 0.5f)
        tocar(0.75f, 0.5f)
        compose.onNodeWithTag(ETIQUETA_TRAMO).performTextInput("25")

        compose.onNodeWithText("Tramo de 25 cm → 8 ind/m").assertExists()
        compose.onNodeWithText("Usar 8 ind/m").assertIsEnabled().performClick()

        compose.runOnIdle {
            assertEquals(25, tramo) // el tramo también vuelve a la Vista previa
            assertEquals(8, usado)
        }
    }

    @Test
    fun tramoFueraDeRango_noSePuedeUsar() {
        abrir(tramoCm = 900)
        tocar(0.5f, 0.5f)
        compose.onNodeWithText("El tramo debe estar entre 1 y 500 cm").assertExists()
        compose.onNodeWithText("Usar el conteo").assertIsNotEnabled()
    }
}
