package cl.aldemar.musselapp.data.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.aldemar.musselapp.data.ml.Especie

/** Quién puso la detección: el modelo o el operario al revisar. */
enum class OrigenDeteccion { MODELO, OPERARIO }

/**
 * Un individuo (o la referencia) del conteo automático de una muestra (tabla `detecciones`, desde la versión 8).
 * Las que quita el operario no se borran: quedan con [eliminada] = true y sirven para reentrenar el modelo.
 */
@Entity(
    tableName = "detecciones",
    foreignKeys = [
        ForeignKey(
            entity = MuestraEntity::class,
            parentColumns = ["id"],
            childColumns = ["muestra_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("muestra_id")],
)
data class DeteccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "muestra_id") val muestraId: Long,
    /** Especie que quedó después de la revisión del operario. */
    val especie: Especie,
    /** Lo que dijo el modelo; null si la agregó el operario. */
    @ColumnInfo(name = "especie_modelo") val especieModelo: Especie?,
    /** Confianza del modelo (0..1); null si la agregó el operario. */
    val confianza: Float?,
    /** Recuadro normalizado 0..1 respecto del ancho y alto de la foto. */
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
    /** Largo de la concha según el calibre; null sin referencia o si no se pudo medir. */
    @ColumnInfo(name = "largo_mm") val largoMm: Double?,
    val origen: OrigenDeteccion,
    @ColumnInfo(defaultValue = "0") val eliminada: Boolean = false,
)
