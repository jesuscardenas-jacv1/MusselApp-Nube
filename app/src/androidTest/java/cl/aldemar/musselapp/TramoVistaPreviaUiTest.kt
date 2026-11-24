package cl.aldemar.musselapp

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.aldemar.musselapp.ui.muestra.BorradorMuestra
import cl.aldemar.musselapp.ui.muestra.ETIQUETA_LIENZO
import cl.aldemar.musselapp.ui.muestra.VistaPreviaScreen
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** El conteo por marcas depende del tramo: si el tramo cambia después de contar, se recalcula. */
@RunWith(AndroidJUnit4::class)
class TramoVistaPreviaUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var borrador by mutableStateOf(BorradorMuestra())
    private lateinit var foto: File

    @Before
    fun preparar() {
        foto = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "tramo_prueba.jpg")
        foto.outputStream().use { Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 80, it) }
        borrador = BorradorMuestra(fotoRuta = foto.path, tramoCm = 40)
        compose.setContent {
            MusselAppTheme {
                VistaPreviaScreen(
                    borrador = borrador,
                    onConteoCambiado = { borrador = borrador.copy(conteoIndM = it) },
                    onTramoCambiado = { borrador = borrador.copy(tramoCm = it) },
                )
            }
        }
    }

    @After
    fun limpiar() {
        foto.delete()
    }

    private fun contarDos() {
        compose.onNodeWithText("Contar sobre la foto").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(ETIQUETA_LIENZO).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(ETIQUETA_LIENZO).performTouchInput { click(percentOffset(0.25f, 0.5f)) }
        compose.onNodeWithTag(ETIQUETA_LIENZO).performTouchInput { click(percentOffset(0.75f, 0.5f)) }
        compose.onNodeWithText("Usar 5 ind/m").performClick()
        compose.runOnIdle { assertEquals(5, borrador.conteoIndM) }
    }

    private fun moverCinta(cm: Int) {
        compose.onNodeWithContentDescription("Tramo muestreado, cinta métrica").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(cm.toFloat()) }
        compose.waitForIdle()
    }

    @Test
    fun cambiarElTramoDespuesDeContar_recalculaElConteo() {
        contarDos()
        moverCinta(20)
        compose.runOnIdle {
            assertEquals(20, borrador.tramoCm)
            assertEquals(10, borrador.conteoIndM)
        }
    }

    @Test
    fun conteoEscritoAMano_noCambiaConElTramo() {
        compose.runOnIdle { borrador = borrador.copy(conteoIndM = 390) }
        moverCinta(20)
        compose.runOnIdle { assertEquals(390, borrador.conteoIndM) }
    }
}
