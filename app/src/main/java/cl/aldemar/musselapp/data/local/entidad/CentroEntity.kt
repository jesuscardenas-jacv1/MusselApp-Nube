package cl.aldemar.musselapp.data.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Centro de cultivo de choritos (tabla `centros`). */
@Entity(tableName = "centros")
data class CentroEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    // Desde la versión 5: punto de referencia del centro y radio de su área de cultivo
    val lat: Double? = null,
    val lon: Double? = null,
    @ColumnInfo(name = "radio_m") val radioM: Int? = null,
)
