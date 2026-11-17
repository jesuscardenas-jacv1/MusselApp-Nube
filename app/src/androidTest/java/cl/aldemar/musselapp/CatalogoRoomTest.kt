package cl.aldemar.musselapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.data.local.entidad.LineaEntity
import cl.aldemar.musselapp.data.local.entidad.TrenEntity
import cl.aldemar.musselapp.data.model.CatalogoCultivo
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.repository.CatalogoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** N8: los trenes y líneas del formulario salen de las tablas `trenes` y `lineas`. */
@RunWith(AndroidJUnit4::class)
class CatalogoRoomTest {

    private lateinit var db: MusselDatabase
    private lateinit var repositorio: CatalogoRepository

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusselDatabase::class.java).build()
        DatosIniciales.poblar(db)
        repositorio = CatalogoRepository(db.catalogoDao())
    }

    @After
    fun cerrar() {
        db.close()
    }

    /** Con los datos iniciales el formulario ofrece lo mismo que la lista fija anterior: A, B, C y 12 líneas. */
    @Test
    fun cadaCentro_ofreceLasMismasListasQueAntes() = runBlocking {
        CentroCultivo.disponibles.forEach { centro ->
            assertEquals(centro.id, CatalogoCultivo.porDefecto, repositorio.observar(centro.id).first())
        }
    }

    @Test
    fun centroSinTrenes_usaElCatalogoPorDefecto() = runBlocking {
        assertEquals(CatalogoCultivo.porDefecto, repositorio.observar("no-existe").first())
    }

    @Test
    fun trenNuevoEnLaBase_apareceSoloEnSuCentro() = runBlocking {
        val trenId = db.catalogoDao().insertarTren(TrenEntity(centroId = "quellon", codigo = "D"))
        db.catalogoDao().insertarLineas(
            listOf(LineaEntity(trenId = trenId, numero = 2), LineaEntity(trenId = trenId, numero = 1)),
        )

        val quellon = repositorio.observar("quellon").first()
        assertEquals(listOf("A", "B", "C", "D"), quellon.trenes)
        assertEquals(listOf(1, 2), quellon.lineasDe("D"))
        assertEquals((1..12).toList(), quellon.lineasDe("A"))
        assertEquals(CatalogoCultivo.porDefecto, repositorio.observar("dalcahue").first())
    }
}
