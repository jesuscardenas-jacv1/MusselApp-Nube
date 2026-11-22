package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.util.FichaMuestra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** Contenido de la ficha técnica que se descarga, imprime y comparte. */
class FichaMuestraTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private val muestra = Muestra(
        id = 12, centroId = "dalcahue", linea = 4, tren = "B", profundidadM = 6,
        tallaMm = 65, conteoIndM = 410, observacion = "", estado = EstadoMuestra.CORREGIDO,
        registradaEn = 0L, sincronizada = false, operador = "Carla Vera",
        revisadaPor = "Diego Muñoz", comentarioSupervisor = "Conteo ajustado",
    )

    @Test
    fun filas_sinPosicionNiClima_lodicen() {
        val filas = FichaMuestra.filas(muestra, utc).toMap()
        assertEquals("No registrada", filas["Ubicación"])
        assertEquals("No registrada", filas["Distancia al centro"])
        assertEquals("No registrado", filas["Tiempo"])
        assertEquals("No registrado", filas["Mar"])
        assertEquals("No registrado", filas["Clima consultado"])
        assertEquals("Sin ubicación", filas["Alertas al registrar"])
    }

    @Test
    fun filas_conPosicionYClima() {
        val completa = muestra.copy(
            // Unos 3 km al sur del punto de referencia de Dalcahue
            posicion = Posicion(-42.401, -73.622, 12.0),
            clima = Clima(
                tempAireC = 17.0, vientoKn = 2.4, rachaKn = 5.4, vientoDir = 21, codigo = 3,
                olaM = 0.08, tempAguaC = 16.3, mareaM = 0.28, obtenidoEn = 0L,
            ),
        )
        val filas = FichaMuestra.filas(completa, utc).toMap()
        assertEquals("-42.4010, -73.6220 (±12 m)", filas["Ubicación"])
        assertEquals("3,0 km (fuera del área)", filas["Distancia al centro"])
        assertEquals("Nublado · 17 °C · viento 2 kn del N (racha 5 kn)", filas["Tiempo"])
        assertEquals("Ola 0,1 m · agua 16,3 °C · marea +0,28 m", filas["Mar"])
        assertEquals("01/01/1970 00:00", filas["Clima consultado"])
        assertEquals("Fuera del área", filas["Alertas al registrar"])
        // La sección del PDF también muestra cuándo se consultó el clima
        val condiciones = FichaMuestra.secciones(completa, utc).first { it.titulo == "Condiciones al muestrear" }
        assertEquals("01/01/1970 00:00", condiciones.filas.toMap()["Clima consultado"])
    }

    @Test
    fun nombreYTitulo_usanElCodigoDeLaMuestra() {
        assertEquals("Ficha_Muestra_012.pdf", FichaMuestra.nombreArchivo(muestra))
        assertEquals("Ficha técnica · Muestra 012", FichaMuestra.titulo(muestra))
    }

    @Test
    fun filas_traenLosDatosDeLaMuestra() {
        val filas = FichaMuestra.filas(muestra, utc).toMap()
        assertTrue(filas.getValue("Centro de cultivo").startsWith("Centro Canal Dalcahue"))
        assertEquals("Tren B · L04", filas["Tren / Línea"])
        assertEquals("410 ind/m", filas["Conteo"])
        assertEquals("Corregido", filas["Estado"])
        assertEquals("01/01/1970 00:00", filas["Registrada"])
        assertEquals("Conteo ajustado", filas["Comentario del supervisor"])
        // Campos vacíos se muestran con un guion, no en blanco
        assertEquals("—", filas["Observación de terreno"])
        assertEquals("No (pendiente de envío)", filas["Sincronizada"])
    }

    @Test
    fun textoCompartir_incluyeTituloDatosYPie() {
        val texto = FichaMuestra.textoCompartir(muestra, utc)
        assertTrue(texto.startsWith(FichaMuestra.titulo(muestra)))
        assertTrue(texto.contains("Revisada por: Diego Muñoz"))
        assertTrue(texto.endsWith(FichaMuestra.PIE))
    }
}
