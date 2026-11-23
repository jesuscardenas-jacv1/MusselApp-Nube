package cl.aldemar.musselapp.ui.muestra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.aldemar.musselapp.MusselApplication
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.repository.MuestraRepository
import cl.aldemar.musselapp.data.ubicacion.Ubicador
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Datos de la muestra que se está registrando, compartidos entre
 * Nueva Muestra, Captura y Vista previa.
 */
data class BorradorMuestra(
    val parametros: ParametrosMuestra? = null,
    val fotoRuta: String? = null,
    // Conteo y talla los ingresa el operario en Vista previa
    val conteoIndM: Int? = null,
    val tallaMm: Int? = null,
    // Cholgas: otra especie de la misma familia; se anotan aparte (0 si no hay)
    val cholgasIndM: Int? = null,
    // Largo del tramo de cuerda muestreado, en cm
    val tramoCm: Int? = null,
    val observacion: String = "",
)

/** Posición del teléfono para la muestra en curso: se busca al llegar a la Vista previa. */
data class EstadoUbicacion(
    val buscando: Boolean = false,
    val posicion: Posicion? = null,
)

/** Rangos aceptados al ingresar las mediciones (evitan errores de tipeo, no son límites biológicos). */
const val CONTEO_MAXIMO_IND_M = 5000
const val TALLA_MAXIMA_MM = 100
const val TRAMO_MAXIMO_CM = 500

/**
 * Revisa que el borrador tenga todo lo obligatorio para guardarse.
 * Devuelve el primer problema encontrado, o null si se puede guardar.
 */
fun validarBorrador(borrador: BorradorMuestra): String? = when {
    borrador.parametros == null -> "Faltan centro, tren, línea y estrato"
    borrador.fotoRuta == null -> "Falta la fotografía de la muestra"
    borrador.conteoIndM == null -> "Ingresa el conteo de individuos por metro"
    borrador.conteoIndM !in 1..CONTEO_MAXIMO_IND_M -> "El conteo debe estar entre 1 y $CONTEO_MAXIMO_IND_M ind/m"
    borrador.tallaMm == null -> "Ingresa la talla media en mm"
    borrador.tallaMm !in 1..TALLA_MAXIMA_MM -> "La talla debe estar entre 1 y $TALLA_MAXIMA_MM mm"
    borrador.cholgasIndM == null -> "Ingresa el conteo de cholgas (0 si no hay)"
    borrador.cholgasIndM !in 0..CONTEO_MAXIMO_IND_M -> "Las cholgas deben estar entre 0 y $CONTEO_MAXIMO_IND_M ind/m"
    borrador.tramoCm == null -> "Ingresa el largo del tramo muestreado en cm"
    borrador.tramoCm !in 1..TRAMO_MAXIMO_CM -> "El tramo debe estar entre 1 y $TRAMO_MAXIMO_CM cm"
    else -> null
}

class BorradorMuestraViewModel(
    private val repository: MuestraRepository = MusselApplication.contenedor.muestraRepository,
    /** Borra el archivo de una foto que ya no se usa (inyectable para las pruebas). */
    private val borrarFoto: (String) -> Unit = { MusselApplication.contenedor.borrarFoto(it) },
    /** Posición del teléfono (inyectable para las pruebas). */
    private val ubicador: Ubicador = Ubicador { MusselApplication.contenedor.ubicador.posicionActual() },
) : ViewModel() {

    private val _ubicacion = MutableStateFlow(EstadoUbicacion())
    val ubicacion: StateFlow<EstadoUbicacion> = _ubicacion.asStateFlow()

    private var busquedaUbicacion: Job? = null

    /**
     * Empieza a leer la posición para que esté lista al confirmar. Se llama al llegar a la
     * Vista previa; si ya hay una búsqueda en curso no se repite.
     */
    fun buscarUbicacion() {
        if (busquedaUbicacion?.isActive == true) return
        _ubicacion.update { it.copy(buscando = true) }
        busquedaUbicacion = viewModelScope.launch {
            val posicion = ubicador.posicionActual()
            // Si esta lectura falla se conserva la anterior de la misma muestra
            _ubicacion.update { EstadoUbicacion(buscando = false, posicion = posicion ?: it.posicion) }
        }
    }

    private val _borrador = MutableStateFlow(BorradorMuestra())
    val borrador: StateFlow<BorradorMuestra> = _borrador.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    fun guardarParametros(parametros: ParametrosMuestra) {
        _borrador.update { it.copy(parametros = parametros) }
    }

    fun guardarFoto(ruta: String) = cambiarFoto(ruta)

    /** Cambia la foto desde Vista previa; null significa que se eliminó. */
    fun actualizarFoto(ruta: String?) = cambiarFoto(ruta)

    // Al repetir o eliminar la foto, la anterior se borra para que no quede un archivo sin muestra
    private fun cambiarFoto(ruta: String?) {
        val anterior = _borrador.value.fotoRuta
        _borrador.update { it.copy(fotoRuta = ruta) }
        if (anterior != null && anterior != ruta) borrarFoto(anterior)
    }

    /** Corrige centro, tren, línea o estrato sin volver al formulario. */
    fun actualizarParametros(parametros: ParametrosMuestra) {
        _borrador.update { it.copy(parametros = parametros) }
    }

    fun actualizarObservacion(texto: String) {
        _borrador.update { it.copy(observacion = texto) }
    }

    /** null significa que el operario borró el valor. */
    fun actualizarConteo(conteoIndM: Int?) {
        _borrador.update { it.copy(conteoIndM = conteoIndM) }
    }

    fun actualizarTalla(tallaMm: Int?) {
        _borrador.update { it.copy(tallaMm = tallaMm) }
    }

    /** null significa que el operario borró el valor; 0 es que contó y no había cholgas. */
    fun actualizarCholgas(cholgasIndM: Int?) {
        _borrador.update { it.copy(cholgasIndM = cholgasIndM) }
    }

    /** null significa que el operario borró el valor. */
    fun actualizarTramo(tramoCm: Int?) {
        _borrador.update { it.copy(tramoCm = tramoCm) }
    }

    /**
     * Guarda la muestra en la base local a nombre de [operador].
     * Si se guarda, limpia el borrador y llama a [alGuardar] con el código de la muestra (ej. "Muestra 012");
     * si falta un dato o la base lo rechaza, llama a [alFallar] con el motivo.
     */
    fun confirmar(
        operador: Usuario,
        alGuardar: (codigo: String) -> Unit,
        alFallar: (String) -> Unit,
    ) {
        if (_guardando.value) return
        val actual = _borrador.value
        validarBorrador(actual)?.let { problema ->
            alFallar(problema)
            return
        }
        // validarBorrador ya comprobó que estos datos no son null
        val parametros = actual.parametros!!
        val nueva = Muestra(
            id = 0, // lo asigna la base al insertar
            centroId = parametros.centroId,
            linea = parametros.linea,
            tren = parametros.tren,
            profundidadM = parametros.profundidadM,
            tallaMm = actual.tallaMm!!,
            conteoIndM = actual.conteoIndM!!,
            cholgasIndM = actual.cholgasIndM!!,
            tramoCm = actual.tramoCm!!,
            observacion = actual.observacion.trim(),
            estado = EstadoMuestra.PENDIENTE,
            registradaEn = System.currentTimeMillis(),
            sincronizada = false,
            operador = operador.nombre,
            operadorRut = operador.rut,
            fotoRuta = actual.fotoRuta,
        )

        _guardando.value = true
        viewModelScope.launch {
            try {
                // La muestra se guarda con o sin posición: se espera un poco a la lectura en curso, no más
                withTimeoutOrNull(ESPERA_UBICACION_MS) { busquedaUbicacion?.join() }
                val id = repository.agregarMuestra(nueva.copy(posicion = _ubicacion.value.posicion))
                limpiar()
                alGuardar(nueva.copy(id = id).codigo)
            } catch (e: Exception) {
                // Antes un error aquí cerraba la app; ahora se avisa y el borrador se conserva
                alFallar("No se pudo guardar la muestra: ${e.message ?: "error de la base de datos"}")
            } finally {
                _guardando.value = false
            }
        }
    }

    /**
     * Abandona la muestra a medio registrar (por ejemplo al cerrar sesión) y borra su foto.
     * No usar después de guardar: esa foto ya pertenece a la muestra (para eso está [limpiar]).
     */
    fun descartar() {
        _borrador.value.fotoRuta?.let(borrarFoto)
        limpiar()
    }

    /** Se llama al terminar el muestreo, para empezar el siguiente desde cero. La foto se conserva. */
    fun limpiar() {
        _borrador.value = BorradorMuestra()
        busquedaUbicacion?.cancel()
        _ubicacion.value = EstadoUbicacion()
    }

    private companion object {
        const val ESPERA_UBICACION_MS = 4_000L
    }
}
