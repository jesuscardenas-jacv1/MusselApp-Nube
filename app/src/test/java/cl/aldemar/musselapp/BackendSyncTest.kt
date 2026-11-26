package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.ValoresOperario
import cl.aldemar.musselapp.data.nube.AlmacenBackend
import cl.aldemar.musselapp.data.nube.BackendApi
import cl.aldemar.musselapp.data.nube.BackendSync
import cl.aldemar.musselapp.data.nube.FotoEnviarDto
import cl.aldemar.musselapp.data.nube.MuestraEnviarDto
import cl.aldemar.musselapp.data.nube.MuestraRemotaDto
import cl.aldemar.musselapp.data.nube.RevisionEnviarDto
import cl.aldemar.musselapp.data.nube.ServidorDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BackendSyncTest {

    private class AlmacenEnMemoria : AlmacenBackend {
        val locales = mutableMapOf<String, Long>()
        val versiones = mutableMapOf<String, Long>()
        val huellas = mutableMapOf<String, String>()
        var usado = false
        override fun instalacion() = "tel01"
        override fun yaRespondio() = usado
        override fun marcarRespuesta() { usado = true }
        override fun idLocalDe(idCliente: String) = locales[idCliente]
        override fun idClienteDe(idLocal: Long) = locales.entries.find { it.value == idLocal }?.key
        override fun asociar(idCliente: String, idLocal: Long) { locales[idCliente] = idLocal }
        override fun versionVista(servidor: String, idCliente: String) = versiones["$servidor|$idCliente"] ?: 0L
        override fun guardarVersion(servidor: String, idCliente: String, version: Long) { versiones["$servidor|$idCliente"] = version }
        override fun huellaEnviada(servidor: String, idCliente: String) = huellas["$servidor|$idCliente"]
        override fun guardarHuella(servidor: String, idCliente: String, huella: String) { huellas["$servidor|$idCliente"] = huella }
    }

    /** Servidor falso con las mismas reglas que el real: no duplica, no pisa y gana el primer dictamen. */
    private class Servidor(var uuid: String = "mac-a") : BackendApi {
        var apagado = false
        var rechaza = emptySet<String>()
        val filas = linkedMapOf<String, MuestraRemotaDto>()
        val dictamenes = mutableListOf<RevisionEnviarDto>()
        val fotos = mutableListOf<Long>()
        val borradas = mutableListOf<Long>()
        val posts = mutableListOf<String>()
        private var reloj = 1000L

        private fun responder() { if (apagado) throw IOException("apagado") }
        override suspend fun servidor(): ServidorDto { responder(); return ServidorDto(uuid) }
        override suspend fun listarMuestras(idCliente: String?): List<MuestraRemotaDto> {
            responder()
            return filas.values.filter { idCliente == null || it.idCliente == idCliente }
        }
        override suspend fun crearMuestra(rol: String, muestra: MuestraEnviarDto): MuestraRemotaDto {
            responder()
            posts += muestra.idCliente
            if (muestra.idCliente in rechaza) throw HttpException(Response.error<Any>(400, okhttp3.ResponseBody.create(null, "")))
            return filas.getOrPut(muestra.idCliente) { fila(muestra.idCliente, muestra) }
        }
        private fun fila(idCliente: String, m: MuestraEnviarDto) = MuestraRemotaDto(
            filas.size + 1L, idCliente, m.centroId, m.tren, m.linea, m.profundidadM, m.tallaMm, m.conteoIndM, m.cholgasIndM,
            m.tramoCm, m.observacion, "PENDIENTE", m.registradaEn, m.operadorRut, null, null, null, null, null, false, ++reloj,
        )
        override suspend fun subirFoto(id: Long, rol: String, foto: FotoEnviarDto) { fotos += id }
        override suspend fun dictaminar(id: Long, rol: String, revision: RevisionEnviarDto): MuestraRemotaDto {
            assertEquals("SUPERVISOR", rol)
            dictamenes += revision
            return editar(id) { it.copy(estado = revision.estado, conteoIndM = revision.conteoIndM ?: it.conteoIndM, conteoOperario = it.conteoIndM, tallaOperario = it.tallaMm) }
        }
        override suspend fun eliminarMuestra(id: Long, rol: String): Response<Unit> {
            borradas += id
            filas.entries.removeIf { it.value.id == id }
            return Response.success(Unit)
        }
        fun editar(id: Long, cambio: (MuestraRemotaDto) -> MuestraRemotaDto): MuestraRemotaDto {
            val (clave, vieja) = filas.entries.first { it.value.id == id }.toPair()
            return cambio(vieja).copy(actualizadaEn = ++reloj).also { filas[clave] = it }
        }
        fun agregar(idCliente: String) { filas[idCliente] = fila(idCliente, modelo(idCliente)) }
        private fun modelo(id: String) = MuestraEnviarDto(
            id, "Muestra", "dalcahue", "B", 8, 3, 62, 390, 6, 40, "swagger", 5000L, "Carla Vera", "111111111",
            null, null, null, null, null, null, null, null, null,
        )
    }

    private val servidor = Servidor()
    private val almacen = AlmacenEnMemoria()
    private val sync = BackendSync(servidor, almacen, codificarFoto = { ruta -> ruta?.let { "AAA" } }, log = { _, _ -> })

    private fun muestra(id: Long = 15, estado: EstadoMuestra = EstadoMuestra.PENDIENTE, conteo: Int = 390, operario: ValoresOperario? = null, foto: String? = null) = Muestra(
        id = id, centroId = "dalcahue", linea = 8, tren = "B", profundidadM = 3, tallaMm = 62, conteoIndM = conteo,
        observacion = "", estado = estado, registradaEn = 10_000L, sincronizada = true, operador = "Carla Vera",
        operadorRut = "111111111", revisadaPor = "Diego Muñoz", comentarioSupervisor = "Recuento", fotoRuta = foto,
        cholgasIndM = 6, tramoCm = 40, valoresOperario = operario,
    )

    @Test
    fun apagado_noLanzaNiDejaNadaPendiente() = runTest {
        servidor.apagado = true
        sync.enviar(listOf(muestra()))
        assertTrue(sync.cambios().isEmpty())
        sync.eliminar(15)
        assertTrue(almacen.huellas.isEmpty())
    }

    @Test
    fun sinRespuesta_soloSiYaSeHabiaUsado() = runTest {
        servidor.apagado = true
        sync.cambios()
        assertFalse(sync.sinRespuesta)
        servidor.apagado = false
        sync.cambios()
        assertFalse(sync.sinRespuesta)
        servidor.apagado = true
        sync.cambios()
        assertTrue(sync.sinRespuesta)
    }

    @Test
    fun enviar_mandaUnaVezYLaFoto_yNoRepiteSiNadaCambio() = runTest {
        sync.enviar(listOf(muestra(foto = "/f/m.jpg")))
        sync.enviar(listOf(muestra(foto = "/f/m.jpg")))
        assertEquals(listOf("tel01_15"), servidor.posts)
        assertEquals(1, servidor.fotos.size)
        assertTrue(sync.cambios().isEmpty())
    }

    @Test
    fun putEnSwagger_seEntregaUnaSolaVez() = runTest {
        sync.enviar(listOf(muestra()))
        servidor.editar(1) { it.copy(conteoIndM = 380) }
        val cambio = sync.cambios().single()
        assertEquals(15L, cambio.idLocal)
        assertEquals(380, cambio.conteoIndM)
        sync.revisado(cambio, 15)
        assertTrue(sync.cambios().isEmpty())
    }

    @Test
    fun correccionDelPortal_llegaAlBackendYNoVuelveElValorViejo() = runTest {
        sync.enviar(listOf(muestra()))
        // El portal corrigió 390 → 380 y Room ya lo tiene
        sync.enviar(listOf(muestra(estado = EstadoMuestra.CORREGIDO, conteo = 380, operario = ValoresOperario(390, 62, 6))))
        val dictamen = servidor.dictamenes.single()
        assertEquals("CORREGIDO", dictamen.estado)
        assertEquals(380, dictamen.conteoIndM)
        assertTrue(sync.cambios().isEmpty())
    }

    @Test
    fun reintentoTrasUnPutDeSwagger_noLoPisaNiLoDaPorVisto() = runTest {
        sync.enviar(listOf(muestra()))
        servidor.editar(1) { it.copy(conteoIndM = 380) }
        sync.enviar(listOf(muestra(foto = "/f/m.jpg"))) // cambió la huella: vuelve a enviar
        assertEquals(380, servidor.filas.getValue("tel01_15").conteoIndM)
        assertEquals(380, sync.cambios().single().conteoIndM)
    }

    @Test
    fun creadaEnSwagger_seImportaYAlDictaminarlaNoSeDuplica() = runTest {
        servidor.agregar("api_abc")
        val cambio = sync.cambios().single()
        assertNull(cambio.idLocal)
        sync.revisado(cambio, 40)
        sync.enviar(listOf(muestra(id = 40, estado = EstadoMuestra.VALIDADO)))
        assertEquals(setOf("api_abc"), servidor.filas.keys)
        assertEquals("VALIDADO", servidor.filas.getValue("api_abc").estado)
        assertNull(servidor.dictamenes.single().conteoIndM)
    }

    @Test
    fun noTraeMuestrasDeOtrosTelefonosNiLasDeEjemplo() = runTest {
        servidor.agregar("otroTel_3")
        servidor.agregar("seed_001")
        assertTrue(sync.cambios().isEmpty())
    }

    @Test
    fun otroComputador_noConfundeLasMuestrasYRecibeLasPropias() = runTest {
        servidor.agregar("api_abc")
        sync.revisado(sync.cambios().single(), 40)
        sync.enviar(listOf(muestra()))
        // Otro Mac: los ids de la tabla vuelven a empezar y son otras muestras
        servidor.uuid = "mac-b"
        servidor.filas.clear()
        servidor.agregar("otroTel_9")
        servidor.agregar("api_xyz")
        val cambio = sync.cambios().single()
        assertEquals("api_xyz", cambio.idCliente)
        assertNull(cambio.idLocal)
        sync.enviar(listOf(muestra()))
        assertTrue("tel01_15" in servidor.filas)
    }

    @Test
    fun rechazada_noFrenaALasDemasNiSeRepite() = runTest {
        servidor.rechaza = setOf("tel01_1")
        sync.enviar(listOf(muestra(id = 1), muestra(id = 2)))
        sync.enviar(listOf(muestra(id = 1), muestra(id = 2)))
        assertEquals(listOf("tel01_1", "tel01_2"), servidor.posts)
    }

    @Test
    fun eliminar_borraSoloLaMuestraPropia() = runTest {
        servidor.agregar("otroTel_3")
        sync.enviar(listOf(muestra()))
        sync.eliminar(15)
        assertEquals(setOf("otroTel_3"), servidor.filas.keys)
    }
}
