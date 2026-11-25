package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.nube.ResultadoMlNube.DeteccionNube
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultadoMlNubeTest {

    private fun det(cholga: Boolean = false, mm: Double? = 55.0, operario: Boolean = false, conf: Float? = 0.934f) =
        DeteccionNube(cholga, if (operario) null else conf, 0.1204f, 0.34f, 0.06f, 0.028f, mm, operario)

    @Suppress("UNCHECKED_CAST")
    private fun ml(campos: Map<String, Any>) = campos["ml"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun detecciones(campos: Map<String, Any>) = campos["detecciones"] as List<Map<String, Any>>

    @Test fun `resume conteos y tallas por especie`() {
        val campos = ResultadoMlNube.aCampos("model_v1",
            listOf(det(mm = 52.0), det(mm = 56.0), det(cholga = true, mm = 71.0, operario = true)), mmPorPx = 0.212345, editado = true)
        val m = ml(campos)
        assertEquals("model_v1", m["version"])
        assertEquals(3, m["total"]); assertEquals(2, m["choritos"]); assertEquals(1, m["cholgas"])
        assertEquals(54.0, m["tallaChoritoMm"]); assertEquals(71.0, m["tallaCholgaMm"])
        assertEquals(0.2123, m["mmPorPx"]); assertEquals(true, m["editado"])
    }

    @Test fun `cada deteccion usa el formato compacto del contrato`() {
        val d = detecciones(ResultadoMlNube.aCampos("model_v1", listOf(det(), det(cholga = true, operario = true, mm = null)), null, false))
        assertEquals(mapOf("e" to "C", "p" to 0.93, "x" to 120, "y" to 340, "w" to 60, "h" to 28, "mm" to 55, "o" to "M"), d[0])
        assertEquals(mapOf("e" to "K", "x" to 120, "y" to 340, "w" to 60, "h" to 28, "o" to "O"), d[1])
    }

    @Test fun `solo usa los campos que permiten las reglas`() {
        val m = ml(ResultadoMlNube.aCampos("model_v1", listOf(det()), 0.2, false))
        val permitidos = setOf("version", "total", "choritos", "cholgas", "mmPorPx", "tallaChoritoMm", "tallaCholgaMm", "editado")
        assertTrue(permitidos.containsAll(m.keys))
    }

    @Test fun `sin calibre no sube tallas ni mm por pixel`() {
        val m = ml(ResultadoMlNube.aCampos("model_v1", listOf(det(mm = null)), mmPorPx = null, editado = false))
        assertFalse("tallaChoritoMm" in m); assertFalse("tallaCholgaMm" in m); assertFalse("mmPorPx" in m)
    }

    @Test fun `respeta los limites de las reglas`() {
        val muchas = List(2500) { det() }
        val campos = ResultadoMlNube.aCampos("v".repeat(100), muchas, 0.2, false)
        assertEquals(ResultadoMlNube.MAX_DETECCIONES, detecciones(campos).size)
        assertEquals(ResultadoMlNube.MAX_CONTEO, ml(campos)["total"])
        assertEquals(40, (ml(campos)["version"] as String).length)
    }

    @Test fun `recuadros fuera de la foto se ajustan a 0-1000`() {
        val d = detecciones(ResultadoMlNube.aCampos("model_v1",
            listOf(DeteccionNube(false, 1.4f, -0.2f, 1.3f, 0.5f, 0.5f, 50.0, false)), null, false))[0]
        assertEquals(0, d["x"]); assertEquals(1000, d["y"]); assertEquals(1.0, d["p"])
    }

    @Test fun `el resultado del modelo no se reescribe si la nube ya tiene dictamen`() {
        val datos = mapOf("observacion" to "x", "ml" to mapOf("total" to 3), "detecciones" to emptyList<Any>(), "estado" to "PENDIENTE")
        assertEquals(mapOf("observacion" to "x"), ProteccionDictamen.filtrar(datos, estadoRemoto = "VALIDADO"))
        assertEquals(datos, ProteccionDictamen.filtrar(datos, estadoRemoto = "PENDIENTE"))
    }
}
