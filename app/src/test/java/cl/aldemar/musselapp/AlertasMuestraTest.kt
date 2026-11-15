package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.Alerta
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.util.AlertasMuestra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de las alertas automáticas, probadas en el borde de cada umbral. */
class AlertasMuestraTest {

    private val dalcahue = CentroCultivo.porId("dalcahue")!!
    private val enElCentro = Posicion(dalcahue.lat, dalcahue.lon, precisionM = 8.0)

    /** Muestra normal: dentro del área, densidad estándar y buen tiempo. */
    private fun muestra(
        conteo: Int = 380,
        talla: Int = 60,
        cholgas: Int? = 0,
        posicion: Posicion? = enElCentro,
        clima: Clima? = Clima(rachaKn = 10.0, olaM = 0.3, obtenidoEn = 0L),
    ) = Muestra(
        id = 1, centroId = "dalcahue", linea = 1, tren = "A", profundidadM = 3, tallaMm = talla,
        conteoIndM = conteo, observacion = "", estado = EstadoMuestra.PENDIENTE, registradaEn = 0L,
        sincronizada = false, cholgasIndM = cholgas, posicion = posicion, clima = clima,
    )

    private fun alertas(m: Muestra) = AlertasMuestra.evaluar(m)

    @Test
    fun muestraNormal_noTieneAlertas() {
        assertEquals(emptyList<Alerta>(), alertas(muestra()))
    }

    @Test
    fun sinPosicion_avisaSinUbicacionYNoFueraDeArea() {
        assertEquals(listOf(Alerta.SIN_UBICACION), alertas(muestra(posicion = null)))
    }

    @Test
    fun fueraDeArea_soloPasadoElRadioDelCentro() {
        // 0,013° de latitud son unos 1.445 m (dentro de los 1.500 m); 0,014° son unos 1.557 m
        val dentro = Posicion(dalcahue.lat + 0.013, dalcahue.lon, 8.0)
        val fuera = Posicion(dalcahue.lat + 0.014, dalcahue.lon, 8.0)
        assertFalse(Alerta.FUERA_DE_AREA in alertas(muestra(posicion = dentro)))
        assertTrue(Alerta.FUERA_DE_AREA in alertas(muestra(posicion = fuera)))
    }

    @Test
    fun ubicacionImprecisa_sobre100Metros() {
        assertFalse(Alerta.UBICACION_IMPRECISA in alertas(muestra(posicion = enElCentro.copy(precisionM = 100.0))))
        assertTrue(Alerta.UBICACION_IMPRECISA in alertas(muestra(posicion = enElCentro.copy(precisionM = 101.0))))
        assertFalse(Alerta.UBICACION_IMPRECISA in alertas(muestra(posicion = enElCentro.copy(precisionM = null))))
    }

    @Test
    fun densidadBaja_bajo350() {
        assertEquals(emptyList<Alerta>(), alertas(muestra(conteo = 350)))
        assertEquals(listOf(Alerta.DENSIDAD_BAJA), alertas(muestra(conteo = 349)))
    }

    @Test
    fun desvioDeDensidad_sobreEl20PorCientoDelEstandar() {
        // 380 + 20 % = 456
        assertEquals(emptyList<Alerta>(), alertas(muestra(conteo = 456)))
        assertEquals(listOf(Alerta.DESVIO_DENSIDAD), alertas(muestra(conteo = 457)))
    }

    @Test
    fun cholgasAltas_sobreEl10PorCientoDelConteo() {
        assertEquals(emptyList<Alerta>(), alertas(muestra(conteo = 400, cholgas = 40)))
        assertEquals(listOf(Alerta.CHOLGAS_ALTAS), alertas(muestra(conteo = 400, cholgas = 41)))
        assertEquals(emptyList<Alerta>(), alertas(muestra(cholgas = null)))
    }

    @Test
    fun tallaFueraDeRango_bajo30OSobre90() {
        assertEquals(emptyList<Alerta>(), alertas(muestra(talla = 30)))
        assertEquals(emptyList<Alerta>(), alertas(muestra(talla = 90)))
        assertEquals(listOf(Alerta.TALLA_FUERA_DE_RANGO), alertas(muestra(talla = 29)))
        assertEquals(listOf(Alerta.TALLA_FUERA_DE_RANGO), alertas(muestra(talla = 91)))
    }

    @Test
    fun malTiempo_porRachaOPorOla() {
        assertEquals(emptyList<Alerta>(), alertas(muestra(clima = Clima(rachaKn = 25.0, olaM = 1.0, obtenidoEn = 0L))))
        assertEquals(listOf(Alerta.MAL_TIEMPO), alertas(muestra(clima = Clima(rachaKn = 25.5, olaM = 0.2, obtenidoEn = 0L))))
        assertEquals(listOf(Alerta.MAL_TIEMPO), alertas(muestra(clima = Clima(rachaKn = 5.0, olaM = 1.1, obtenidoEn = 0L))))
        // Sin clima no se puede afirmar que hubo mal tiempo
        assertEquals(emptyList<Alerta>(), alertas(muestra(clima = null)))
    }

    @Test
    fun lasCriticasVanPrimero() {
        val m = muestra(conteo = 300, talla = 20, posicion = null)
        assertEquals(
            listOf(Alerta.DENSIDAD_BAJA, Alerta.SIN_UBICACION, Alerta.TALLA_FUERA_DE_RANGO),
            alertas(m),
        )
    }
}
