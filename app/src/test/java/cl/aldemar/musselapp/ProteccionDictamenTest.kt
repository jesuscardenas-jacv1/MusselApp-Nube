package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.ProteccionDictamen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F2: un reintento de subida no debe revertir un dictamen que el supervisor ya dejó en la nube. */
class ProteccionDictamenTest {

    /** Lo que sube el teléfono para una muestra que localmente sigue pendiente. */
    private val datosPendiente: Map<String, Any?> = mapOf(
        "codigo" to "Muestra 012",
        "centroId" to "quellon",
        "estado" to "PENDIENTE",
        "conteoIndM" to 410,
        "tallaMm" to 65,
        "cholgasIndM" to 12,
        "observacion" to "Muestreo preliminar",
        "lat" to -43.1,
        "lon" to -73.6,
        "alertas" to listOf("DENSIDAD_BAJA"),
    )

    @Test
    fun yaDictaminada_soloCuandoElEstadoRemotoNoEsPendiente() {
        assertFalse(ProteccionDictamen.yaDictaminada(null))
        assertFalse(ProteccionDictamen.yaDictaminada("PENDIENTE"))
        assertTrue(ProteccionDictamen.yaDictaminada("VALIDADO"))
        assertTrue(ProteccionDictamen.yaDictaminada("CORREGIDO"))
    }

    @Test
    fun reintento_sobreMuestraYaValidadaEnLaNube_noEscribeElEstadoNiLosValoresCorregidos() {
        val datos = ProteccionDictamen.filtrar(datosPendiente, EstadoMuestra.PENDIENTE, estadoRemoto = "CORREGIDO")

        // No se tocan los campos del supervisor
        ProteccionDictamen.CAMPOS_DEL_SUPERVISOR.forEach { campo ->
            assertFalse("no debe escribir $campo", datos.containsKey(campo))
        }
        // El resto se sigue actualizando (por ejemplo, posición y alertas que llegaron después)
        assertEquals("Muestra 012", datos["codigo"])
        assertEquals("quellon", datos["centroId"])
        assertEquals("Muestreo preliminar", datos["observacion"])
        assertEquals(-43.1, datos["lat"])
        assertEquals(listOf("DENSIDAD_BAJA"), datos["alertas"])
    }

    @Test
    fun primeraSubida_sinDocumentoRemoto_escribeTodo() {
        assertEquals(datosPendiente, ProteccionDictamen.filtrar(datosPendiente, EstadoMuestra.PENDIENTE, estadoRemoto = null))
    }

    @Test
    fun reintento_conLaNubeTodaviaPendiente_escribeTodo() {
        assertEquals(datosPendiente, ProteccionDictamen.filtrar(datosPendiente, EstadoMuestra.PENDIENTE, estadoRemoto = "PENDIENTE"))
    }

    @Test
    fun muestraRevisadaEnElTelefono_seSubeCompleta_comoHastaAhora() {
        // Fuera del alcance de F2: la revisión hecha en el teléfono se sigue subiendo igual (ver F3 del plan)
        val datosRevisada = datosPendiente + mapOf("estado" to "VALIDADO", "revisadaPor" to "Diego Muñoz")
        assertEquals(datosRevisada, ProteccionDictamen.filtrar(datosRevisada, EstadoMuestra.VALIDADO, estadoRemoto = "CORREGIDO"))
        assertEquals(datosRevisada, ProteccionDictamen.filtrar(datosRevisada, EstadoMuestra.VALIDADO, estadoRemoto = "PENDIENTE"))
    }
}
