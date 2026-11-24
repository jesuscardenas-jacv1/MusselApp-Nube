package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.nube.AlmacenSyncBackend
import cl.aldemar.musselapp.data.nube.BackendApi
import cl.aldemar.musselapp.data.nube.BackendSync
import cl.aldemar.musselapp.data.nube.CambioRemoto
import cl.aldemar.musselapp.data.nube.FotoEnviarDto
import cl.aldemar.musselapp.data.nube.FotoRemotaDto
import cl.aldemar.musselapp.data.nube.MuestraEnviarDto
import cl.aldemar.musselapp.data.nube.MuestraRemotaDto
import cl.aldemar.musselapp.data.nube.NubeCompuesta
import cl.aldemar.musselapp.data.nube.ReporteResumenDto
import cl.aldemar.musselapp.data.nube.RevisionEnviarDto
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.nube.VeredictoNube
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendSyncTest {

    private class AlmacenEnMemoria(private val idInstalacion: String = "tel01") : AlmacenSyncBackend {
        val mapa = mutableMapOf<String, Long>()
        override fun instalacion(): String = idInstalacion
        override fun idLocalDe(idRemoto: String): Long? = mapa[idRemoto]
        override fun guardarIdLocal(idRemoto: String, idLocal: Long) { mapa[idRemoto] = idLocal }
    }

    private class BackendApiFalsa : BackendApi {
        var fallar = false
        val recibidas = mutableListOf<Pair<String, MuestraEnviarDto>>()
        val fotos = mutableListOf<Triple<Long, String, FotoEnviarDto>>()
        val revisiones = mutableListOf<Triple<Long, String, RevisionEnviarDto>>()
        var listaRemota: List<MuestraRemotaDto> = emptyList()
        var estadoRemotoAlCrear: String = EstadoMuestra.PENDIENTE.name

        override suspend fun listarMuestras(): List<MuestraRemotaDto> {
            if (fallar) error("servidor apagado")
            return listaRemota
        }

        override suspend fun crearOActualizarMuestra(rol: String, muestra: MuestraEnviarDto): MuestraRemotaDto {
            if (fallar) error("servidor apagado")
            recibidas += rol to muestra
            return MuestraRemotaDto(
                id = 100L + recibidas.size,
                idCliente = muestra.idCliente,
                codigo = muestra.codigo,
                centroId = muestra.centroId,
                tren = muestra.tren,
                linea = muestra.linea,
                profundidadM = muestra.profundidadM,
                tallaMm = muestra.tallaMm,
                conteoIndM = muestra.conteoIndM,
                cholgasIndM = muestra.cholgasIndM,
                tramoCm = muestra.tramoCm,
                observacion = muestra.observacion,
                estado = estadoRemotoAlCrear,
                registradaEn = muestra.registradaEn,
                operador = muestra.operador,
                operadorRut = muestra.operadorRut,
                revisadaPor = null,
                revisadaEn = null,
                comentarioSupervisor = null,
                actualizadaEn = muestra.registradaEn,
            )
        }

        override suspend fun actualizarMuestra(id: Long, rol: String, muestra: MuestraEnviarDto): MuestraRemotaDto =
            crearOActualizarMuestra(rol, muestra)

        override suspend fun subirFoto(id: Long, rol: String, foto: FotoEnviarDto): FotoRemotaDto {
            if (fallar) error("error foto")
            fotos += Triple(id, rol, foto)
            return FotoRemotaDto(1L, id, foto.nombreArchivo, foto.tipoMime, foto.contenidoBase64.length.toLong())
        }

        override suspend fun dictaminar(id: Long, rol: String, revision: RevisionEnviarDto): MuestraRemotaDto {
            if (fallar) error("error dictamen")
            revisiones += Triple(id, rol, revision)
            return MuestraRemotaDto(
                id = id, idCliente = null, codigo = "Muestra 001", centroId = "dalcahue", tren = "A", linea = 1,
                profundidadM = 3, tallaMm = 54, conteoIndM = 400, cholgasIndM = null, tramoCm = 100,
                observacion = "", estado = revision.estado, registradaEn = 1000L, operador = "Carla Vera",
                operadorRut = "111111111", revisadaPor = revision.supervisorNombre, revisadaEn = 2000L,
                comentarioSupervisor = revision.comentario, actualizadaEn = 2000L,
            )
        }

        override suspend fun resumenReportes(): ReporteResumenDto =
            ReporteResumenDto(0, 0, 0, 0, 0.0, 0.0)
    }

    private fun muestraDemo(
        id: Long = 15L,
        estado: EstadoMuestra = EstadoMuestra.PENDIENTE,
        fotoRuta: String? = null,
        comentario: String? = null,
    ) = Muestra(
        id = id,
        centroId = "dalcahue",
        linea = 4,
        tren = "B",
        profundidadM = 3,
        tallaMm = 55,
        conteoIndM = 410,
        observacion = "Fijación pareja",
        estado = estado,
        registradaEn = 10_000L,
        sincronizada = false,
        operador = "Carla Vera",
        operadorRut = "111111111",
        revisadaPor = if (estado != EstadoMuestra.PENDIENTE) "Diego Muñoz" else null,
        comentarioSupervisor = comentario,
        fotoRuta = fotoRuta,
        cholgasIndM = 10,
        tramoCm = 100,
    )

    @Test
    fun subir_enviaMuestraFotoYDictamenYGuardaMapa() = runTest {
        val api = BackendApiFalsa()
        val almacen = AlmacenEnMemoria("tel01")
        val sync = BackendSync(api, almacen, codificarFoto = { if (it != null) "data:image/jpeg;base64,ABC" else null })

        val ok = sync.subir(
            muestraDemo(
                id = 15L,
                estado = EstadoMuestra.VALIDADO,
                fotoRuta = "/fotos/m15.jpg",
                comentario = "Correcto",
            ),
        )

        assertTrue(ok)
        assertEquals(1, api.recibidas.size)
        assertEquals("OPERARIO", api.recibidas[0].first)
        assertEquals("tel01_15", api.recibidas[0].second.idCliente)
        assertEquals(15L, almacen.idLocalDe("101"))
        assertEquals(1, api.fotos.size)
        assertEquals("m15.jpg", api.fotos[0].third.nombreArchivo)
        assertEquals(1, api.revisiones.size)
        assertEquals("SUPERVISOR", api.revisiones[0].second)
        assertEquals("VALIDADO", api.revisiones[0].third.estado)
    }

    @Test
    fun subir_sinServidor_devuelveFalseSinLanzar() = runTest {
        val api = BackendApiFalsa().apply { fallar = true }
        val sync = BackendSync(api, AlmacenEnMemoria(), codificarFoto = { null })

        assertFalse(sync.subir(muestraDemo()))
    }

    @Test
    fun descargarCambios_filtraSemillasIntactasYResuelveIdsLocales() = runTest {
        val api = BackendApiFalsa()
        val almacen = AlmacenEnMemoria("tel01").apply { guardarIdLocal("50", 99L) }
        api.listaRemota = listOf(
            // Semilla sin editar: se ignora
            MuestraRemotaDto(1L, "seed_001", "Muestra 001", "dalcahue", "A", 1, 3, 50, 380, null, 100, "", "PENDIENTE", 1000L, "Carla", "111111111", null, null, null, false, 1000L),
            // Muestra propia del teléfono editada en Swagger
            MuestraRemotaDto(2L, "tel01_15", "Muestra 015", "dalcahue", "B", 4, 3, 60, 450, 8, 100, "Editada en Swagger", "PENDIENTE", 2000L, "Carla", "111111111", null, null, null, false, 3000L),
            // Muestra creada en Swagger ya mapeada antes
            MuestraRemotaDto(50L, null, "Muestra 050", "calbuco", "A", 2, 1, 52, 390, null, 100, "Swagger", "VALIDADO", 4000L, "Carla", "111111111", "Diego Muñoz", 5000L, "Ok", false, 5000L),
            // Muestra nueva creada en Swagger
            MuestraRemotaDto(51L, null, "Muestra 051", "quellon", "C", 3, 5, 58, 415, 5, 100, "Nueva Swagger", "PENDIENTE", 6000L, "Carla", "111111111", null, null, null, false, 6000L),
        )
        val sync = BackendSync(api, almacen, codificarFoto = { null })

        val cambios = sync.descargarCambios()

        assertEquals(3, cambios.size)
        assertEquals(15L, cambios[0].idLocalExistente)
        assertEquals("Editada en Swagger", cambios[0].observacion)
        assertEquals(99L, cambios[1].idLocalExistente)
        assertEquals(EstadoMuestra.VALIDADO, cambios[1].estado)
        assertNull(cambios[2].idLocalExistente)
        assertEquals("51", cambios[2].idRemoto)
    }

    @Test
    fun nubeCompuesta_intentaAmbasYExigeConfirmacionDeAmbas() = runTest {
        var llamadasPrimaria = 0
        var llamadasSecundaria = 0
        val okNube = object : ServicioNube {
            override suspend fun subir(m: Muestra): Boolean { llamadasSecundaria++; return true }
            override fun escucharVeredictos(alEliminar: (Long) -> Unit, alAplicar: (VeredictoNube) -> Unit) {}
        }
        val fallaNube = object : ServicioNube {
            override suspend fun subir(m: Muestra): Boolean { llamadasPrimaria++; return false }
            override fun escucharVeredictos(alEliminar: (Long) -> Unit, alAplicar: (VeredictoNube) -> Unit) {}
            override suspend fun descargarCambios(): List<CambioRemoto> = listOf(
                CambioRemoto("7", null, "dalcahue", "A", 1, 3, 55, 400, null, 100, "Swagger", EstadoMuestra.PENDIENTE, 1000L, "Carla", "111111111", null, null, null),
            )
        }

        val compuesta = NubeCompuesta(primaria = fallaNube, secundaria = okNube)
        assertFalse(compuesta.subir(muestraDemo()))
        assertEquals(1, llamadasPrimaria)
        assertEquals(1, llamadasSecundaria)
        assertEquals(1, compuesta.descargarCambios().size)
    }
}
