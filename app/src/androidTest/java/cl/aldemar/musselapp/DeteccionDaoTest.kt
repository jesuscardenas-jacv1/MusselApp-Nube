package cl.aldemar.musselapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.local.entidad.DeteccionEntity
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.local.entidad.OrigenDeteccion
import cl.aldemar.musselapp.data.ml.Especie
import cl.aldemar.musselapp.data.model.EstadoMuestra
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Paso 5: las detecciones del conteo automático se guardan por muestra en Room (v8). */
@RunWith(AndroidJUnit4::class)
class DeteccionDaoTest {

    private lateinit var db: MusselDatabase

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusselDatabase::class.java).build()
        DatosIniciales.poblar(db)
    }

    @After
    fun cerrar() {
        db.close()
    }

    private suspend fun nuevaMuestra(): Long {
        val lineaId = db.catalogoDao().buscarLineaId("dalcahue", "A", 1)!!
        return db.muestraDao().insertar(
            MuestraEntity(
                lineaId = lineaId, operadorRut = DatosIniciales.RUT_OPERARIO, profundidadM = 3, tallaMm = 60,
                conteoIndM = 300, observacion = "", estado = EstadoMuestra.PENDIENTE, registradaEn = 0,
                sincronizada = false, fotoRuta = null,
            ),
        )
    }

    private fun deteccion(especie: Especie, origen: OrigenDeteccion = OrigenDeteccion.MODELO) = DeteccionEntity(
        muestraId = 0, especie = especie,
        especieModelo = especie.takeIf { origen == OrigenDeteccion.MODELO },
        confianza = 0.8f.takeIf { origen == OrigenDeteccion.MODELO },
        x = 0.1f, y = 0.2f, ancho = 0.1f, alto = 0.05f, largoMm = null, origen = origen,
    )

    @Test
    fun guardaYLeeLasDeteccionesDeCadaMuestra() = runBlocking {
        val a = nuevaMuestra()
        val b = nuevaMuestra()
        val dao = db.deteccionDao()
        dao.insertar(listOf(deteccion(Especie.CHORITO).copy(muestraId = a), deteccion(Especie.REFERENCIA).copy(muestraId = a)))
        dao.insertar(listOf(deteccion(Especie.CHOLGA, OrigenDeteccion.OPERARIO).copy(muestraId = b)))

        val deA = dao.deMuestra(a)
        assertEquals(listOf(Especie.CHORITO, Especie.REFERENCIA), deA.map { it.especie })
        assertFalse(deA.first().eliminada)

        val deB = dao.deMuestra(b).single()
        assertEquals(OrigenDeteccion.OPERARIO, deB.origen)
        assertNull(deB.confianza)
        assertNull(deB.especieModelo)
    }

    @Test
    fun reemplazarYBorrarSoloTocanLaMuestraIndicada() = runBlocking {
        val a = nuevaMuestra()
        val b = nuevaMuestra()
        val dao = db.deteccionDao()
        dao.reemplazar(a, List(3) { deteccion(Especie.CHORITO) })
        dao.reemplazar(b, List(2) { deteccion(Especie.CHOLGA) })

        dao.reemplazar(a, listOf(deteccion(Especie.CHOLGA).copy(eliminada = true)))
        assertEquals(1, dao.deMuestra(a).size)
        assertTrue(dao.deMuestra(a).single().eliminada)
        assertEquals(2, dao.deMuestra(b).size)

        assertEquals(2, dao.borrarDeMuestra(b))
        assertTrue(dao.deMuestra(b).isEmpty())
        assertEquals(1, dao.deMuestra(a).size)
    }

    @Test
    fun alBorrarLaMuestraSeBorranSusDetecciones() = runBlocking {
        val a = nuevaMuestra()
        db.deteccionDao().reemplazar(a, List(4) { deteccion(Especie.CHORITO) })

        db.muestraDao().eliminar(a)

        assertTrue(db.deteccionDao().deMuestra(a).isEmpty())
    }

    @Test
    fun lasMuestrasNuevasNacenSinConteoAutomatico() = runBlocking {
        val a = nuevaMuestra()
        db.openHelper.readableDatabase.query("SELECT ml_total, ml_editado FROM muestras WHERE id = $a").use { c ->
            c.moveToNext()
            assertTrue(c.isNull(0))
            assertEquals(0, c.getInt(1))
        }
    }
}
