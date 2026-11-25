package cl.aldemar.musselapp.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalidaRfDetrTest {

    private val alto = 3f    // logit 3 → confianza 0,95
    private val bajo = -3f   // logit -3 → confianza 0,05

    @Test fun `convierte centro y tamano a esquina y elige la clase con mayor puntaje`() {
        val cajas = floatArrayOf(0.5f, 0.5f, 0.2f, 0.1f)
        val logits = floatArrayOf(bajo, alto, bajo)  // clase 1
        val r = SalidaRfDetr.decodificar(cajas, logits, numClases = 3)
        assertEquals(1, r.size)
        val c = r[0]
        assertEquals(1, c.clase)
        assertEquals(0.4f, c.x, 1e-5f); assertEquals(0.45f, c.y, 1e-5f)
        assertEquals(0.2f, c.ancho, 1e-5f); assertEquals(0.1f, c.alto, 1e-5f)
        assertEquals(0.9526f, c.confianza, 1e-3f)
    }

    @Test fun `descarta las consultas bajo el umbral`() {
        val cajas = floatArrayOf(0.2f, 0.2f, 0.1f, 0.1f, 0.7f, 0.7f, 0.1f, 0.1f)
        val logits = floatArrayOf(bajo, bajo, alto, bajo)
        val r = SalidaRfDetr.decodificar(cajas, logits, numClases = 2)
        assertEquals(1, r.size)
        assertEquals(0.65f, r[0].x, 1e-5f)
    }

    @Test fun `quita cajas duplicadas sobre el mismo individuo`() {
        val cajas = floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.51f, 0.5f, 0.2f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f)
        val logits = floatArrayOf(alto, bajo, 2f, bajo, alto, bajo)
        val r = SalidaRfDetr.decodificar(cajas, logits, numClases = 2)
        assertEquals(2, r.size)
        assertTrue(r.all { it.confianza > 0.5f })
        assertEquals(3, SalidaRfDetr.decodificar(cajas, logits, numClases = 2, iouDuplicado = 1f).size)
    }

    @Test fun `ajusta a la foto las cajas que se salen del borde`() {
        val r = SalidaRfDetr.decodificar(floatArrayOf(0.02f, 0.98f, 0.1f, 0.1f), floatArrayOf(alto), numClases = 1)
        assertEquals(0f, r[0].x, 1e-6f)
        assertEquals(1f, r[0].y + r[0].alto, 1e-6f)
    }

    @Test fun `ordena de mayor a menor confianza`() {
        val cajas = floatArrayOf(0.2f, 0.2f, 0.1f, 0.1f, 0.7f, 0.7f, 0.1f, 0.1f)
        val r = SalidaRfDetr.decodificar(cajas, floatArrayOf(1f, 4f), numClases = 1)
        assertTrue(r[0].confianza > r[1].confianza)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza tamanos que no calzan`() {
        SalidaRfDetr.decodificar(FloatArray(8), FloatArray(5), numClases = 3)
    }
}
