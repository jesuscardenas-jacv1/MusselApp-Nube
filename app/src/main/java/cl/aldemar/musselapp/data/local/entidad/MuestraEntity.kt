package cl.aldemar.musselapp.data.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.aldemar.musselapp.data.model.EstadoMuestra

/**
 * Muestra registrada en terreno (tabla `muestras`).
 * La foto no se guarda en la base: se guarda como archivo privado de la app y aquí solo su ruta.
 */
@Entity(
    tableName = "muestras",
    foreignKeys = [
        ForeignKey(
            entity = LineaEntity::class,
            parentColumns = ["id"],
            childColumns = ["linea_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["rut"],
            childColumns = ["operador_rut"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("linea_id"), Index("operador_rut"), Index("estado"), Index("registrada_en")],
)
data class MuestraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "linea_id") val lineaId: Long,
    @ColumnInfo(name = "operador_rut") val operadorRut: String,
    @ColumnInfo(name = "profundidad_m") val profundidadM: Int,
    @ColumnInfo(name = "talla_mm") val tallaMm: Int,
    @ColumnInfo(name = "conteo_ind_m") val conteoIndM: Int,
    val observacion: String,
    val estado: EstadoMuestra,
    @ColumnInfo(name = "registrada_en") val registradaEn: Long,
    /** false mientras la muestra no se envía al servidor (sincronización futura). */
    val sincronizada: Boolean,
    @ColumnInfo(name = "foto_ruta") val fotoRuta: String?,
    /** Cholgas por metro (desde la versión 4 de la base); null en las muestras antiguas. */
    @ColumnInfo(name = "cholgas_ind_m") val cholgasIndM: Int? = null,
    /** Largo del tramo de cuerda muestreado, en cm (desde la versión 6); null en las muestras antiguas. */
    @ColumnInfo(name = "tramo_cm") val tramoCm: Int? = null,
    // Desde la versión 5: posición del teléfono al guardar y condiciones del momento.
    // Todo es opcional: sin GPS o sin internet la muestra se guarda igual.
    val lat: Double? = null,
    val lon: Double? = null,
    @ColumnInfo(name = "precision_m") val precisionM: Double? = null,
    @ColumnInfo(name = "clima_temp_aire_c") val climaTempAireC: Double? = null,
    @ColumnInfo(name = "clima_viento_kn") val climaVientoKn: Double? = null,
    @ColumnInfo(name = "clima_racha_kn") val climaRachaKn: Double? = null,
    @ColumnInfo(name = "clima_viento_dir") val climaVientoDir: Int? = null,
    @ColumnInfo(name = "clima_codigo") val climaCodigo: Int? = null,
    @ColumnInfo(name = "clima_ola_m") val climaOlaM: Double? = null,
    @ColumnInfo(name = "clima_temp_agua_c") val climaTempAguaC: Double? = null,
    @ColumnInfo(name = "clima_marea_m") val climaMareaM: Double? = null,
    /** Momento en que se consultó el clima; null si no se pudo obtener. */
    @ColumnInfo(name = "clima_obtenido_en") val climaObtenidoEn: Long? = null,
    // Desde la versión 7: lo que registró el operario si el supervisor corrigió los valores en el portal.
    // null mientras no haya corrección.
    @ColumnInfo(name = "conteo_operario") val conteoOperario: Int? = null,
    @ColumnInfo(name = "talla_operario") val tallaOperario: Int? = null,
    @ColumnInfo(name = "cholgas_operario") val cholgasOperario: Int? = null,
    // Desde la versión 8: resumen del conteo automático (detalle en la tabla `detecciones`).
    // null si la muestra se contó a mano.
    @ColumnInfo(name = "ml_modelo_version") val mlModeloVersion: String? = null,
    @ColumnInfo(name = "ml_total") val mlTotal: Int? = null,
    @ColumnInfo(name = "ml_choritos") val mlChoritos: Int? = null,
    @ColumnInfo(name = "ml_cholgas") val mlCholgas: Int? = null,
    /** Milímetros por píxel según la referencia de 40 mm; null si no se detectó. */
    @ColumnInfo(name = "ml_mm_por_px") val mlMmPorPx: Double? = null,
    /** true si el operario corrigió lo que detectó el modelo. */
    @ColumnInfo(name = "ml_editado", defaultValue = "0") val mlEditado: Boolean = false,
)
