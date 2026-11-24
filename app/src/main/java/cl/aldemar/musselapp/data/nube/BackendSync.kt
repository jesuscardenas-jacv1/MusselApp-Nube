package cl.aldemar.musselapp.data.nube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.util.CodigoTiempo
import cl.aldemar.musselapp.util.Distancia
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Almacén mínimo para recordar el id de instalación y el mapa entre ids del backend y Room. */
interface AlmacenSyncBackend {
    fun instalacion(): String
    fun idLocalDe(idRemoto: String): Long?
    fun guardarIdLocal(idRemoto: String, idLocal: Long)
}

/** Implementación con SharedPreferences privadas para la app Android. */
class PreferenciasBackend(context: Context) : AlmacenSyncBackend {
    private val prefs = context.applicationContext.getSharedPreferences("backend_sync", Context.MODE_PRIVATE)

    override fun instalacion(): String =
        prefs.getString("instalacion", null)
            ?: UUID.randomUUID().toString().take(8).also { prefs.edit().putString("instalacion", it).apply() }

    override fun idLocalDe(idRemoto: String): Long? =
        if (prefs.contains("remoto_$idRemoto")) prefs.getLong("remoto_$idRemoto", -1L).takeIf { it > 0 } else null

    override fun guardarIdLocal(idRemoto: String, idLocal: Long) {
        prefs.edit().putLong("remoto_$idRemoto", idLocal).apply()
    }
}

/**
 * Sincronización contra el backend propio en Spring Boot (`backend/`) usando Retrofit.
 * Room sigue siendo la fuente de verdad: si el backend está apagado o sin red, [subir] devuelve false
 * y WorkManager reintenta automáticamente usando `idCliente` para evitar duplicados.
 */
class BackendSync(
    private val api: BackendApi = crearApi(),
    private val almacen: AlmacenSyncBackend,
    private val codificarFoto: (String?) -> String? = ::fotoBase64DesdeArchivo,
    private val log: (String, Throwable?) -> Unit = { msg, err -> runCatching { Log.w(TAG, msg, err) } },
) : ServicioNube {

    constructor(context: Context, urlBase: String = BackendApi.URL_BASE) : this(
        api = crearApi(urlBase),
        almacen = PreferenciasBackend(context),
    )

    private val instalacion: String get() = almacen.instalacion()

    private fun idClienteDe(muestraId: Long): String = "${instalacion}_$muestraId"

    override suspend fun subir(m: Muestra): Boolean {
        return try {
            val dto = MuestraEnviarDto(
                idCliente = idClienteDe(m.id),
                codigo = m.codigo,
                centroId = m.centroId,
                tren = m.tren,
                linea = m.linea,
                profundidadM = m.profundidadM,
                tallaMm = m.tallaMm,
                conteoIndM = m.conteoIndM,
                cholgasIndM = m.cholgasIndM,
                tramoCm = m.tramoCm,
                observacion = m.observacion,
                registradaEn = m.registradaEn,
                operador = m.operador.ifBlank { "Carla Vera" },
                operadorRut = m.operadorRut.ifBlank { "111111111" },
                lat = m.posicion?.lat,
                lon = m.posicion?.lon,
                precisionM = m.posicion?.precisionM,
                distanciaCentroM = Distancia.alCentro(m),
                climaTempAireC = m.clima?.tempAireC,
                climaVientoKn = m.clima?.vientoKn,
                climaOlaM = m.clima?.olaM,
                climaTempAguaC = m.clima?.tempAguaC,
                climaDescripcion = m.clima?.codigo?.let { CodigoTiempo.descripcion(it) },
            )
            val remota = api.crearOActualizarMuestra(rol = "OPERARIO", muestra = dto)
            almacen.guardarIdLocal(remota.id.toString(), m.id)

            // Fotografía de evidencia en endpoint separado (/api/muestras/{id}/foto)
            val base64 = codificarFoto(m.fotoRuta)
            if (base64 != null) {
                val nombre = m.fotoRuta?.substringAfterLast('/')?.ifBlank { "muestra_${m.id}.jpg" } ?: "muestra_${m.id}.jpg"
                api.subirFoto(
                    id = remota.id,
                    rol = "OPERARIO",
                    foto = FotoEnviarDto(
                        nombreArchivo = nombre,
                        tipoMime = "image/jpeg",
                        contenidoBase64 = base64,
                    ),
                )
            }

            // Si el supervisor dictaminó en el teléfono y el backend sigue pendiente, se sincroniza el dictamen
            if (m.estado != EstadoMuestra.PENDIENTE && remota.estado == EstadoMuestra.PENDIENTE.name) {
                api.dictaminar(
                    id = remota.id,
                    rol = "SUPERVISOR",
                    revision = RevisionEnviarDto(
                        estado = m.estado.name,
                        comentario = m.comentarioSupervisor,
                        supervisorNombre = m.revisadaPor,
                        supervisorRut = "222222222",
                    ),
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("No se pudo sincronizar muestra ${m.id} con Spring Boot", e)
            false
        }
    }

    override fun escucharVeredictos(alEliminar: (Long) -> Unit, alAplicar: (VeredictoNube) -> Unit) {
        // El backend REST funciona bajo demanda vía [descargarCambios] (Retrofit GET /api/muestras)
    }

    override suspend fun descargarCambios(): List<CambioRemoto> {
        return try {
            val prefijoPropio = "${instalacion}_"
            api.listarMuestras().mapNotNull { r ->
                // Las 3 semillas del backend no se importan mientras nadie las edite en Swagger
                if (r.idCliente?.startsWith("seed_") == true && r.actualizadaEn <= r.registradaEn) {
                    return@mapNotNull null
                }
                val estado = runCatching { EstadoMuestra.valueOf(r.estado) }.getOrDefault(EstadoMuestra.PENDIENTE)
                val idLocalPropio = r.idCliente
                    ?.takeIf { it.startsWith(prefijoPropio) }
                    ?.substringAfterLast('_')
                    ?.toLongOrNull()
                val idLocal = idLocalPropio ?: almacen.idLocalDe(r.id.toString())
                CambioRemoto(
                    idRemoto = r.id.toString(),
                    idLocalExistente = idLocal,
                    centroId = r.centroId,
                    tren = r.tren,
                    linea = r.linea,
                    profundidadM = r.profundidadM,
                    tallaMm = r.tallaMm,
                    conteoIndM = r.conteoIndM,
                    cholgasIndM = r.cholgasIndM,
                    tramoCm = r.tramoCm,
                    observacion = r.observacion.orEmpty(),
                    estado = estado,
                    registradaEn = r.registradaEn,
                    operador = r.operador.orEmpty(),
                    operadorRut = r.operadorRut.orEmpty(),
                    revisadaPor = r.revisadaPor,
                    revisadaEn = r.revisadaEn,
                    comentarioSupervisor = r.comentarioSupervisor,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("No se pudieron consultar muestras del backend Spring Boot", e)
            emptyList()
        }
    }

    override fun registrarIdLocal(idRemoto: String, idLocal: Long) {
        almacen.guardarIdLocal(idRemoto, idLocal)
    }

    companion object {
        /** Interruptor del backend Spring Boot: en true sincroniza con Retrofit junto a Firebase. */
        const val ACTIVO = true
        private const val TAG = "BackendSync"
        private const val LADO_MAX = 800
        private const val MAX_BYTES = 500_000

        fun crearApi(urlBase: String = BackendApi.URL_BASE): BackendApi = Retrofit.Builder()
            .baseUrl(urlBase)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)

        /** Reduce y codifica la foto local a Base64 JPEG (< 500 KB) para enviarla al endpoint de fotos. */
        private fun fotoBase64DesdeArchivo(ruta: String?): String? {
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
                runCatching { Log.w(TAG, "No se pudo codificar la foto para el backend", e) }
                null
            }
        }
    }
}
