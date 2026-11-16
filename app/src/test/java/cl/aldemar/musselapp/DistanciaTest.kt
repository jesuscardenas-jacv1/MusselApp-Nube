package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.util.Distancia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Distancia entre la muestra y su centro de cultivo. */
class DistanciaTest {

    private val dalcahue = CentroCultivo.porId("dalcahue")!!

    private fun muestra(posicion: Posicion?) = Muestra(
        id = 1, centroId = "dalcahue", linea = 1, tren = "A", profundidadM = 3, tallaMm = 60,
        conteoIndM = 400, observacion = "", estado = EstadoMuestra.PENDIENTE, registradaEn = 0L,
        sincronizada = false, posicion = posicion,
    )

    @Test
    fun mismoPunto_esCero() {
        assertEquals(0.0, Distancia.metros(-42.374, -73.622, -42.374, -73.622), 0.001)
    }

    @Test
    fun unGradoDeLatitud_sonUnos111Km() {
        assertEquals(111_195.0, Distancia.metros(-42.0, -73.0, -43.0, -73.0), 100.0)
    }

    @Test
    fun entreDalcahueYCalbuco_sonUnos74Km() {
        val calbuco = CentroCultivo.porId("calbuco")!!
        val metros = Distancia.metros(dalcahue.lat, dalcahue.lon, calbuco.lat, calbuco.lon)
        assertEquals(74_000.0, metros, 2_000.0)
    }

    @Test
    fun alCentro_sinPosicionOSinCentro_esNull() {
        assertNull(Distancia.alCentro(muestra(null)))
        assertNull(Distancia.alCentro(muestra(Posicion(-42.374, -73.622)), centro = null))
    }

    @Test
    fun alCentro_enElPuntoDelCentro_esCero() {
        assertEquals(0, Distancia.alCentro(muestra(Posicion(dalcahue.lat, dalcahue.lon))))
    }

    @Test
    fun texto_usaMetrosBajoElKilometroYKilometrosSobreEl() {
        assertEquals("180 m", Distancia.texto(180))
        assertEquals("3,2 km", Distancia.texto(3_200))
    }
}
