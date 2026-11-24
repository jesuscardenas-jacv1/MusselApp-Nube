package cl.aldemar.musselapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cl.aldemar.musselapp.data.local.entidad.MuestraCompleta
import cl.aldemar.musselapp.data.local.entidad.MuestraEntity
import cl.aldemar.musselapp.data.local.entidad.RevisionEntity
import cl.aldemar.musselapp.data.model.EstadoMuestra
import kotlinx.coroutines.flow.Flow

/**
 * Une la muestra con línea, tren, operador y su revisión vigente: la última que se registró en este
 * teléfono, que es la que fijó el estado. No se ordena por fecha porque un dictamen adoptado de la nube
 * puede traer una fecha anterior a la del dictamen local que reemplaza.
 */
private const val SELECT_COMPLETA = """
    SELECT m.*,
           t.centro_id AS centro_id,
           t.codigo AS tren_codigo,
           l.numero AS linea_numero,
           op.nombre AS operador_nombre,
           sup.nombre AS supervisor_nombre,
           r.comentario AS comentario_supervisor,
           r.revisada_en AS revisada_en
    FROM muestras m
    JOIN lineas l ON l.id = m.linea_id
    JOIN trenes t ON t.id = l.tren_id
    JOIN usuarios op ON op.rut = m.operador_rut
    LEFT JOIN revisiones r ON r.id = (
        SELECT r2.id FROM revisiones r2
        WHERE r2.muestra_id = m.id
        ORDER BY r2.id DESC
        LIMIT 1
    )
    LEFT JOIN usuarios sup ON sup.rut = r.supervisor_rut
"""

/** Muestras y sus revisiones. */
@Dao
interface MuestraDao {

    @Query("$SELECT_COMPLETA ORDER BY m.registrada_en DESC")
    fun observarTodas(): Flow<List<MuestraCompleta>>

    @Query("$SELECT_COMPLETA WHERE m.id = :id")
    fun observarPorId(id: Long): Flow<MuestraCompleta?>

    @Insert
    suspend fun insertar(muestra: MuestraEntity): Long

    @Insert
    suspend fun insertarRevision(revision: RevisionEntity): Long

    /**
     * Aplica los valores que corrigió el supervisor en el portal. La primera vez guarda lo que había
     * registrado el operario (SQLite evalúa la asignación con los valores de antes); si la corrección
     * llega repetida, lo del operario no se pisa.
     */
    @Query(
        """
        UPDATE muestras SET
            conteo_operario = COALESCE(conteo_operario, conteo_ind_m),
            talla_operario = COALESCE(talla_operario, talla_mm),
            cholgas_operario = CASE WHEN conteo_operario IS NULL THEN cholgas_ind_m ELSE cholgas_operario END,
            conteo_ind_m = :conteoIndM, talla_mm = :tallaMm, cholgas_ind_m = :cholgasIndM
        WHERE id = :id
        """,
    )
    suspend fun aplicarCorreccion(id: Long, conteoIndM: Int, tallaMm: Int, cholgasIndM: Int?)

    @Query("UPDATE muestras SET estado = :estado, sincronizada = 0 WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: EstadoMuestra)

    /** Cambia el estado y deja el dictamen en el historial de revisiones, en una sola transacción. */
    @Transaction
    suspend fun registrarRevision(
        muestraId: Long,
        estado: EstadoMuestra,
        comentario: String?,
        supervisorRut: String,
        revisadaEn: Long,
    ) {
        actualizarEstado(muestraId, estado)
        insertarRevision(
            RevisionEntity(
                muestraId = muestraId,
                supervisorRut = supervisorRut,
                estadoAsignado = estado,
                comentario = comentario,
                revisadaEn = revisadaEn,
            ),
        )
    }

    @Query("SELECT * FROM revisiones WHERE muestra_id = :muestraId ORDER BY revisada_en DESC, id DESC")
    fun observarRevisiones(muestraId: Long): Flow<List<RevisionEntity>>

    /** Simula el envío al servidor: marca todo como sincronizado y devuelve cuántas se enviaron. */
    @Query("UPDATE muestras SET sincronizada = 1 WHERE sincronizada = 0")
    suspend fun marcarSincronizadas(): Int

    @Query("SELECT id FROM muestras WHERE sincronizada = 0 ORDER BY registrada_en")
    suspend fun pendientesDeSincronizar(): List<Long>

    @Query("UPDATE muestras SET sincronizada = 1 WHERE id = :id")
    suspend fun marcarSincronizada(id: Long)

    /** Valores cambiados en el backend (por ejemplo, un PUT en Swagger). Queda pendiente para que también llegue a Firestore. */
    @Query(
        """
        UPDATE muestras SET
            profundidad_m = :profundidadM, talla_mm = :tallaMm, conteo_ind_m = :conteoIndM,
            cholgas_ind_m = :cholgasIndM, tramo_cm = :tramoCm, observacion = :observacion,
            sincronizada = 0
        WHERE id = :id
        """,
    )
    suspend fun actualizarDesdeRemoto(
        id: Long,
        profundidadM: Int,
        tallaMm: Int,
        conteoIndM: Int,
        cholgasIndM: Int?,
        tramoCm: Int?,
        observacion: String,
    )

    /** Borra la muestra; sus revisiones se borran en cascada. Devuelve cuántas filas se borraron. */
    @Query("DELETE FROM muestras WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    /** Guarda el clima consultado después de insertar la muestra (guardar nunca espera a la red). */
    @Query(
        """
        UPDATE muestras SET
            clima_temp_aire_c = :tempAireC, clima_viento_kn = :vientoKn, clima_racha_kn = :rachaKn,
            clima_viento_dir = :vientoDir, clima_codigo = :codigo, clima_ola_m = :olaM,
            clima_temp_agua_c = :tempAguaC, clima_marea_m = :mareaM, clima_obtenido_en = :obtenidoEn
        WHERE id = :id
        """,
    )
    suspend fun guardarClima(
        id: Long,
        tempAireC: Double?,
        vientoKn: Double?,
        rachaKn: Double?,
        vientoDir: Int?,
        codigo: Int?,
        olaM: Double?,
        tempAguaC: Double?,
        mareaM: Double?,
        obtenidoEn: Long,
    )

    @Query("SELECT COUNT(*) FROM muestras")
    suspend fun contar(): Int
}
