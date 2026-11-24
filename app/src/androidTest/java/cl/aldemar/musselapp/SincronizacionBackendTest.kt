package cl.aldemar.musselapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.local.aModelo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.ValoresOperario
import cl.aldemar.musselapp.data.nube.CambioBackend
import cl.aldemar.musselapp.data.nube.ServicioBackend
import cl.aldemar.musselapp.data.nube.ValoresCorregidos
import cl.aldemar.musselapp.data.nube.VeredictoNube
import cl.aldemar.musselapp.data.repository.ResultadoSincronizacion
import cl.aldemar.musselapp.data.repository.RoomMuestraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room, Firestore (falso) y backend (falso) juntos: el backend no puede pisar ni perder datos. */
@RunWith(AndroidJUnit4::class)
class SincronizacionBackendTest {

    private class BackendFalso : ServicioBackend {
        var pendientes = listOf<CambioBackend>()
        val vistos = mutableListOf<Pair<String, Long?>>()
        var enviadas = listOf<Muestra>()
        val eliminadas = mutableListOf<Long>()
        override suspend fun cambios(): List<CambioBackend> { delay(50); return pendientes }
        override fun revisado(cambio: CambioBackend, idLocal: Long?) {
            vistos += cambio.idCliente to idLocal
            pendientes = pendientes - cambio
        }
        override suspend fun enviar(muestras: List<Muestra>) { enviadas = muestras }
        override suspend fun eliminar(idLocal: Long) { eliminadas += idLocal }
    }

    private lateinit var db: MusselDatabase
    private lateinit var alcance: CoroutineScope
    private val nube = FakeServicioNube()
    private val backend = BackendFalso()
    private lateinit var repo: RoomMuestraRepository
    private var avisos = 0

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusselDatabase::class.java).build()
        DatosIniciales.poblar(db)
        alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = RoomMuestraRepository(db, alcance, nube = nube, latenciaSincronizacionMs = 0, alRevisar = { _, _, _ -> avisos++ }, backend = backend)
    }

    @After
    fun cerrar() = runBlocking {
        alcance.coroutineContext.job.cancelAndJoin()
        db.close()
    }

    private suspend fun todas() = db.muestraDao().observarTodas().first().map { it.aModelo() }
    private suspend fun muestra(id: Long) = db.muestraDao().observarPorId(id).first()!!.aModelo()
    private suspend fun pendiente() = todas().first { it.estado == EstadoMuestra.PENDIENTE }

    private fun cambio(m: Muestra, idLocal: Long? = m.id, conteo: Int = m.conteoIndM, estado: EstadoMuestra = EstadoMuestra.PENDIENTE, operario: ValoresOperario? = null) = CambioBackend(
        idCliente = "x_${m.id}", servidor = "mac-a", version = 9L, idLocal = idLocal, centroId = m.centroId, tren = m.tren, linea = m.linea,
        profundidadM = m.profundidadM, tallaMm = m.tallaMm, conteoIndM = conteo, cholgasIndM = m.cholgasIndM, tramoCm = m.tramoCm,
        observacion = m.observacion, estado = estado, registradaEn = m.registradaEn, operadorRut = m.operadorRut,
        revisadaEn = 99L, comentario = "Desde Swagger", valoresOperario = operario,
    )

    @Test
    fun putDeSwagger_llegaARoomYTambienAFirestore() = runBlocking {
        repo.sincronizar()
        val m = pendiente()
        backend.pendientes = listOf(cambio(m, conteo = 381))
        assertEquals(ResultadoSincronizacion(enviadas = 1, recibidas = 1), repo.sincronizarConDetalle())
        assertEquals(ResultadoSincronizacion(enviadas = 0), repo.sincronizarConDetalle())
        assertEquals(381, muestra(m.id).conteoIndM)
        assertTrue(muestra(m.id).sincronizada)
        assertEquals(m.id, nube.idsSubidos().last())
        assertEquals(0, repo.contarPendientes())
    }

    @Test
    fun correccionDelPortal_noLaPisaUnValorViejoDelBackendYSeLeEnvia() = runBlocking {
        val m = pendiente()
        nube.emitir(VeredictoNube(m.id, EstadoMuestra.CORREGIDO, "Recuento", 50L, ValoresCorregidos(380, m.tallaMm, m.cholgasIndM)))
        withTimeout(5_000) { while (backend.enviadas.none { it.id == m.id && it.conteoIndM == 380 }) delay(50) }
        backend.pendientes = listOf(cambio(m))
        assertEquals(0, repo.sincronizarConDetalle().recibidas)
        assertEquals(380, muestra(m.id).conteoIndM)
        assertEquals(EstadoMuestra.CORREGIDO, muestra(m.id).estado)
    }

    @Test
    fun dictamenDeSwagger_conservaLoDelOperarioAvisaYSubeAFirestore() = runBlocking {
        val m = pendiente()
        backend.pendientes = listOf(cambio(m, conteo = 380, estado = EstadoMuestra.CORREGIDO, operario = ValoresOperario(m.conteoIndM, m.tallaMm, m.cholgasIndM)))
        repo.sincronizar()
        val despues = muestra(m.id)
        assertEquals(EstadoMuestra.CORREGIDO, despues.estado)
        assertEquals(380, despues.conteoIndM)
        assertEquals(m.conteoIndM, despues.valoresOperario?.conteoIndM)
        assertEquals(1, avisos)
        assertEquals(EstadoMuestra.CORREGIDO, nube.dictamenEnLaNube(m.id)?.estado)
    }

    @Test
    fun siElPortalDictaminoPrimero_valeElDelPortal() = runBlocking {
        val m = pendiente()
        nube.dictaminarEnPortal(VeredictoNube(m.id, EstadoMuestra.VALIDADO, "Portal", 10L))
        backend.pendientes = listOf(cambio(m, estado = EstadoMuestra.CORREGIDO))
        repo.sincronizar()
        withTimeout(5_000) { while (muestra(m.id).estado != EstadoMuestra.VALIDADO) delay(50) }
    }

    @Test
    fun creadaEnSwagger_seImportaUnaSolaVezAunqueSeSincroniceDosVecesALaVez() = runBlocking {
        val modelo = pendiente()
        val antes = todas().size
        backend.pendientes = listOf(cambio(modelo.copy(id = 900, registradaEn = 777L, observacion = "creada en swagger"), idLocal = null))
        List(3) { async(Dispatchers.IO) { repo.sincronizar() } }.awaitAll()
        val nueva = todas().single { it.observacion == "creada en swagger" }
        assertEquals(antes + 1, todas().size)
        assertEquals(listOf<Pair<String, Long?>>("x_900" to nueva.id), backend.vistos)
        assertTrue(nube.idsSubidos().contains(nueva.id))
    }

    @Test
    fun mismoIdPeroOtraMuestra_noSeToca() = runBlocking {
        val m = pendiente()
        backend.pendientes = listOf(cambio(m, conteo = 1).copy(registradaEn = 1L))
        repo.sincronizar()
        assertEquals(m.conteoIndM, muestra(m.id).conteoIndM)
    }

    @Test
    fun backendApagado_noDejaMuestrasEnCola() = runBlocking {
        val sinBackend = object : ServicioBackend {
            override suspend fun cambios() = emptyList<CambioBackend>()
            override fun revisado(cambio: CambioBackend, idLocal: Long?) {}
            override suspend fun enviar(muestras: List<Muestra>) {}
            override suspend fun eliminar(idLocal: Long) {}
        }
        val r = RoomMuestraRepository(db, alcance, nube = FakeServicioNube(), latenciaSincronizacionMs = 0, backend = sinBackend)
        r.sincronizar()
        assertEquals(0, r.contarPendientes())
    }

    @Test
    fun eliminadaEnElPortal_seBorraTambienDelBackend() = runBlocking {
        val m = pendiente()
        nube.eliminar(m.id)
        withTimeout(5_000) { while (backend.eliminadas.isEmpty()) delay(50) }
        assertEquals(listOf(m.id), backend.eliminadas)
        assertFalse(todas().any { it.id == m.id })
    }
}
