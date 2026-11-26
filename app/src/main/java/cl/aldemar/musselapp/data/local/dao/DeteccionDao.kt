package cl.aldemar.musselapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cl.aldemar.musselapp.data.local.entidad.DeteccionEntity

/** Detecciones del conteo automático de cada muestra. */
@Dao
interface DeteccionDao {

    @Insert
    suspend fun insertar(detecciones: List<DeteccionEntity>)

    /** Todas, incluidas las eliminadas por el operario, en el orden en que se guardaron. */
    @Query("SELECT * FROM detecciones WHERE muestra_id = :muestraId ORDER BY id")
    suspend fun deMuestra(muestraId: Long): List<DeteccionEntity>

    @Query("DELETE FROM detecciones WHERE muestra_id = :muestraId")
    suspend fun borrarDeMuestra(muestraId: Long): Int

    /** Cambia las detecciones de la muestra por las nuevas (al volver a contar o al guardar la revisión). */
    @Transaction
    suspend fun reemplazar(muestraId: Long, detecciones: List<DeteccionEntity>) {
        borrarDeMuestra(muestraId)
        insertar(detecciones.map { it.copy(id = 0, muestraId = muestraId) })
    }
}
