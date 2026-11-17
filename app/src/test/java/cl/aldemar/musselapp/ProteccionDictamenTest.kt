package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.nube.ProteccionDictamen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F2: un reintento de subida no debe revertir un dictamen que el supervisor ya dejó en la nube.
 * F3: el dictamen hecho en el teléfono se sube, pero si la nube ya tiene uno, vale el de la nube.
 */
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
        val datos = ProteccionDictamen.filtrar(datosPendiente, estadoRemoto = "CORREGIDO")

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
        assertEquals(datosPendiente, ProteccionDictamen.filtrar(datosPendiente, estadoRemoto = null))
    }

    @Test
    fun reintento_conLaNubeTodaviaPendiente_escribeTodo() {
        assertEquals(datosPendiente, ProteccionDictamen.filtrar(datosPendiente, estadoRemoto = "PENDIENTE"))
    }

    // ── F3 ─────────────────────────────────────────────────────────────────

    /** Lo que sube el teléfono para una muestra que el supervisor validó ahí mismo. */
    private val datosRevisada: Map<String, Any?> =
        datosPendiente + mapOf("estado" to "VALIDADO", "revisadaPor" to "Diego Muñoz", "comentarioSupervisor" to "Conforme")

    @Test
    fun muestraRevisadaEnElTelefono_conLaNubePendiente_subeElDictamen() {
        assertEquals(datosRevisada, ProteccionDictamen.filtrar(datosRevisada, estadoRemoto = "PENDIENTE"))
        assertEquals(datosRevisada, ProteccionDictamen.filtrar(datosRevisada, estadoRemoto = null))
    }

    @Test
    fun muestraRevisadaEnElTelefono_conDictamenYaEnLaNube_noLoPisa() {
        listOf("CORREGIDO", "VALIDADO").forEach { remoto ->
            val datos = ProteccionDictamen.filtrar(datosRevisada, estadoRemoto = remoto)
            ProteccionDictamen.CAMPOS_DEL_SUPERVISOR.forEach { campo ->
                assertFalse("con la nube en $remoto no debe escribir $campo", datos.containsKey(campo))
            }
            assertEquals("Muestreo preliminar", datos["observacion"])
        }
    }

    @Test
    fun prevaleceRemoto_soloSiLosDosDictamenesSonDistintos() {
        assertTrue(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.VALIDADO, "CORREGIDO"))
        assertTrue(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.CORREGIDO, "VALIDADO"))
        // Mismo dictamen en los dos lados: no hay nada que adoptar
        assertFalse(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.VALIDADO, "VALIDADO"))
        // La nube sigue pendiente o no tiene la muestra: se sube el dictamen del teléfono
        assertFalse(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.VALIDADO, "PENDIENTE"))
        assertFalse(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.VALIDADO, null))
        // Muestra local pendiente: es el reintento de F2, el dictamen llega por el listener
        assertFalse(ProteccionDictamen.prevaleceRemoto(EstadoMuestra.PENDIENTE, "VALIDADO"))
    }

    @Test
    fun debeAplicarRemoto_muestraLocalPendiente_siempreAplica() {
        assertTrue(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.PENDIENTE, sincronizadaLocal = true, EstadoMuestra.VALIDADO))
        assertTrue(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.PENDIENTE, sincronizadaLocal = false, EstadoMuestra.CORREGIDO))
    }

    @Test
    fun debeAplicarRemoto_dictamenLocalSinSubir_cedeAnteUnoDistintoDeLaNube() {
        assertTrue(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.VALIDADO, sincronizadaLocal = false, EstadoMuestra.CORREGIDO))
        // Si es el mismo, no hay nada que cambiar
        assertFalse(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.VALIDADO, sincronizadaLocal = false, EstadoMuestra.VALIDADO))
    }

    @Test
    fun debeAplicarRemoto_dictamenLocalYaSincronizado_seMantiene() {
        assertFalse(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.VALIDADO, sincronizadaLocal = true, EstadoMuestra.CORREGIDO))
        assertFalse(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.CORREGIDO, sincronizadaLocal = true, EstadoMuestra.CORREGIDO))
    }

    @Test
    fun debeAplicarRemoto_nuncaAplicaUnPendiente() {
        assertFalse(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.PENDIENTE, sincronizadaLocal = false, EstadoMuestra.PENDIENTE))
        assertFalse(ProteccionDictamen.debeAplicarRemoto(EstadoMuestra.VALIDADO, sincronizadaLocal = false, EstadoMuestra.PENDIENTE))
    }
}
