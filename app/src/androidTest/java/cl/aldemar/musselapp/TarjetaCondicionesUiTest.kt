package cl.aldemar.musselapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.ui.componentes.TarjetaCondiciones
import cl.aldemar.musselapp.ui.theme.MusselAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tarjeta "Condiciones al muestrear": cada dato en su recuadro y la ubicación con su pastilla. */
@RunWith(AndroidJUnit4::class)
class TarjetaCondicionesUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val base = Muestra(
        id = 14, centroId = "dalcahue", linea = 4, tren = "B", profundidadM = 6, tallaMm = 72, conteoIndM = 450,
        observacion = "", estado = EstadoMuestra.VALIDADO, registradaEn = 0L, sincronizada = true, operador = "Carla Vera",
    )

    private fun mostrar(muestra: Muestra) = compose.setContent { MusselAppTheme { TarjetaCondiciones(muestra) } }

    @Test
    fun conClimaYPosicion_muestraRecuadrosYArea() {
        mostrar(
            base.copy(
                posicion = Posicion(-42.3696, -73.6248, 6.0),
                clima = Clima(tempAireC = 14.0, vientoKn = 9.0, rachaKn = 12.0, vientoDir = 180, codigo = 61,
                    olaM = 0.3, tempAguaC = 14.1, mareaM = -0.6, obtenidoEn = 0L),
            ),
        )
        compose.onNodeWithText("Tiempo · Lluvia").assertIsDisplayed()
        compose.onNodeWithText("kn del S").assertIsDisplayed()
        compose.onNodeWithText("-0,60").assertIsDisplayed()
        compose.onNodeWithText("Dentro del área").assertIsDisplayed()
    }

    @Test
    fun sinClimaNiPosicion_loDice() {
        mostrar(base)
        compose.onNodeWithText("Tiempo y mar no registrados").assertIsDisplayed()
        compose.onNodeWithText("Ubicación no registrada").assertIsDisplayed()
    }
}
