package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.repository.ResultadoSincronizacion
import cl.aldemar.musselapp.util.Textos
import org.junit.Assert.assertEquals
import org.junit.Test

class TextosTest {

    @Test
    fun resultadoSincronizacion_usaSingularYPlural() {
        assertEquals("Todo al día: no hay muestras pendientes", Textos.resultadoSincronizacion(ResultadoSincronizacion(0), enCola = 0))
        assertEquals("1 muestra sincronizada", Textos.resultadoSincronizacion(ResultadoSincronizacion(1), enCola = 0))
        assertEquals("3 muestras sincronizadas", Textos.resultadoSincronizacion(ResultadoSincronizacion(3), enCola = 0))
    }

    @Test
    fun resultadoSincronizacion_conMuestrasEnCola_noDiceQueTodoEstabaSincronizado() {
        assertEquals(
            "No se pudo enviar: 1 muestra en cola. Se enviará sola al volver internet",
            Textos.resultadoSincronizacion(ResultadoSincronizacion(0), enCola = 1),
        )
        assertEquals(
            "No se pudo enviar: 2 muestras en cola. Se enviarán solas al volver internet",
            Textos.resultadoSincronizacion(ResultadoSincronizacion(0), enCola = 2),
        )
        assertEquals("1 muestra sincronizada, 2 en cola", Textos.resultadoSincronizacion(ResultadoSincronizacion(1), enCola = 2))
    }

    @Test
    fun resultadoSincronizacion_sinInternetYSinNadaEnCola_loDice() {
        assertEquals(
            "Sin conexión. No hay muestras pendientes de envío",
            Textos.resultadoSincronizacion(ResultadoSincronizacion(0), enCola = 0, enLinea = false),
        )
    }

    @Test
    fun resultadoSincronizacion_avisaLoQueLlegoDelServidor() {
        assertEquals("1 muestra actualizada desde el servidor", Textos.resultadoSincronizacion(ResultadoSincronizacion(1, recibidas = 1), 0))
        assertEquals("2 muestras actualizadas desde el servidor, 1 enviada", Textos.resultadoSincronizacion(ResultadoSincronizacion(3, recibidas = 2), 0))
        assertEquals("1 muestra actualizada desde el servidor, 1 en cola", Textos.resultadoSincronizacion(ResultadoSincronizacion(0, recibidas = 1), 1))
    }

    @Test
    fun resultadoSincronizacion_avisaSiElBackendNoResponde() {
        assertEquals(
            "Todo al día: no hay muestras pendientes. El backend no responde",
            Textos.resultadoSincronizacion(ResultadoSincronizacion(0, backendSinRespuesta = true), 0),
        )
    }

    @Test
    fun estadoConexion_indicaInternetYCola() {
        assertEquals("EN LÍNEA", Textos.estadoConexion(enLinea = true, enCola = 0))
        assertEquals("EN LÍNEA · 2 EN COLA", Textos.estadoConexion(enLinea = true, enCola = 2))
        assertEquals("SIN CONEXIÓN", Textos.estadoConexion(enLinea = false, enCola = 0))
        assertEquals("1 EN COLA DE ENVÍO", Textos.estadoConexion(enLinea = false, enCola = 1))
    }
}
