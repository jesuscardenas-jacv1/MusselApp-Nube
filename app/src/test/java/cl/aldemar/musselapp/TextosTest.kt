package cl.aldemar.musselapp

import cl.aldemar.musselapp.util.Textos
import org.junit.Assert.assertEquals
import org.junit.Test

class TextosTest {

    @Test
    fun resultadoSincronizacion_usaSingularYPlural() {
        assertEquals("Todo estaba sincronizado", Textos.resultadoSincronizacion(0, enCola = 0))
        assertEquals("1 muestra sincronizada", Textos.resultadoSincronizacion(1, enCola = 0))
        assertEquals("3 muestras sincronizadas", Textos.resultadoSincronizacion(3, enCola = 0))
    }

    @Test
    fun resultadoSincronizacion_conMuestrasEnCola_noDiceQueTodoEstabaSincronizado() {
        assertEquals(
            "No se pudo enviar: 1 muestra en cola. Se enviará sola al volver internet",
            Textos.resultadoSincronizacion(0, enCola = 1),
        )
        assertEquals(
            "No se pudo enviar: 2 muestras en cola. Se enviarán solas al volver internet",
            Textos.resultadoSincronizacion(0, enCola = 2),
        )
        assertEquals("1 muestra sincronizada, 2 en cola", Textos.resultadoSincronizacion(1, enCola = 2))
    }

    @Test
    fun estadoConexion_indicaInternetYCola() {
        assertEquals("EN LÍNEA", Textos.estadoConexion(enLinea = true, enCola = 0))
        assertEquals("EN LÍNEA · 2 EN COLA", Textos.estadoConexion(enLinea = true, enCola = 2))
        assertEquals("SIN CONEXIÓN", Textos.estadoConexion(enLinea = false, enCola = 0))
        assertEquals("1 EN COLA DE ENVÍO", Textos.estadoConexion(enLinea = false, enCola = 1))
    }
}
