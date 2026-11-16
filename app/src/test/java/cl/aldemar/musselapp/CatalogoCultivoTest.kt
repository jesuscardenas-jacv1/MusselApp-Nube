package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CatalogoCultivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Trenes y líneas que ofrece el formulario de Nueva muestra (N8). */
class CatalogoCultivoTest {

    @Test
    fun porDefecto_tieneLosTresTrenesConDoceLineas() {
        val c = CatalogoCultivo.porDefecto
        assertEquals(listOf("A", "B", "C"), c.trenes)
        assertEquals((1..12).toList(), c.lineasDe("A"))
        assertEquals((1..12).toList(), c.lineasDe("C"))
    }

    @Test
    fun sinTrenElegido_ofreceTodasLasLineasDelCentro() {
        assertEquals((1..12).toList(), CatalogoCultivo.porDefecto.lineasDe(null))
        val c = CatalogoCultivo(mapOf("A" to listOf(1, 2), "B" to listOf(2, 5)))
        assertEquals(listOf(1, 2, 5), c.lineasDe(null))
        // Un tren que no es del centro tampoco deja la lista vacía
        assertEquals(listOf(1, 2, 5), c.lineasDe("Z"))
    }

    @Test
    fun desde_agrupaPorTrenYOrdena() {
        val c = CatalogoCultivo.desde(listOf("B" to 2, "A" to 3, "B" to 1, "A" to 1))
        assertEquals(listOf("A", "B"), c.trenes)
        assertEquals(listOf(1, 3), c.lineasDe("A"))
        assertEquals(listOf(1, 2), c.lineasDe("B"))
    }

    @Test
    fun desde_sinFilas_usaElCatalogoPorDefecto() {
        assertEquals(CatalogoCultivo.porDefecto, CatalogoCultivo.desde(emptyList()))
    }

    @Test
    fun tieneTrenYTieneLinea() {
        val c = CatalogoCultivo(mapOf("A" to listOf(1, 2), "B" to listOf(5)))
        assertTrue(c.tieneTren("A"))
        assertFalse(c.tieneTren("C"))
        assertFalse(c.tieneTren(null))
        assertTrue(c.tieneLinea("B", 5))
        assertFalse(c.tieneLinea("B", 1)) // la línea 1 es del tren A, no del B
        assertFalse(c.tieneLinea("A", null))
    }
}
