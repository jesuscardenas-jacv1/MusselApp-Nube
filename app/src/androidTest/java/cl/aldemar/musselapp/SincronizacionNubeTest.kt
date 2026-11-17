package cl.aldemar.musselapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.local.aModelo
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.nube.VeredictoNube
import cl.aldemar.musselapp.data.repository.RoomMuestraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de la sincronización con la nube usando Room en memoria y una nube falsa.
 * Fijan el comportamiento actual de [RoomMuestraRepository] frente a subidas y dictámenes.
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionNubeTest {

    private lateinit var db: MusselDatabase
    private lateinit var alcance: CoroutineScope
    private val alcancesExtra = mutableListOf<CoroutineScope>()
    private val revisadas = mutableListOf<Triple<Muestra, EstadoMuestra, String?>>()
    private val supervisor = Usuario("22.222.222-2", "Diego Muñoz", Rol.SUPERVISOR)
    private val eliminadas = mutableListOf<Muestra>()

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusselDatabase::class.java).build()
        DatosIniciales.poblar(db)
        alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun cerrar() = runBlocking {
        // Igual que en RepositoriosRoomTest: nada puede seguir consultando la base después de cerrarla
        alcancesExtra.forEach { it.coroutineContext.job.cancelAndJoin() }
        alcance.coroutineContext.job.cancelAndJoin()
        db.close()
    }

    private fun crearRepositorio(nube: ServicioNube?, alcanceNube: CoroutineScope = alcance) =
        RoomMuestraRepository(
            db, alcance,
            nube = nube,
            alcanceNube = alcanceNube,
            latenciaSincronizacionMs = 0,
            alRevisar = { m, estado, comentario -> synchronized(revisadas) { revisadas += Triple(m, estado, comentario) } },
            alEliminar = { m -> synchronized(eliminadas) { eliminadas += m } },
        )

    private fun alcanceInmediato() =
        CoroutineScope(SupervisorJob() + Dispatchers.Unconfined).also { alcancesExtra += it }

    private fun nueva() = Muestra(
        id = 0, centroId = "quellon", linea = 5, tren = "C", profundidadM = 4, tallaMm = 64,
        conteoIndM = 415, observacion = OBSERVACION_PRUEBA, estado = EstadoMuestra.PENDIENTE,
        registradaEn = System.currentTimeMillis(), sincronizada = false,
        operador = "Carla Vera", operadorRut = "11.111.111-1",
    )

    private suspend fun idDeLaNueva(): Long =
        db.muestraDao().observarTodas().first().first { it.muestra.observacion == OBSERVACION_PRUEBA }.muestra.id

    private suspend fun idSembrada(estado: EstadoMuestra): Long =
        db.muestraDao().observarTodas().first().first { it.muestra.estado == estado }.muestra.id

    private suspend fun muestra(id: Long): Muestra = db.muestraDao().observarPorId(id).first()!!.aModelo()

    private suspend fun cantidadRevisiones(id: Long) = db.muestraDao().observarRevisiones(id).first().size

    private fun revisadasAhora() = synchronized(revisadas) { revisadas.toList() }

    private fun eliminadasAhora() = synchronized(eliminadas) { eliminadas.toList() }

    /** El trabajo con la nube corre en segundo plano: se espera hasta 5 s a que se cumpla la condición. */
    private suspend fun esperar(mensaje: String, condicion: suspend () -> Boolean) {
        try {
            withTimeout(ESPERA_MS) { while (!condicion()) delay(50) }
        } catch (e: TimeoutCancellationException) {
            fail("No ocurrió a tiempo: $mensaje")
        }
    }

    // ── Subida ─────────────────────────────────────────────────────────────

    @Test
    fun agregarMuestra_conNubeOk_quedaSincronizada() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        repositorio.agregarMuestra(nueva())
        val id = idDeLaNueva()
        esperar("la muestra nueva se marca sincronizada") { muestra(id).sincronizada }
        assertTrue(nube.idsSubidos().contains(id))
    }

    @Test
    fun agregarMuestra_conNubeCaida_quedaPendienteYSincronizarLaSubeDespues() = runBlocking {
        val nube = FakeServicioNube(subidaOk = false)
        val repositorio = crearRepositorio(nube)
        repositorio.agregarMuestra(nueva())
        val id = idDeLaNueva()
        esperar("se intenta subir la muestra nueva") { nube.idsSubidos().contains(id) }
        assertFalse(muestra(id).sincronizada)

        nube.subidaOk = true
        val pendientes = db.muestraDao().pendientesDeSincronizar()
        assertTrue(pendientes.contains(id))
        assertEquals(pendientes.size, repositorio.sincronizar())
        assertTrue(muestra(id).sincronizada)
        assertEquals(0, db.muestraDao().pendientesDeSincronizar().size)
    }

    @Test
    fun sinNube_sincronizarMarcaTodasLasPendientes() = runBlocking {
        val repositorio = crearRepositorio(nube = null)
        assertTrue(repositorio.sincronizar() > 0)
        assertEquals(0, db.muestraDao().pendientesDeSincronizar().size)
        assertEquals(0, repositorio.sincronizar())
    }

    // ── Dictámenes que llegan desde la nube ────────────────────────────────

    @Test
    fun veredicto_sobreMuestraPendiente_registraLaRevisionYAvisa() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val revisionesAntes = cantidadRevisiones(id)
        val revisadaEn = System.currentTimeMillis()

        nube.emitir(VeredictoNube(id, EstadoMuestra.VALIDADO, "Conforme", revisadaEn))

        // El dictamen se aplica en dos pasos (revisión y marca de sincronizada): se espera a los dos
        esperar("la muestra pasa a VALIDADO y queda sincronizada") {
            muestra(id).let { it.estado == EstadoMuestra.VALIDADO && it.sincronizada }
        }
        val m = muestra(id)
        assertEquals("Conforme", m.comentarioSupervisor)
        assertEquals("Diego Muñoz", m.revisadaPor)
        assertEquals(revisadaEn, m.revisadaEn)
        assertEquals(revisionesAntes + 1, cantidadRevisiones(id))
        esperar("se avisa una vez al operario") { revisadasAhora().size == 1 }
        val aviso = revisadasAhora().single()
        assertEquals(id, aviso.first.id)
        assertEquals(EstadoMuestra.VALIDADO, aviso.second)
        assertEquals("Conforme", aviso.third)
    }

    @Test
    fun veredicto_sobreMuestraYaRevisadaYSincronizada_seIgnora() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.VALIDADO)
        // Su dictamen ya está en la nube: uno distinto que llegue después no lo cambia
        db.muestraDao().marcarSincronizada(id)
        val revisionesAntes = cantidadRevisiones(id)

        nube.emitir(VeredictoNube(id, EstadoMuestra.CORREGIDO, "Segundo dictamen", System.currentTimeMillis()))
        delay(ESPERA_NEGATIVA_MS)

        assertEquals(EstadoMuestra.VALIDADO, muestra(id).estado)
        assertEquals(revisionesAntes, cantidadRevisiones(id))
        assertTrue(revisadasAhora().isEmpty())
    }

    @Test
    fun veredicto_deMuestraInexistente_seIgnoraSinFallar() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val total = db.muestraDao().contar()

        nube.emitir(VeredictoNube(ID_INEXISTENTE, EstadoMuestra.VALIDADO, null, System.currentTimeMillis()))
        delay(ESPERA_NEGATIVA_MS)

        assertEquals(total, db.muestraDao().contar())
        assertTrue(revisadasAhora().isEmpty())
    }

    // ── F2: carrera al arrancar ────────────────────────────────────────────

    /**
     * Al abrir la app, Firestore puede entregar su snapshot antes de que Room emita la primera lista.
     * El dictamen debe aplicarse igual: el repositorio tiene que consultar Room, no la lista en memoria.
     * La nube falsa emite el veredicto en el mismo instante en que el repositorio se suscribe, y el
     * alcance inmediato hace que se procese antes de que exista la lista en memoria.
     */
    @Test
    fun veredicto_queLlegaAntesDeLaPrimeraListaDeRoom_seAplicaIgual() = runBlocking {
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val nube = FakeServicioNube(
            veredictosIniciales = listOf(VeredictoNube(id, EstadoMuestra.VALIDADO, "Llegó primero", System.currentTimeMillis())),
        )

        crearRepositorio(nube, alcanceNube = alcanceInmediato())

        esperar("el dictamen que llegó antes que Room se aplica") {
            muestra(id).let { it.estado == EstadoMuestra.VALIDADO && it.sincronizada }
        }
        assertEquals("Llegó primero", muestra(id).comentarioSupervisor)
        esperar("se avisa al operario") { revisadasAhora().size == 1 }
    }

    // ── F3: dictamen hecho en el teléfono ──────────────────────────────────

    @Test
    fun revisarEnElTelefono_subeElDictamenDeInmediato() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)

        repositorio.actualizarEstado(id, EstadoMuestra.VALIDADO, "Conforme", supervisor)

        esperar("el dictamen del teléfono llega a la nube") { nube.dictamenEnLaNube(id) != null }
        val enLaNube = nube.dictamenEnLaNube(id)!!
        assertEquals(EstadoMuestra.VALIDADO, enLaNube.estado)
        assertEquals("Conforme", enLaNube.comentario)
        esperar("la muestra queda sincronizada") { muestra(id).sincronizada }
        assertEquals(EstadoMuestra.VALIDADO, muestra(id).estado)
        // El dictamen lo hizo este teléfono: no se avisa de vuelta
        assertTrue(revisadasAhora().isEmpty())
    }

    @Test
    fun revisarEnElTelefono_sinConexion_quedaPendienteYSincronizarLoSube() = runBlocking {
        val nube = FakeServicioNube(subidaOk = false)
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val intentosAntes = nube.idsSubidos().count { it == id }

        repositorio.actualizarEstado(id, EstadoMuestra.CORREGIDO, "Repetir conteo", supervisor)

        esperar("se intenta subir el dictamen") { nube.idsSubidos().count { it == id } > intentosAntes }
        assertFalse(muestra(id).sincronizada)
        assertNull(nube.dictamenEnLaNube(id))

        nube.subidaOk = true
        assertTrue(repositorio.sincronizar() > 0)
        assertTrue(muestra(id).sincronizada)
        assertEquals(EstadoMuestra.CORREGIDO, nube.dictamenEnLaNube(id)?.estado)
        assertEquals("Repetir conteo", nube.dictamenEnLaNube(id)?.comentario)
    }

    /**
     * El supervisor revisó en el teléfono, pero la muestra ya tenía en la nube un dictamen distinto del
     * portal que este teléfono no había recibido. Vale el de la nube (llegó primero): no se pisa, el
     * teléfono lo adopta y avisa. El dictamen de la nube trae una fecha anterior a la del teléfono.
     */
    @Test
    fun revisarEnElTelefono_conOtroDictamenYaEnLaNube_valeElDeLaNube() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val revisionesAntes = cantidadRevisiones(id)
        nube.dictaminarEnPortal(VeredictoNube(id, EstadoMuestra.CORREGIDO, "Repetir conteo", FECHA_PORTAL))

        repositorio.actualizarEstado(id, EstadoMuestra.VALIDADO, "Conforme", supervisor)

        esperar("el teléfono adopta el dictamen de la nube") {
            muestra(id).let { it.estado == EstadoMuestra.CORREGIDO && it.sincronizada }
        }
        val m = muestra(id)
        assertEquals("Repetir conteo", m.comentarioSupervisor)
        assertEquals(FECHA_PORTAL, m.revisadaEn)
        // En la nube sigue el primer dictamen
        assertEquals(EstadoMuestra.CORREGIDO, nube.dictamenEnLaNube(id)?.estado)
        assertEquals("Repetir conteo", nube.dictamenEnLaNube(id)?.comentario)
        // Quedan los dos en el historial: el del teléfono y el que terminó valiendo
        assertEquals(revisionesAntes + 2, cantidadRevisiones(id))
        esperar("se avisa una vez del dictamen que vale") { revisadasAhora().size == 1 }
        assertEquals(EstadoMuestra.CORREGIDO, revisadasAhora().single().second)
        assertEquals(0, db.muestraDao().pendientesDeSincronizar().count { it == id })
    }

    @Test
    fun revisarEnElTelefono_conElMismoDictamenYaEnLaNube_quedaSincronizadaSinAvisar() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        nube.dictaminarEnPortal(VeredictoNube(id, EstadoMuestra.VALIDADO, "Conforme (portal)", FECHA_PORTAL))

        repositorio.actualizarEstado(id, EstadoMuestra.VALIDADO, "Conforme", supervisor)

        esperar("la muestra queda sincronizada") { muestra(id).sincronizada }
        assertEquals(EstadoMuestra.VALIDADO, muestra(id).estado)
        // No se pisa lo que ya estaba en la nube
        assertEquals("Conforme (portal)", nube.dictamenEnLaNube(id)?.comentario)
        delay(ESPERA_NEGATIVA_MS)
        assertTrue(revisadasAhora().isEmpty())
    }

    /** Igual que el caso anterior al de arriba, pero el dictamen de la nube llega por el listener antes que la subida. */
    @Test
    fun veredictoDistinto_sobreDictamenDelTelefonoSinSubir_valeElDeLaNube() = runBlocking {
        val nube = FakeServicioNube(subidaOk = false)
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val intentosAntes = nube.idsSubidos().count { it == id }
        repositorio.actualizarEstado(id, EstadoMuestra.VALIDADO, "Conforme", supervisor)
        esperar("se intenta subir el dictamen") { nube.idsSubidos().count { it == id } > intentosAntes }
        assertFalse(muestra(id).sincronizada)

        nube.emitir(VeredictoNube(id, EstadoMuestra.CORREGIDO, "Repetir conteo", FECHA_PORTAL))

        esperar("el teléfono adopta el dictamen de la nube") {
            muestra(id).let { it.estado == EstadoMuestra.CORREGIDO && it.sincronizada }
        }
        assertEquals("Repetir conteo", muestra(id).comentarioSupervisor)
        esperar("se avisa una vez") { revisadasAhora().size == 1 }
        assertEquals(EstadoMuestra.CORREGIDO, revisadasAhora().single().second)
    }

    @Test
    fun sinNube_revisarEnElTelefono_funcionaComoAntes() = runBlocking {
        val repositorio = crearRepositorio(nube = null)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)
        val revisionesAntes = cantidadRevisiones(id)

        repositorio.actualizarEstado(id, EstadoMuestra.VALIDADO, "Conforme", supervisor)

        val m = muestra(id)
        assertEquals(EstadoMuestra.VALIDADO, m.estado)
        assertEquals("Conforme", m.comentarioSupervisor)
        assertFalse(m.sincronizada)
        assertEquals(revisionesAntes + 1, cantidadRevisiones(id))
    }

    // ── N9: muestra borrada en el portal ───────────────────────────────────

    @Test
    fun eliminadaEnElPortal_seBorraDelTelefonoConSusRevisionesYAvisa() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.VALIDADO)
        assertTrue(cantidadRevisiones(id) > 0)
        val total = db.muestraDao().contar()

        nube.eliminar(id)

        esperar("la muestra desaparece de Room") { db.muestraDao().observarPorId(id).first() == null }
        assertEquals(total - 1, db.muestraDao().contar())
        assertEquals(0, cantidadRevisiones(id))
        esperar("se avisa una vez al operario") { eliminadasAhora().size == 1 }
        assertEquals(id, eliminadasAhora().single().id)
        esperar("la lista viva ya no la tiene") { repositorio.muestras.value.none { it.id == id } }
    }

    @Test
    fun eliminadaEnElPortal_idInexistente_seIgnoraSinAvisar() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val total = db.muestraDao().contar()

        nube.eliminar(ID_INEXISTENTE)
        delay(ESPERA_NEGATIVA_MS)

        assertEquals(total, db.muestraDao().contar())
        assertTrue(eliminadasAhora().isEmpty())
    }

    @Test
    fun eliminadaEnElPortal_dosVeces_avisaUnaSolaVez() = runBlocking {
        val nube = FakeServicioNube()
        val repositorio = crearRepositorio(nube)
        esperar("Room entrega la primera lista") { repositorio.muestras.value.isNotEmpty() }
        val id = idSembrada(EstadoMuestra.PENDIENTE)

        nube.eliminar(id)
        nube.eliminar(id)

        esperar("la muestra desaparece de Room") { db.muestraDao().observarPorId(id).first() == null }
        delay(ESPERA_NEGATIVA_MS)
        assertEquals(1, eliminadasAhora().size)
    }

    private companion object {
        const val OBSERVACION_PRUEBA = "Desde la prueba de nube"
        const val ID_INEXISTENTE = 999_999L
        const val ESPERA_MS = 5_000L
        const val ESPERA_NEGATIVA_MS = 500L
        /** Fecha del dictamen del portal, anterior a cualquier dictamen hecho durante la prueba. */
        const val FECHA_PORTAL = 1_000L
    }
}
