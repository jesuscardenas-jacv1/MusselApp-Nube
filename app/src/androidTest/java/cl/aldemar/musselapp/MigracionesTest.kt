package cl.aldemar.musselapp

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.aldemar.musselapp.data.local.Migraciones
import cl.aldemar.musselapp.data.local.MusselDatabase
import cl.aldemar.musselapp.util.HashPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La migración v1 → v2 convierte Observado en Corregido sin perder la muestra.
 * Usa los esquemas exportados en app/schemas (1.json y 2.json).
 */
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val nombre = "migracion-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MusselDatabase::class.java,
    )

    @Test
    fun de1a2_observadoPasaACorregido() {
        // Base en versión 1 con una muestra observada y su revisión
        helper.createDatabase(nombre, 1).apply {
            execSQL("INSERT INTO centros (id, nombre) VALUES ('dalcahue', 'Centro Dalcahue')")
            execSQL("INSERT INTO trenes (id, centro_id, codigo) VALUES (1, 'dalcahue', 'A')")
            execSQL("INSERT INTO lineas (id, tren_id, numero) VALUES (1, 1, 4)")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('111111111', 'Carla Vera', 'OPERARIO', '1234')")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('222222222', 'Diego Muñoz', 'SUPERVISOR', '1234')")
            execSQL(
                "INSERT INTO muestras (id, linea_id, operador_rut, profundidad_m, talla_mm, conteo_ind_m, " +
                    "observacion, estado, registrada_en, sincronizada, foto_ruta) " +
                    "VALUES (1, 1, '111111111', 5, 60, 300, 'Densidad baja', 'OBSERVADO', 0, 0, NULL)",
            )
            execSQL(
                "INSERT INTO muestras (id, linea_id, operador_rut, profundidad_m, talla_mm, conteo_ind_m, " +
                    "observacion, estado, registrada_en, sincronizada, foto_ruta) " +
                    "VALUES (2, 1, '111111111', 5, 70, 420, '', 'VALIDADO', 0, 0, NULL)",
            )
            execSQL(
                "INSERT INTO revisiones (id, muestra_id, supervisor_rut, estado_asignado, comentario, revisada_en) " +
                    "VALUES (1, 1, '222222222', 'OBSERVADO', 'Repetir conteo', 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(nombre, 2, true, Migraciones.DE_1_A_2)

        db.query("SELECT estado FROM muestras ORDER BY id").use { c ->
            c.moveToNext(); assertEquals("CORREGIDO", c.getString(0))
            c.moveToNext(); assertEquals("VALIDADO", c.getString(0)) // las demás no cambian
        }
        db.query("SELECT estado_asignado, comentario FROM revisiones").use { c ->
            c.moveToNext()
            assertEquals("CORREGIDO", c.getString(0))
            assertEquals("Repetir conteo", c.getString(1))
        }
    }

    @Test
    fun de2a3_elPinPasaAHash() {
        // Base en versión 2 con los PIN en texto plano, como quedaron en los teléfonos
        helper.createDatabase(nombre, 2).apply {
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('111111111', 'Carla Vera', 'OPERARIO', '1234')")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('222222222', 'Diego Muñoz', 'SUPERVISOR', '5678')")
            close()
        }

        val db = helper.runMigrationsAndValidate(nombre, 3, true, Migraciones.DE_2_A_3)

        db.query("SELECT rut, pin FROM usuarios ORDER BY rut").use { c ->
            c.moveToNext()
            assertTrue(HashPin.esHash(c.getString(1)))
            assertTrue(HashPin.verificar("1234", c.getString(1)))
            c.moveToNext()
            assertTrue(HashPin.verificar("5678", c.getString(1)))
            assertTrue(!HashPin.verificar("1234", c.getString(1)))
        }
    }

    @Test
    fun de3a4_seAgreganLasCholgasSinTocarLasMuestras() {
        // Base en versión 3 con una muestra guardada antes de que existieran las cholgas
        helper.createDatabase(nombre, 3).apply {
            execSQL("INSERT INTO centros (id, nombre) VALUES ('dalcahue', 'Centro Dalcahue')")
            execSQL("INSERT INTO trenes (id, centro_id, codigo) VALUES (1, 'dalcahue', 'A')")
            execSQL("INSERT INTO lineas (id, tren_id, numero) VALUES (1, 1, 4)")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('111111111', 'Carla Vera', 'OPERARIO', 'x')")
            execSQL(
                "INSERT INTO muestras (id, linea_id, operador_rut, profundidad_m, talla_mm, conteo_ind_m, " +
                    "observacion, estado, registrada_en, sincronizada, foto_ruta) " +
                    "VALUES (1, 1, '111111111', 5, 35, 282, '', 'PENDIENTE', 0, 0, NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(nombre, 4, true, Migraciones.DE_3_A_4)

        // La muestra antigua se conserva y queda sin cholgas registradas (null, no 0)
        db.query("SELECT conteo_ind_m, cholgas_ind_m FROM muestras").use { c ->
            assertEquals(1, c.count)
            c.moveToNext()
            assertEquals(282, c.getInt(0))
            assertTrue(c.isNull(1))
        }
    }

    @Test
    fun de4a5_seAgreganPosicionYClimaYLosCentrosRecibenCoordenadas() {
        // Base en versión 4 con una muestra guardada antes de que existieran la posición y el clima
        helper.createDatabase(nombre, 4).apply {
            execSQL("INSERT INTO centros (id, nombre) VALUES ('dalcahue', 'Centro Dalcahue')")
            execSQL("INSERT INTO centros (id, nombre) VALUES ('melinka', 'Centro Melinka')")
            execSQL("INSERT INTO trenes (id, centro_id, codigo) VALUES (1, 'dalcahue', 'A')")
            execSQL("INSERT INTO lineas (id, tren_id, numero) VALUES (1, 1, 4)")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('111111111', 'Carla Vera', 'OPERARIO', 'x')")
            execSQL(
                "INSERT INTO muestras (id, linea_id, operador_rut, profundidad_m, talla_mm, conteo_ind_m, " +
                    "observacion, estado, registrada_en, sincronizada, foto_ruta, cholgas_ind_m) " +
                    "VALUES (1, 1, '111111111', 5, 35, 282, '', 'PENDIENTE', 0, 0, NULL, 12)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(nombre, 5, true, Migraciones.DE_4_A_5)

        // La muestra antigua se conserva, sin posición ni clima (null: no se registró)
        db.query("SELECT conteo_ind_m, cholgas_ind_m, lat, lon, clima_obtenido_en FROM muestras").use { c ->
            assertEquals(1, c.count)
            c.moveToNext()
            assertEquals(282, c.getInt(0))
            assertEquals(12, c.getInt(1))
            assertTrue(c.isNull(2))
            assertTrue(c.isNull(3))
            assertTrue(c.isNull(4))
        }
        // Los centros del caso reciben su punto de referencia y el radio del área
        db.query("SELECT lat, lon, radio_m FROM centros WHERE id = 'dalcahue'").use { c ->
            c.moveToNext()
            assertEquals(-42.374, c.getDouble(0), 0.0001)
            assertEquals(-73.622, c.getDouble(1), 0.0001)
            assertEquals(1500, c.getInt(2))
        }
        db.query("SELECT lat FROM centros WHERE id = 'melinka'").use { c ->
            c.moveToNext()
            assertEquals(-43.905, c.getDouble(0), 0.0001)
        }
    }

    @Test
    fun de5a6_seAgregaElTramoYLasMuestrasAntiguasSeConservan() {
        // Base en versión 5 con una muestra guardada antes de que existiera el tramo
        helper.createDatabase(nombre, 5).apply {
            execSQL("INSERT INTO centros (id, nombre, lat, lon, radio_m) VALUES ('dalcahue', 'Centro Dalcahue', -42.374, -73.622, 1500)")
            execSQL("INSERT INTO trenes (id, centro_id, codigo) VALUES (1, 'dalcahue', 'A')")
            execSQL("INSERT INTO lineas (id, tren_id, numero) VALUES (1, 1, 4)")
            execSQL("INSERT INTO usuarios (rut, nombre, rol, pin) VALUES ('111111111', 'Carla Vera', 'OPERARIO', 'x')")
            execSQL(
                "INSERT INTO muestras (id, linea_id, operador_rut, profundidad_m, talla_mm, conteo_ind_m, " +
                    "observacion, estado, registrada_en, sincronizada, foto_ruta, cholgas_ind_m, lat, lon) " +
                    "VALUES (1, 1, '111111111', 5, 35, 282, 'Antigua', 'VALIDADO', 0, 1, NULL, 12, -42.37, -73.62)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(nombre, 6, true, Migraciones.DE_5_A_6)

        // La muestra antigua conserva sus datos y queda sin tramo registrado (null, no 0)
        db.query("SELECT conteo_ind_m, cholgas_ind_m, estado, lat, tramo_cm FROM muestras").use { c ->
            assertEquals(1, c.count)
            c.moveToNext()
            assertEquals(282, c.getInt(0))
            assertEquals(12, c.getInt(1))
            assertEquals("VALIDADO", c.getString(2))
            assertEquals(-42.37, c.getDouble(3), 0.0001)
            assertTrue(c.isNull(4))
        }
    }
}
