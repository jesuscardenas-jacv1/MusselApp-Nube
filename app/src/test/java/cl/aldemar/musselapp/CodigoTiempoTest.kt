package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.clima.ClimaRepository
import cl.aldemar.musselapp.data.clima.MarActual
import cl.aldemar.musselapp.data.clima.TiempoActual
import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.util.CodigoTiempo
import cl.aldemar.musselapp.util.DatoCondicion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

/** Textos del clima en la ficha y unión de las dos respuestas de Open-Meteo. */
class CodigoTiempoTest {

    private val clima = Clima(
        tempAireC = 17.0, vientoKn = 2.4, rachaKn = 5.4, vientoDir = 21, codigo = 3,
        olaM = 0.08, tempAguaC = 16.3, mareaM = 0.28, obtenidoEn = 0L,
    )

    @Test
    fun descripcion_traduceLosCodigosWmo() {
        assertEquals("Despejado", CodigoTiempo.descripcion(0))
        assertEquals("Nublado", CodigoTiempo.descripcion(3))
        assertEquals("Llovizna", CodigoTiempo.descripcion(53))
        assertEquals("Lluvia", CodigoTiempo.descripcion(63))
        assertEquals("Tormenta", CodigoTiempo.descripcion(95))
        assertEquals("Sin dato", CodigoTiempo.descripcion(null))
        assertEquals("Sin dato", CodigoTiempo.descripcion(999))
    }

    @Test
    fun cardinal_segunLosGrados() {
        assertEquals("N", CodigoTiempo.cardinal(0))
        assertEquals("N", CodigoTiempo.cardinal(21))
        assertEquals("NE", CodigoTiempo.cardinal(45))
        assertEquals("S", CodigoTiempo.cardinal(180))
        assertEquals("NO", CodigoTiempo.cardinal(322))
        assertEquals("N", CodigoTiempo.cardinal(350))
        assertNull(CodigoTiempo.cardinal(null))
    }

    @Test
    fun tiempoYMar_seEscribenParaLaFicha() {
        assertEquals("Nublado · 17 °C · viento 2 kn del N (racha 5 kn)", CodigoTiempo.tiempo(clima))
        assertEquals("Ola 0,1 m · agua 16,3 °C · marea +0,28 m", CodigoTiempo.mar(clima))
    }

    @Test
    fun sinClima_diceNoRegistrado() {
        assertEquals("No registrado", CodigoTiempo.tiempo(null))
        assertEquals("No registrado", CodigoTiempo.mar(null))
        // Llegó el tiempo pero no el mar
        assertEquals("No registrado", CodigoTiempo.mar(Clima(tempAireC = 12.0, obtenidoEn = 0L)))
    }

    @Test
    fun fuente_indicaQueEsModeladoYCuandoSeConsulto() {
        val utc = TimeZone.getTimeZone("UTC")
        val consultado = clima.copy(obtenidoEn = 1_795_271_520_000L) // 21/11/2026 14:32 UTC
        assertEquals("21/11/2026 14:32", CodigoTiempo.consultado(consultado, utc))
        assertEquals(
            "Open-Meteo (dato modelado, consultado el 21/11/2026 a las 14:32)",
            CodigoTiempo.fuente(consultado, utc),
        )
        assertEquals("No registrado", CodigoTiempo.consultado(null, utc))
        assertEquals(CodigoTiempo.FUENTE, CodigoTiempo.fuente(null, utc))
    }

    @Test
    fun recuadros_deTiempoYMar() {
        assertEquals(
            listOf(DatoCondicion("Aire", "17", "°C"), DatoCondicion("Viento", "2", "kn del N"), DatoCondicion("Racha", "5", "kn")),
            CodigoTiempo.datosTiempo(clima),
        )
        assertEquals(
            listOf(DatoCondicion("Ola", "0,1", "m"), DatoCondicion("Agua", "16,3", "°C"), DatoCondicion("Marea", "+0,28", "m")),
            CodigoTiempo.datosMar(clima),
        )
    }

    @Test
    fun recuadros_conDatosQueFaltan_muestranRaya() {
        val parcial = Clima(tempAireC = 12.0, obtenidoEn = 0L)
        assertEquals(listOf("12", "—", "—"), CodigoTiempo.datosTiempo(parcial).map { it.valor })
        assertEquals("kn", CodigoTiempo.datosTiempo(parcial)[1].unidad) // sin dirección no dice "del"
        assertEquals(false, CodigoTiempo.hayMar(parcial))
        assertEquals(true, CodigoTiempo.hayMar(clima))
    }

    @Test
    fun posicion_conYSinPrecision() {
        assertEquals("-42.3741, -73.6220 (±12 m)", CodigoTiempo.posicion(Posicion(-42.37412, -73.62204, 12.3)))
        assertEquals("-42.3741, -73.6220", CodigoTiempo.posicion(Posicion(-42.37412, -73.62204)))
        assertEquals("No registrada", CodigoTiempo.posicion(null))
    }

    @Test
    fun combinar_uneTiempoYMar() {
        val c = ClimaRepository.combinar(
            TiempoActual(temperatura = 17.0, viento = 2.4, racha = 5.4, direccion = 20.6, codigo = 3),
            MarActual(ola = 0.08, temperaturaAgua = 16.3, nivelMar = 0.28),
            ahora = 123L,
        )
        assertEquals(clima.copy(obtenidoEn = 123L), c)
    }

    @Test
    fun combinar_conUnaSolaRespuestaOSinNinguna() {
        val soloTiempo = ClimaRepository.combinar(TiempoActual(10.0, 3.0, 6.0, 90.0, 61), null, 1L)
        assertEquals(10.0, soloTiempo?.tempAireC)
        assertNull(soloTiempo?.olaM)
        assertNull(ClimaRepository.combinar(null, null, 1L))
    }
}
