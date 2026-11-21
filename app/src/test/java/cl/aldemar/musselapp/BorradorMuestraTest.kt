package cl.aldemar.musselapp

import cl.aldemar.musselapp.ui.muestra.BorradorMuestra
import cl.aldemar.musselapp.ui.muestra.CONTEO_MAXIMO_IND_M
import cl.aldemar.musselapp.ui.muestra.ParametrosMuestra
import cl.aldemar.musselapp.ui.muestra.TALLA_MAXIMA_MM
import cl.aldemar.musselapp.ui.muestra.TRAMO_MAXIMO_CM
import cl.aldemar.musselapp.ui.muestra.validarBorrador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Reglas para poder guardar una muestra: ya no se inventan conteo ni talla. */
class BorradorMuestraTest {

    private val completo = BorradorMuestra(
        parametros = ParametrosMuestra("dalcahue", "B", 4, 3),
        fotoRuta = "/fotos/muestra.jpg",
        conteoIndM = 420,
        tallaMm = 65,
        cholgasIndM = 10,
        tramoCm = 100,
    )

    @Test
    fun borradorCompleto_sePuedeGuardar() {
        assertNull(validarBorrador(completo))
    }

    @Test
    fun sinConteoNiTalla_noSePuedeGuardar() {
        assertNotNull(validarBorrador(completo.copy(conteoIndM = null)))
        assertNotNull(validarBorrador(completo.copy(tallaMm = null)))
    }

    @Test
    fun sinParametrosOFoto_noSePuedeGuardar() {
        assertNotNull(validarBorrador(completo.copy(parametros = null)))
        assertNotNull(validarBorrador(completo.copy(fotoRuta = null)))
    }

    @Test
    fun valoresFueraDeRango_noSePuedenGuardar() {
        assertNotNull(validarBorrador(completo.copy(conteoIndM = 0)))
        assertNotNull(validarBorrador(completo.copy(conteoIndM = CONTEO_MAXIMO_IND_M + 1)))
        assertNotNull(validarBorrador(completo.copy(tallaMm = 0)))
        assertNotNull(validarBorrador(completo.copy(tallaMm = TALLA_MAXIMA_MM + 1)))
        assertNotNull(validarBorrador(completo.copy(cholgasIndM = -1)))
        assertNotNull(validarBorrador(completo.copy(cholgasIndM = CONTEO_MAXIMO_IND_M + 1)))
    }

    @Test
    fun cholgas_sonObligatoriasPeroPuedenSerCero() {
        assertNotNull(validarBorrador(completo.copy(cholgasIndM = null)))
        assertNull(validarBorrador(completo.copy(cholgasIndM = 0)))
    }

    @Test
    fun tramo_esObligatorioYConRango() {
        assertEquals("Ingresa el largo del tramo muestreado en cm", validarBorrador(completo.copy(tramoCm = null)))
        assertEquals("El tramo debe estar entre 1 y $TRAMO_MAXIMO_CM cm", validarBorrador(completo.copy(tramoCm = 0)))
        assertNotNull(validarBorrador(completo.copy(tramoCm = TRAMO_MAXIMO_CM + 1)))
        assertNull(validarBorrador(completo.copy(tramoCm = TRAMO_MAXIMO_CM)))
    }
}
