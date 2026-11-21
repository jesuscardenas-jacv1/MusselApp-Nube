package cl.aldemar.musselapp

import cl.aldemar.musselapp.ui.muestra.ConteoMarcas
import cl.aldemar.musselapp.ui.muestra.Marca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conteo asistido con marcas sobre la foto (B6): agregar, quitar, deshacer y pasar a ind/m. */
class ConteoMarcasTest {

    private val radio = 0.05f

    @Test
    fun tocarLejos_agregaMarcas() {
        val conteo = ConteoMarcas()
            .tocar(Marca(0.1f, 0.1f), radio)
            .tocar(Marca(0.5f, 0.5f), radio)
            .tocar(Marca(0.9f, 0.9f), radio)
        assertEquals(3, conteo.total)
        assertEquals(Marca(0.5f, 0.5f), conteo.marcas[1])
    }

    @Test
    fun tocarSobreUnaMarca_laQuita() {
        val conteo = ConteoMarcas()
            .tocar(Marca(0.2f, 0.2f), radio)
            .tocar(Marca(0.6f, 0.6f), radio)
            .tocar(Marca(0.21f, 0.21f), radio) // cae sobre la primera
        assertEquals(listOf(Marca(0.6f, 0.6f)), conteo.marcas)
    }

    @Test
    fun conVariasCerca_quitaLaMasCercana() {
        val conteo = ConteoMarcas()
            .tocar(Marca(0.30f, 0.30f), radio)
            .tocar(Marca(0.36f, 0.30f), radio) // fuera del radio de la anterior: se agrega
            .tocar(Marca(0.35f, 0.30f), radio) // más cerca de la segunda
        assertEquals(listOf(Marca(0.30f, 0.30f)), conteo.marcas)
    }

    @Test
    fun elRadioConsideraLaProporcionDeLaImagen() {
        // Imagen el doble de ancha que alta: 0,04 en x equivale a 0,08 de alto, fuera del radio
        val conteo = ConteoMarcas()
            .tocar(Marca(0.50f, 0.5f), radio, proporcion = 2f)
            .tocar(Marca(0.54f, 0.5f), radio, proporcion = 2f)
        assertEquals(2, conteo.total)
    }

    @Test
    fun deshacer_vuelveAtrasCadaAccion() {
        var conteo = ConteoMarcas()
        assertFalse(conteo.puedeDeshacer)
        conteo = conteo.tocar(Marca(0.1f, 0.1f), radio).tocar(Marca(0.5f, 0.5f), radio)
        conteo = conteo.tocar(Marca(0.1f, 0.1f), radio) // quita la primera
        assertEquals(1, conteo.total)
        conteo = conteo.deshacer() // la primera vuelve
        assertEquals(2, conteo.total)
        conteo = conteo.deshacer().deshacer()
        assertEquals(0, conteo.total)
        assertFalse(conteo.puedeDeshacer)
        assertEquals(0, conteo.deshacer().total) // deshacer sin historial no hace nada
    }

    @Test
    fun limpiar_quitaTodo_yTambienSePuedeDeshacer() {
        val conMarcas = ConteoMarcas().tocar(Marca(0.1f, 0.1f), radio).tocar(Marca(0.5f, 0.5f), radio)
        val limpio = conMarcas.limpiar()
        assertEquals(0, limpio.total)
        assertTrue(limpio.puedeDeshacer)
        assertEquals(2, limpio.deshacer().total)
        // Limpiar sin marcas no agrega un paso vacío al historial
        assertEquals(ConteoMarcas().puedeDeshacer, ConteoMarcas().limpiar().puedeDeshacer)
    }

    @Test
    fun indPorMetro_convierteSegunElTramo() {
        assertEquals(400, ConteoMarcas.indPorMetro(total = 40, tramoCm = 10))
        assertEquals(120, ConteoMarcas.indPorMetro(total = 60, tramoCm = 50))
        assertEquals(33, ConteoMarcas.indPorMetro(total = 1, tramoCm = 3)) // redondea
        assertEquals(42, ConteoMarcas.indPorMetro(total = 42, tramoCm = 100))
    }

    @Test
    fun indPorMetro_sinMarcasOSinTramo_esNull() {
        assertNull(ConteoMarcas.indPorMetro(total = 0, tramoCm = 50))
        assertNull(ConteoMarcas.indPorMetro(total = 10, tramoCm = null))
        assertNull(ConteoMarcas.indPorMetro(total = 10, tramoCm = 0))
    }
}
