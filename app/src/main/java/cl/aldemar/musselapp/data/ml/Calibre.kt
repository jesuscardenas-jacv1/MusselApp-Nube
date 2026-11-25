package cl.aldemar.musselapp.data.ml

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Calibre (largo en mm) de cada individuo a partir de la foto, con el cuadrado de referencia impreso de 40 mm.
 *
 * Todo trabaja sobre la luminancia (0 = negro, 255 = blanco) de un recorte de la foto, en Kotlin puro:
 * no depende de Android ni de librerías externas, así que se prueba en la JVM. Quien integra (la pantalla de
 * conteo) recorta la foto con el recuadro de cada detección y pasa sus píxeles como [IntArray] de luminancia.
 */
object Calibre {

    /** Lado del cuadrado de referencia impreso. */
    const val REFERENCIA_MM = 40.0

    /** Un recuadro de referencia más alargado que esto indica una foto inclinada: no se calcula el calibre. */
    private const val PROPORCION_MIN = 0.9
    private const val PROPORCION_MAX = 1.1

    /** Menos píxeles oscuros que esto no alcanza para medir una concha. */
    private const val PIXELES_MINIMOS = 20

    /** Por debajo de esta nitidez (varianza del Laplaciano) la foto se considera borrosa. */
    const val NITIDEZ_MINIMA = 60.0

    /**
     * Milímetros por píxel según el recuadro de la referencia, medido en píxeles de la foto original.
     * Devuelve null si el recuadro no es casi cuadrado (foto inclinada o referencia mal detectada).
     */
    fun mmPorPixel(anchoReferenciaPx: Double, altoReferenciaPx: Double, ladoMm: Double = REFERENCIA_MM): Double? {
        if (anchoReferenciaPx <= 0 || altoReferenciaPx <= 0) return null
        val proporcion = anchoReferenciaPx / altoReferenciaPx
        if (proporcion !in PROPORCION_MIN..PROPORCION_MAX) return null
        return ladoMm / ((anchoReferenciaPx + altoReferenciaPx) / 2)
    }

    /** Umbral de Otsu: separa los píxeles oscuros (la concha) del fondo claro. */
    fun umbralOtsu(luminancia: IntArray): Int {
        val histograma = IntArray(256)
        luminancia.forEach { histograma[it.coerceIn(0, 255)]++ }
        val total = luminancia.size
        if (total == 0) return 128
        val sumaTotal = (0..255).sumOf { it.toLong() * histograma[it] }
        var sumaFondo = 0L
        var pesoFondo = 0
        var mejorVarianza = -1.0
        var umbral = 128
        for (t in 0..255) {
            pesoFondo += histograma[t]
            if (pesoFondo == 0) continue
            val pesoFrente = total - pesoFondo
            if (pesoFrente == 0) break
            sumaFondo += t.toLong() * histograma[t]
            val mediaFondo = sumaFondo.toDouble() / pesoFondo
            val mediaFrente = (sumaTotal - sumaFondo).toDouble() / pesoFrente
            val varianza = pesoFondo.toDouble() * pesoFrente * (mediaFondo - mediaFrente) * (mediaFondo - mediaFrente)
            if (varianza > mejorVarianza) {
                mejorVarianza = varianza
                umbral = t
            }
        }
        return umbral
    }

    /**
     * Largo del eje mayor, en píxeles, de los píxeles oscuros del recorte (la concha sobre fondo claro).
     * Usa el análisis de componentes principales: la dirección en que los píxeles están más repartidos es el
     * largo de la valva, aunque esté girada. Devuelve null si hay muy pocos píxeles oscuros.
     */
    fun largoEjeMayorPx(luminancia: IntArray, ancho: Int, alto: Int, umbral: Int = umbralOtsu(luminancia)): Double? {
        require(luminancia.size == ancho * alto) { "El recorte debe tener ancho × alto píxeles" }
        var n = 0
        var sx = 0.0
        var sy = 0.0
        for (y in 0 until alto) for (x in 0 until ancho) {
            if (luminancia[y * ancho + x] <= umbral) { n++; sx += x; sy += y }
        }
        if (n < PIXELES_MINIMOS) return null
        val mx = sx / n
        val my = sy / n
        var cxx = 0.0
        var cyy = 0.0
        var cxy = 0.0
        for (y in 0 until alto) for (x in 0 until ancho) {
            if (luminancia[y * ancho + x] <= umbral) {
                val dx = x - mx
                val dy = y - my
                cxx += dx * dx; cyy += dy * dy; cxy += dx * dy
            }
        }
        // Vector propio del mayor valor propio de la matriz de covarianza 2×2
        val traza = cxx + cyy
        val det = cxx * cyy - cxy * cxy
        val lambda = traza / 2 + sqrt(maxOf(0.0, traza * traza / 4 - det))
        var vx = lambda - cyy
        var vy = cxy
        if (abs(vx) < 1e-9 && abs(vy) < 1e-9) { vx = 1.0; vy = 0.0 }
        val norma = sqrt(vx * vx + vy * vy)
        vx /= norma; vy /= norma
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for (y in 0 until alto) for (x in 0 until ancho) {
            if (luminancia[y * ancho + x] <= umbral) {
                val p = (x - mx) * vx + (y - my) * vy
                if (p < min) min = p
                if (p > max) max = p
            }
        }
        return max - min + 1 // +1: un píxel tiene ancho
    }

    /** Largo en mm de la concha del recorte, o null si no se puede medir. */
    fun largoMm(luminancia: IntArray, ancho: Int, alto: Int, mmPorPixel: Double): Double? =
        largoEjeMayorPx(luminancia, ancho, alto)?.let { it * mmPorPixel }

    /** Nitidez de la foto: varianza del Laplaciano. Una foto movida o desenfocada da un valor bajo. */
    fun nitidez(luminancia: IntArray, ancho: Int, alto: Int): Double {
        require(luminancia.size == ancho * alto) { "La imagen debe tener ancho × alto píxeles" }
        if (ancho < 3 || alto < 3) return 0.0
        var suma = 0.0
        var sumaCuadrados = 0.0
        var n = 0
        for (y in 1 until alto - 1) for (x in 1 until ancho - 1) {
            val i = y * ancho + x
            val lap = (luminancia[i - 1] + luminancia[i + 1] + luminancia[i - ancho] + luminancia[i + ancho] - 4 * luminancia[i]).toDouble()
            suma += lap; sumaCuadrados += lap * lap; n++
        }
        val media = suma / n
        return sumaCuadrados / n - media * media
    }

    fun esBorrosa(luminancia: IntArray, ancho: Int, alto: Int): Boolean = nitidez(luminancia, ancho, alto) < NITIDEZ_MINIMA
}

/** Promedio, mínimo, máximo y distribución por rangos de calibre de una especie. */
data class ResumenCalibre(
    val cantidad: Int,
    val promedioMm: Double?,
    val minimoMm: Double?,
    val maximoMm: Double?,
    /** Cantidad de individuos por rango: la clave es el inicio del rango (30 = de 30 a menos de 35 mm). */
    val porRango: Map<Int, Int>,
) {
    companion object {
        const val ANCHO_RANGO_MM = 5

        fun de(largosMm: List<Double>): ResumenCalibre {
            val validos = largosMm.filter { it.isFinite() && it > 0 }
            if (validos.isEmpty()) return ResumenCalibre(0, null, null, null, emptyMap())
            val porRango = validos
                .groupingBy { (floor(it / ANCHO_RANGO_MM) * ANCHO_RANGO_MM).toInt() }
                .eachCount()
                .toSortedMap()
            return ResumenCalibre(validos.size, validos.average(), validos.min(), validos.max(), porRango)
        }
    }
}
