package cl.aldemar.musselapp.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CalibreTest {

    /** Fondo blanco con una "concha" negra: rectángulo de [largo] × [grosor] px girado [grados], centrado. */
    private fun concha(ancho: Int, alto: Int, largo: Double, grosor: Double, grados: Double): IntArray {
        val rad = Math.toRadians(grados)
        val cx = ancho / 2.0
        val cy = alto / 2.0
        return IntArray(ancho * alto) { i ->
            val x = i % ancho - cx + 0.5
            val y = i / ancho - cy + 0.5
            val u = x * cos(rad) + y * sin(rad)
            val v = -x * sin(rad) + y * cos(rad)
            if (kotlin.math.abs(u) <= largo / 2 && kotlin.math.abs(v) <= grosor / 2) 20 else 235
        }
    }

    @Test fun `la referencia de 200 px da 0,2 mm por pixel`() {
        assertEquals(0.2, Calibre.mmPorPixel(200.0, 200.0)!!, 1e-9)
        assertEquals(40.0 / 205.0, Calibre.mmPorPixel(210.0, 200.0)!!, 1e-9)
    }

    @Test fun `una referencia alargada indica foto inclinada y no se calcula`() {
        assertNull(Calibre.mmPorPixel(200.0, 150.0))
        assertNull(Calibre.mmPorPixel(0.0, 200.0))
    }

    @Test fun `mide el largo de una concha horizontal`() {
        val largo = Calibre.largoEjeMayorPx(concha(200, 120, 100.0, 30.0, 0.0), 200, 120)!!
        assertEquals(100.0, largo, 2.0)
    }

    @Test fun `mide el largo aunque la concha este girada`() {
        for (grados in listOf(25.0, 45.0, 70.0, 135.0)) {
            val largo = Calibre.largoEjeMayorPx(concha(220, 220, 120.0, 40.0, grados), 220, 220)!!
            assertEquals("girada $grados°", 120.0, largo, 3.0)
        }
    }

    @Test fun `convierte el largo a milimetros con la referencia`() {
        val mm = Calibre.largoMm(concha(200, 120, 100.0, 30.0, 0.0), 200, 120, mmPorPixel = 0.5)!!
        assertEquals(50.0, mm, 1.0)
    }

    @Test fun `sin concha no hay medida`() {
        assertNull(Calibre.largoEjeMayorPx(IntArray(50 * 50) { 235 }, 50, 50, umbral = 128))
    }

    @Test fun `otsu separa fondo claro y concha oscura`() {
        val u = Calibre.umbralOtsu(concha(100, 100, 60.0, 20.0, 0.0))
        assertTrue("umbral $u", u in 20 until 235)
    }

    @Test fun `una foto nitida no es borrosa y una lisa si`() {
        val nitida = concha(100, 100, 60.0, 20.0, 30.0)
        val lisa = IntArray(100 * 100) { 128 }
        assertFalse(Calibre.esBorrosa(nitida, 100, 100))
        assertTrue(Calibre.esBorrosa(lisa, 100, 100))
    }

    @Test fun `resumen por especie con rangos de 5 mm`() {
        val r = ResumenCalibre.de(listOf(52.0, 54.9, 55.0, 61.2, Double.NaN, -1.0))
        assertEquals(4, r.cantidad)
        assertEquals(55.775, r.promedioMm!!, 1e-9)
        assertEquals(52.0, r.minimoMm!!, 1e-9)
        assertEquals(61.2, r.maximoMm!!, 1e-9)
        assertEquals(mapOf(50 to 2, 55 to 1, 60 to 1), r.porRango)
    }

    @Test fun `resumen vacio sin datos`() {
        val r = ResumenCalibre.de(emptyList())
        assertEquals(0, r.cantidad)
        assertNull(r.promedioMm)
        assertTrue(r.porRango.isEmpty())
    }
}
