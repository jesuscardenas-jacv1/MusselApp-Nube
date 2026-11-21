package cl.aldemar.musselapp.ui.muestra

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Marca sobre un individuo de la foto. Las coordenadas son relativas a la imagen (de 0 a 1),
 * así no cambian con el zoom ni con el tamaño de la pantalla.
 */
data class Marca(val x: Float, val y: Float)

/**
 * Conteo asistido sobre la foto: las marcas puestas y el historial para deshacer.
 * Es inmutable: cada acción devuelve un conteo nuevo.
 */
class ConteoMarcas private constructor(
    val marcas: List<Marca>,
    private val historial: List<List<Marca>>,
) {
    constructor() : this(emptyList(), emptyList())

    val total: Int get() = marcas.size
    val puedeDeshacer: Boolean get() = historial.isNotEmpty()

    /**
     * Toque en [punto]: si cae sobre una marca (a menos de [radio], medido en alto de la imagen),
     * la quita; si no, agrega una nueva. [proporcion] es ancho / alto de la imagen, para que el
     * radio sea un círculo y no un óvalo.
     */
    fun tocar(punto: Marca, radio: Float, proporcion: Float = 1f): ConteoMarcas {
        val cercana = marcas.indices
            .map { it to distancia(marcas[it], punto, proporcion) }
            .filter { (_, d) -> d <= radio }
            .minByOrNull { (_, d) -> d }
            ?.first
        val nuevas = if (cercana != null) marcas.filterIndexed { i, _ -> i != cercana } else marcas + punto
        return ConteoMarcas(nuevas, historial + listOf(marcas))
    }

    /** Vuelve al estado anterior a la última acción (agregar, quitar o limpiar). */
    fun deshacer(): ConteoMarcas =
        if (historial.isEmpty()) this else ConteoMarcas(historial.last(), historial.dropLast(1))

    /** Quita todas las marcas; se puede deshacer. */
    fun limpiar(): ConteoMarcas =
        if (marcas.isEmpty()) this else ConteoMarcas(emptyList(), historial + listOf(marcas))

    companion object {
        private fun distancia(a: Marca, b: Marca, proporcion: Float): Float =
            hypot((a.x - b.x) * proporcion, a.y - b.y)

        /**
         * Individuos por metro: [total] marcas contadas en un tramo de [tramoCm] centímetros.
         * null si todavía no hay marcas o falta el tramo.
         */
        fun indPorMetro(total: Int, tramoCm: Int?): Int? =
            if (total <= 0 || tramoCm == null || tramoCm <= 0) null else (total * 100.0 / tramoCm).roundToInt()
    }
}
