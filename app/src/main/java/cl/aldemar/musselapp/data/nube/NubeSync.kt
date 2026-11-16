package cl.aldemar.musselapp.data.nube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.util.Log
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.util.AlertasMuestra
import cl.aldemar.musselapp.util.CodigoTiempo
import cl.aldemar.musselapp.util.Distancia
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/** Revisión que el supervisor dejó en la nube para una muestra de este teléfono. */
data class VeredictoNube(
    val muestraId: Long,
    val estado: EstadoMuestra,
    val comentario: String?,
    val revisadaEn: Long,
)

/**
 * Conexión con Firestore (demo en tiempo real). Room sigue siendo la fuente de verdad:
 * esta clase solo copia las muestras a la nube y trae de vuelta los dictámenes.
 * Todo fallo es no fatal: la muestra queda pendiente de sincronizar.
 */
class NubeSync(context: Context) : ServicioNube {

    private val prefs = context.applicationContext.getSharedPreferences("nube", Context.MODE_PRIVATE)

    /** Identifica esta instalación para que los ids de Room no choquen entre teléfonos. */
    private val instalacion: String = prefs.getString("instalacion", null)
        ?: UUID.randomUUID().toString().take(8).also { prefs.edit().putString("instalacion", it).apply() }

    private val firestore: FirebaseFirestore? =
        if (ACTIVA) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Firebase NO inicializado (revisar google-services.json y plugin)", e)
                null
            }
        } else null

    init {
        Log.i(TAG, "Nube lista=${firestore != null}, instalacion=$instalacion")
    }

    private fun docId(muestraId: Long) = "${instalacion}_$muestraId"

    /** Sube (o actualiza) la muestra. Devuelve true si el servidor la confirmó. */
    override suspend fun subir(m: Muestra): Boolean {
        val db = firestore ?: run { Log.e(TAG, "subir: sin Firestore"); return false }
        return try {
            Log.i(TAG, "Subiendo ${docId(m.id)}")
            val datos = hashMapOf<String, Any?>(
                "codigo" to m.codigo,
                "centroId" to m.centroId,
                "tren" to m.tren,
                "linea" to m.linea,
                "profundidadM" to m.profundidadM,
                "tallaMm" to m.tallaMm,
                "conteoIndM" to m.conteoIndM,
                "cholgasIndM" to m.cholgasIndM,
                "observacion" to m.observacion,
                "estado" to m.estado.name,
                "registradaEn" to m.registradaEn,
                "operador" to m.operador,
                "operadorRut" to m.operadorRut,
                // Posición, clima y alertas: la app es la única que las calcula; el portal solo las muestra
                "lat" to m.posicion?.lat,
                "lon" to m.posicion?.lon,
                "precisionM" to m.posicion?.precisionM,
                "distanciaCentroM" to Distancia.alCentro(m),
                "clima" to m.clima?.let { c ->
                    mapOf(
                        "tempAireC" to c.tempAireC, "vientoKn" to c.vientoKn, "rachaKn" to c.rachaKn,
                        "vientoDir" to c.vientoDir, "codigo" to c.codigo, "descripcion" to CodigoTiempo.descripcion(c.codigo),
                        "olaM" to c.olaM, "tempAguaC" to c.tempAguaC, "mareaM" to c.mareaM, "obtenidoEn" to c.obtenidoEn,
                    )
                },
                "alertas" to AlertasMuestra.evaluar(m).map { it.name },
            )
            // La foto va en su propio documento (fotos/{id}): así quien escucha la colección de muestras
            // (el portal y los teléfonos) no descarga todas las fotos; el portal la pide al abrir la muestra
            val foto = fotoBase64(m.fotoRuta)
            if (foto != null) {
                datos["tieneFoto"] = true
                datos["fotoBase64"] = FieldValue.delete() // las versiones anteriores la guardaban dentro de la muestra
            }
            if (m.estado != EstadoMuestra.PENDIENTE) {
                datos["revisadaPor"] = m.revisadaPor
                datos["revisadaEn"] = m.revisadaEn
                datos["comentarioSupervisor"] = m.comentarioSupervisor
            }
            withTimeout(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val refMuestra = db.collection(COLECCION).document(docId(m.id))
                    // Transacción en vez de lote: se lee primero lo que hay en la nube para que un reintento
                    // (por ejemplo, tras un corte de internet) no pise un dictamen que el supervisor ya dejó.
                    // Sin conexión la transacción falla de inmediato y la muestra queda pendiente, igual que antes.
                    db.runTransaction { tx ->
                        val remoto = tx.get(refMuestra)
                        val estadoRemoto = if (remoto.exists()) remoto.getString("estado") else null
                        tx.set(refMuestra, ProteccionDictamen.filtrar(datos, m.estado, estadoRemoto), SetOptions.merge())
                        if (foto != null) tx.set(db.collection(COLECCION_FOTOS).document(docId(m.id)), mapOf("fotoBase64" to foto))
                        null
                    }
                        .addOnSuccessListener { Log.i(TAG, "Subida OK ${docId(m.id)}"); if (cont.isActive) cont.resume(true) }
                        .addOnFailureListener { Log.e(TAG, "Subida FALLÓ", it); if (cont.isActive) cont.resume(false) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo subir la muestra ${m.id}", e)
            false
        }
    }

    /** Escucha en vivo los dictámenes del supervisor para las muestras de este teléfono. */
    override fun escucharVeredictos(alAplicar: (VeredictoNube) -> Unit) {
        val db = firestore ?: return
        try {
            db.collection(COLECCION).addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    error?.let { Log.w(TAG, "Escucha de la nube falló", it) }
                    return@addSnapshotListener
                }
                for (cambio in snap.documentChanges) {
                    if (cambio.type == DocumentChange.Type.REMOVED) continue
                    val doc = cambio.document
                    if (!doc.id.startsWith("${instalacion}_")) continue
                    val id = doc.id.substringAfterLast('_').toLongOrNull() ?: continue
                    val estado = runCatching { EstadoMuestra.valueOf(doc.getString("estado") ?: "") }
                        .getOrNull() ?: continue
                    if (estado == EstadoMuestra.PENDIENTE) continue
                    alAplicar(
                        VeredictoNube(
                            muestraId = id,
                            estado = estado,
                            comentario = doc.getString("comentarioSupervisor"),
                            revisadaEn = doc.getLong("revisadaEn") ?: System.currentTimeMillis(),
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar la escucha", e)
        }
    }

    /** Foto reducida (máx. 800 px, JPEG) en base64, para caber en un documento de Firestore. */
    private fun fotoBase64(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        return try {
            val archivo = File(ruta)
            if (!archivo.exists()) return null
            val bordes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(ruta, bordes)
            var muestreo = 1
            while (maxOf(bordes.outWidth, bordes.outHeight) / (muestreo * 2) >= LADO_MAX) muestreo *= 2
            var bmp = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestreo })
                ?: return null
            val grados = when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (grados != 0f) {
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(grados) }, true)
            }
            var calidad = 70
            var bytes: ByteArray
            do {
                val salida = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                bytes = salida.toByteArray()
                calidad -= 15
            } while (bytes.size > MAX_BYTES && calidad > 10)
            if (bytes.size > MAX_BYTES) null
            else "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo preparar la foto", e)
            null
        }
    }

    companion object {
        /** Interruptor de seguridad: en false la app funciona solo local, como antes. */
        const val ACTIVA = true
        private const val COLECCION = "muestras"
        /** Fotos de las muestras, una por documento con el mismo id que la muestra. */
        private const val COLECCION_FOTOS = "fotos"
        private const val TAG = "NubeSync"
        private const val TIMEOUT_MS = 10_000L
        private const val LADO_MAX = 800
        private const val MAX_BYTES = 500_000
    }
}
