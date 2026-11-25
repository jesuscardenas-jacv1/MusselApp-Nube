package cl.aldemar.musselapp.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalidaRfDetrTest {

    private val alto = 3f    // logit 3 → confianza 0,95
    private val bajo = -3f   // logit -3 → confianza 0,05
    private val etiquetas = listOf("chorito", "cholga", "referencia")

    /** Puntajes de una consulta con 4 columnas (3 clases + 1 sin uso): chorito, cholga, referencia, sobrante. */
    private fun fila(chorito: Float = bajo, cholga: Float = bajo, referencia: Float = bajo, sobrante: Float = bajo) =
        floatArrayOf(chorito, cholga, referencia, sobrante)

    @Test fun `convierte centro y tamano a esquina y elige la clase con mayor puntaje`() {
        val r = SalidaRfDetr.decodificar(floatArrayOf(0.5f, 0.5f, 0.2f, 0.1f), fila(cholga = alto), columnas = 4)
        assertEquals(1, r.size)
        val c = r[0]
        assertEquals("cholga", SalidaRfDetr.etiqueta(c.clase, etiquetas))
        assertEquals(0.4f, c.x, 1e-5f); assertEquals(0.45f, c.y, 1e-5f)
        assertEquals(0.2f, c.ancho, 1e-5f); assertEquals(0.1f, c.alto, 1e-5f)
        assertEquals(0.9526f, c.confianza, 1e-3f)
    }

    @Test fun `la columna 0 es chorito y la ultima columna no es una clase`() {
        val chorito = SalidaRfDetr.decodificar(floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f), fila(chorito = alto), columnas = 4)
        assertEquals("chorito", SalidaRfDetr.etiqueta(chorito[0].clase, etiquetas))
        val sobrante = SalidaRfDetr.decodificar(floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f), fila(sobrante = 5f), columnas = 4)
        assertTrue("la columna sobrante no debe generar detecciones", sobrante.isEmpty())
    }

    @Test fun `descarta las consultas bajo el umbral`() {
        val cajas = floatArrayOf(0.2f, 0.2f, 0.1f, 0.1f, 0.7f, 0.7f, 0.1f, 0.1f)
        val r = SalidaRfDetr.decodificar(cajas, fila() + fila(referencia = alto), columnas = 4)
        assertEquals(1, r.size)
        assertEquals("referencia", SalidaRfDetr.etiqueta(r[0].clase, etiquetas))
        assertEquals(0.65f, r[0].x, 1e-5f)
    }

    @Test fun `quita cajas duplicadas sobre el mismo individuo`() {
        val cajas = floatArrayOf(0.5f, 0.5f, 0.2f, 0.2f, 0.51f, 0.5f, 0.2f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f)
        val logits = fila(chorito = alto) + fila(chorito = 2f) + fila(cholga = alto)
        assertEquals(2, SalidaRfDetr.decodificar(cajas, logits, columnas = 4).size)
        assertEquals(3, SalidaRfDetr.decodificar(cajas, logits, columnas = 4, iouDuplicado = 1f).size)
    }

    @Test fun `ajusta a la foto las cajas que se salen del borde`() {
        val r = SalidaRfDetr.decodificar(floatArrayOf(0.02f, 0.98f, 0.1f, 0.1f), fila(chorito = alto), columnas = 4)
        assertEquals(0f, r[0].x, 1e-6f)
        assertEquals(1f, r[0].y + r[0].alto, 1e-6f)
    }

    @Test fun `ordena de mayor a menor confianza`() {
        val cajas = floatArrayOf(0.2f, 0.2f, 0.1f, 0.1f, 0.7f, 0.7f, 0.1f, 0.1f)
        val r = SalidaRfDetr.decodificar(cajas, fila(chorito = 1f) + fila(chorito = 4f), columnas = 4)
        assertTrue(r[0].confianza > r[1].confianza)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza tamanos que no calzan`() {
        SalidaRfDetr.decodificar(FloatArray(8), FloatArray(5), columnas = 4)
    }
}
