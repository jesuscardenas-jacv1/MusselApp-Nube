package cl.aldemar.musselapp.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorSimuladoTest {

    private val vertical = DetectorSimulado.generar(3000, 4000)

    @Test
    fun entregaCuarentaMoluscosYUnaReferencia() {
        assertEquals(41, vertical.size)
        assertEquals(DetectorSimulado.CHORITOS, vertical.count { it.especie == Especie.CHORITO })
        assertEquals(DetectorSimulado.CHOLGAS, vertical.count { it.especie == Especie.CHOLGA })
        assertEquals(1, vertical.count { it.especie == Especie.REFERENCIA })
    }

    @Test
    fun losRecuadrosQuedanDentroDeLaFotoYLaConfianzaEntreCeroYUno() {
        for (foto in listOf(vertical, DetectorSimulado.generar(4000, 3000), DetectorSimulado.generar(384, 384))) {
            foto.forEach { d ->
                assertTrue(d.toString(), d.x >= 0f && d.y >= 0f && d.ancho > 0f && d.alto > 0f)
                assertTrue(d.toString(), d.x + d.ancho <= 1f && d.y + d.alto <= 1f)
                assertTrue(d.toString(), d.confianza in 0f..1f)
            }
        }
    }

    @Test
    fun laReferenciaEsCuadradaEnPixelesYDaCalibre() {
        for ((ancho, alto) in listOf(3000 to 4000, 4000 to 3000)) {
            val r = DetectorSimulado.generar(ancho, alto).single { it.especie == Especie.REFERENCIA }
            val ladoX = r.ancho * ancho
            val ladoY = r.alto * alto
            assertEquals(ladoX.toDouble(), ladoY.toDouble(), 0.5)
            assertNotNull(Calibre.mmPorPixel(ladoX.toDouble(), ladoY.toDouble()))
        }
    }

    @Test
    fun siempreDaLoMismoParaElMismoTamano() {
        assertEquals(vertical, DetectorSimulado.generar(3000, 4000))
    }

    @Test
    fun laVersionDiceQueEsSimulado() {
        assertEquals("simulado", DetectorSimulado.VERSION)
    }
}
