package cl.aldemar.musselapp.data.nube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import cl.aldemar.musselapp.data.local.DatosIniciales
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.ValoresOperario
import cl.aldemar.musselapp.util.CodigoTiempo
import cl.aldemar.musselapp.util.Distancia
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Lo que el teléfono recuerda de cada servidor. Cada computador tiene su propia base, por eso la clave lleva el servidor. */
interface AlmacenBackend {
    fun instalacion(): String
    /** Muestra local que se importó desde el servidor. */
    fun idLocalDe(idCliente: String): Long?
    fun idClienteDe(idLocal: Long): String?
    fun asociar(idCliente: String, idLocal: Long)
    /** Última versión del servidor que ya se revisó (0 si ninguna). */
    fun versionVista(servidor: String, idCliente: String): Long
    fun guardarVersion(servidor: String, idCliente: String, version: Long)
    /** Huella del último contenido que se envió. */
    fun huellaEnviada(servidor: String, idCliente: String): String?
    fun guardarHuella(servidor: String, idCliente: String, huella: String)
}

class PreferenciasBackend(context: Context) : AlmacenBackend {
    private val prefs = context.applicationContext.getSharedPreferences("backend_sync", Context.MODE_PRIVATE)

    override fun instalacion(): String = prefs.getString("instalacion", null)
        ?: UUID.randomUUID().toString().take(8).also { prefs.edit { putString("instalacion", it) } }

    override fun idLocalDe(idCliente: String): Long? = prefs.getLong("local_$idCliente", 0L).takeIf { it > 0 }
    override fun idClienteDe(idLocal: Long): String? = prefs.getString("cliente_$idLocal", null)
    override fun asociar(idCliente: String, idLocal: Long) = prefs.edit {
        putLong("local_$idCliente", idLocal)
        putString("cliente_$idLocal", idCliente)
    }

    override fun versionVista(servidor: String, idCliente: String): Long = prefs.getLong("version_$servidor|$idCliente", 0L)
    override fun guardarVersion(servidor: String, idCliente: String, version: Long) =
        prefs.edit { putLong("version_$servidor|$idCliente", version) }

    override fun huellaEnviada(servidor: String, idCliente: String): String? = prefs.getString("huella_$servidor|$idCliente", null)
    override fun guardarHuella(servidor: String, idCliente: String, huella: String) =
        prefs.edit { putString("huella_$servidor|$idCliente", huella) }
}

/** [ServicioBackend] sobre Retrofit. */
class BackendSync(
    private val api: BackendApi,
    private val almacen: AlmacenBackend,
    private val codificarFoto: (String?) -> String? = ::fotoBase64DesdeArchivo,
    private val log: (String, Throwable?) -> Unit = { mensaje, error -> Log.w(TAG, mensaje, error) },
) : ServicioBackend {

    constructor(context: Context) : this(api = crearApi(), almacen = PreferenciasBackend(context))

    private val prefijoPropio: String get() = "${almacen.instalacion()}_"

    /** Las importadas conservan el identificador que les dio el servidor: así nunca se suben como nuevas. */
    private fun idClienteDe(idLocal: Long): String = almacen.idClienteDe(idLocal) ?: "$prefijoPropio$idLocal"

    /** Identidad del servidor, o null si está apagado (lo normal fuera de la defensa). */
    private suspend fun servidor(): String? = intentar("El backend no responde", null) { api.servidor().id }

    private suspend fun <T> intentar(mensaje: String, siFalla: T, accion: suspend () -> T): T = try {
        accion()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log(mensaje, e)
        siFalla
    }

    override suspend fun cambios(): List<CambioBackend> {
        val servidor = servidor() ?: return emptyList()
        return intentar("No se pudieron leer las muestras del backend", emptyList()) {
            val prefijo = prefijoPropio
            api.listarMuestras().mapNotNull { r ->
                val idCliente = r.idCliente ?: return@mapNotNull null
                val idLocal = almacen.idLocalDe(idCliente)
                    ?: idCliente.takeIf { it.startsWith(prefijo) }?.removePrefix(prefijo)?.toLongOrNull()
                // Solo lo propio y lo creado en el servidor: no se traen las muestras de otros teléfonos
                if (idLocal == null && !idCliente.startsWith(PREFIJO_API)) return@mapNotNull null
                if (r.actualizadaEn <= almacen.versionVista(servidor, idCliente)) return@mapNotNull null
                val estado = EstadoMuestra.entries.find { it.name == r.estado } ?: return@mapNotNull null
                CambioBackend(
                    idCliente = idCliente, servidor = servidor, version = r.actualizadaEn, idLocal = idLocal,
                    centroId = r.centroId, tren = r.tren, linea = r.linea, profundidadM = r.profundidadM,
                    tallaMm = r.tallaMm, conteoIndM = r.conteoIndM, cholgasIndM = r.cholgasIndM, tramoCm = r.tramoCm,
                    observacion = r.observacion.orEmpty(), estado = estado, registradaEn = r.registradaEn,
                    operadorRut = r.operadorRut.orEmpty(), revisadaEn = r.revisadaEn, comentario = r.comentarioSupervisor,
                    valoresOperario = if (r.conteoOperario != null && r.tallaOperario != null) {
                        ValoresOperario(r.conteoOperario, r.tallaOperario, r.cholgasOperario)
                    } else null,
                )
            }
        }
    }

    override fun revisado(cambio: CambioBackend, idLocal: Long?) {
        if (idLocal != null && cambio.idLocal == null) almacen.asociar(cambio.idCliente, idLocal)
        almacen.guardarVersion(cambio.servidor, cambio.idCliente, cambio.version)
    }

    override suspend fun enviar(muestras: List<Muestra>) {
        val servidor = servidor() ?: return
        for (m in muestras) {
            val idCliente = idClienteDe(m.id)
            val huella = huellaDe(m)
            if (almacen.huellaEnviada(servidor, idCliente) == huella) continue
            try {
                enviarUna(servidor, idCliente, m)
                almacen.guardarHuella(servidor, idCliente, huella)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                log("El backend no aceptó la muestra ${m.id} (HTTP ${e.code()})", e)
                // Rechazada por una regla del servidor: insistir con lo mismo no sirve
                if (e.code() in 400..499) almacen.guardarHuella(servidor, idCliente, huella)
            } catch (e: Exception) {
                log("Se cortó el envío al backend", e)
                return
            }
        }
    }

    private suspend fun enviarUna(servidor: String, idCliente: String, m: Muestra) {
        val remota = api.crearMuestra(BackendApi.OPERARIO, aDto(idCliente, m))
        // Si el servidor trae otros valores (alguien los cambió), no se da por vista: la aplica cambios()
        if (mismosDatos(remota, m)) almacen.guardarVersion(servidor, idCliente, remota.actualizadaEn)

        if (!remota.tieneFoto) {
            codificarFoto(m.fotoRuta)?.let { base64 ->
                val nombre = m.fotoRuta?.substringAfterLast('/').orEmpty().ifBlank { "muestra_${m.id}.jpg" }
                api.subirFoto(remota.id, BackendApi.OPERARIO, FotoEnviarDto(nombre, "image/jpeg", base64))
            }
        }
        // El dictamen lo hizo un supervisor en el teléfono o en el portal; el servidor acepta solo el primero
        if (m.estado != EstadoMuestra.PENDIENTE && remota.estado == EstadoMuestra.PENDIENTE.name) {
            val corrige = m.estado == EstadoMuestra.CORREGIDO && !mismasMedidas(remota, m)
            val dictaminada = api.dictaminar(
                remota.id, BackendApi.SUPERVISOR,
                RevisionEnviarDto(
                    estado = m.estado.name,
                    comentario = m.comentarioSupervisor,
                    supervisorNombre = m.revisadaPor,
                    supervisorRut = DatosIniciales.RUT_SUPERVISOR,
                    conteoIndM = m.conteoIndM.takeIf { corrige },
                    tallaMm = m.tallaMm.takeIf { corrige },
                    cholgasIndM = m.cholgasIndM.takeIf { corrige },
                ),
            )
            almacen.guardarVersion(servidor, idCliente, dictaminada.actualizadaEn)
        }
    }

    override suspend fun eliminar(idLocal: Long) {
        intentar("No se pudo borrar la muestra $idLocal del backend", Unit) {
            api.listarMuestras(idClienteDe(idLocal)).forEach { api.eliminarMuestra(it.id, BackendApi.SUPERVISOR) }
        }
    }

    private fun aDto(idCliente: String, m: Muestra): MuestraEnviarDto {
        // Al crear se manda lo que registró el operario; la corrección viaja con el dictamen
        val operario = m.valoresOperario
        return MuestraEnviarDto(
            idCliente = idCliente, codigo = m.codigo, centroId = m.centroId, tren = m.tren, linea = m.linea,
            profundidadM = m.profundidadM,
            tallaMm = operario?.tallaMm ?: m.tallaMm,
            conteoIndM = operario?.conteoIndM ?: m.conteoIndM,
            cholgasIndM = if (operario != null) operario.cholgasIndM else m.cholgasIndM,
            tramoCm = m.tramoCm, observacion = m.observacion, registradaEn = m.registradaEn,
            operador = m.operador, operadorRut = m.operadorRut,
            lat = m.posicion?.lat, lon = m.posicion?.lon, precisionM = m.posicion?.precisionM,
            distanciaCentroM = Distancia.alCentro(m),
            climaTempAireC = m.clima?.tempAireC, climaVientoKn = m.clima?.vientoKn, climaOlaM = m.clima?.olaM,
            climaTempAguaC = m.clima?.tempAguaC, climaDescripcion = m.clima?.codigo?.let { CodigoTiempo.descripcion(it) },
        )
    }

    private fun mismasMedidas(r: MuestraRemotaDto, m: Muestra) =
        r.conteoIndM == m.conteoIndM && r.tallaMm == m.tallaMm && r.cholgasIndM == m.cholgasIndM

    private fun mismosDatos(r: MuestraRemotaDto, m: Muestra) = mismasMedidas(r, m) && r.estado == m.estado.name &&
        r.profundidadM == m.profundidadM && r.tramoCm == m.tramoCm && r.observacion.orEmpty() == m.observacion

    private fun huellaDe(m: Muestra) = listOf(
        m.estado, m.conteoIndM, m.tallaMm, m.cholgasIndM, m.comentarioSupervisor, m.fotoRuta != null, m.posicion != null, m.clima != null,
    ).joinToString("|")

    companion object {
        private const val TAG = "BackendSync"
        /** Prefijo que pone el servidor a las muestras creadas en él (por ejemplo desde Swagger). */
        private const val PREFIJO_API = "api_"
        private const val LADO_MAX = 800
        private const val MAX_BYTES = 500_000

        fun crearApi(urlBase: String = BackendApi.URL_BASE): BackendApi = Retrofit.Builder()
            .baseUrl(urlBase)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
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
