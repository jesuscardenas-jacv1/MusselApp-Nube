package cl.aldemar.musselapp.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.util.HashPin

/** Cambios de la base entre versiones, sin perder las muestras guardadas en el teléfono. */
object Migraciones {

    /**
     * v1 → v2: se elimina el estado Observado. El supervisor ahora valida o corrige,
     * así que las muestras y revisiones que estaban en Observado pasan a Corregido.
     * Las tablas no cambian: solo los datos.
     */
    val DE_1_A_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE muestras SET estado = 'CORREGIDO' WHERE estado = 'OBSERVADO'")
            db.execSQL("UPDATE revisiones SET estado_asignado = 'CORREGIDO' WHERE estado_asignado = 'OBSERVADO'")
        }
    }

    /**
     * v2 → v3: el PIN deja de guardarse en texto plano. Cada PIN existente se reemplaza
     * por su hash (PBKDF2). Las tablas no cambian: solo los datos de la columna `pin`.
     */
    val DE_2_A_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val pendientes = mutableListOf<Pair<String, String>>()
            db.query("SELECT rut, pin FROM usuarios").use { c ->
                while (c.moveToNext()) {
                    val rut = c.getString(0)
                    val pin = c.getString(1)
                    // Un PIN vacío no permite ingresar de todos modos: se deja tal cual
                    if (pin.isNotEmpty() && !HashPin.esHash(pin)) pendientes += rut to pin
                }
            }
            pendientes.forEach { (rut, pin) ->
                val valores = ContentValues().apply { put("pin", HashPin.generar(pin)) }
                db.update("usuarios", SQLiteDatabase.CONFLICT_NONE, valores, "rut = ?", arrayOf(rut))
            }
        }
    }

    /**
     * v3 → v4: se agrega el conteo de cholgas por metro. Las muestras que ya estaban
     * guardadas quedan con null (no se registró), no con 0 (se contó y no había).
     */
    val DE_3_A_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE muestras ADD COLUMN cholgas_ind_m INTEGER")
        }
    }

    /**
     * v4 → v5: cada muestra guarda la posición del teléfono y el clima del momento, y cada centro
     * su punto de referencia y el radio de su área. Las muestras anteriores quedan con null
     * (no se registró); los cuatro centros del caso reciben sus coordenadas.
     */
    val DE_4_A_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "lat REAL", "lon REAL", "precision_m REAL",
                "clima_temp_aire_c REAL", "clima_viento_kn REAL", "clima_racha_kn REAL",
                "clima_viento_dir INTEGER", "clima_codigo INTEGER", "clima_ola_m REAL",
                "clima_temp_agua_c REAL", "clima_marea_m REAL", "clima_obtenido_en INTEGER",
            ).forEach { db.execSQL("ALTER TABLE muestras ADD COLUMN $it") }

            listOf("lat REAL", "lon REAL", "radio_m INTEGER")
                .forEach { db.execSQL("ALTER TABLE centros ADD COLUMN $it") }
            CentroCultivo.disponibles.forEach { c ->
                db.execSQL(
                    "UPDATE centros SET lat = ?, lon = ?, radio_m = ? WHERE id = ?",
                    arrayOf<Any>(c.lat, c.lon, c.radioM, c.id),
                )
            }
        }
    }

    /**
     * v5 → v6: largo del tramo de cuerda muestreado, en cm. Las muestras que ya estaban guardadas
     * quedan con null (no se registró).
     */
    val DE_5_A_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE muestras ADD COLUMN tramo_cm INTEGER")
        }
    }

    val TODAS = arrayOf(DE_1_A_2, DE_2_A_3, DE_3_A_4, DE_4_A_5, DE_5_A_6)
}
