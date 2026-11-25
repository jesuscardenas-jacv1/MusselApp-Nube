package cl.aldemar.musselapp.data.ml

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Convierte la salida cruda del modelo RF-DETR (archivo .tflite) en recuadros con clase y confianza.
 *
 * El modelo entrega dos tensores (contrato en el repo MusselApp-ML, CONTRATO.md):
 * - cajas: [N × 4] con centro x, centro y, ancho y alto, normalizados de 0 a 1;
 * - puntajes: [N × C] sin activar (logits); se les aplica sigmoide. La columna c es la etiqueta c de labels.txt:
 *   0 = chorito, 1 = cholga, 2 = referencia. El modelo trae una columna más al final (C = clases + 1) que no se usa.
 * N es el número de consultas del modelo (300 en Nano). Cada consulta propone a lo más un objeto.
 *
 * Verificado el 2026-11-25 con un RF-DETR Nano entrenado y exportado con rfdetr 1.12.0 (305 de 310 individuos
 * de prueba con la clase correcta): en el .tflite la salida 0 son los puntajes [1, 300, 4] y la salida 1 las
 * cajas [1, 300, 4]. En INT8 pierden el nombre y ambas terminan en 4: hay que tomarlas por posición.
 *
 * Kotlin puro para probarlo en la JVM; quien ejecuta el intérprete (DetectorLiteRt) solo le pasa los arreglos.
 */
object SalidaRfDetr {

    /** Etiqueta de una clase según labels.txt (chorito, cholga, referencia), o null si no existe. */
    fun etiqueta(clase: Int, etiquetas: List<String>): String? = etiquetas.getOrNull(clase)

    /** Recuadro normalizado 0..1 con su esquina superior izquierda en ([x], [y]). [clase] es el índice en labels.txt. */
    data class CajaDetectada(val clase: Int, val confianza: Float, val x: Float, val y: Float, val ancho: Float, val alto: Float)

    /**
     * @param cajas arreglo plano de N × 4 (cx, cy, w, h).
     * @param logits arreglo plano de N × [columnas].
     * @param columnas columnas del tensor de puntajes (4 en nuestro modelo).
     * @param clases cuántas columnas son clases reales (las de labels.txt: 3); las demás se ignoran.
     * @param umbral confianza mínima (después de la sigmoide).
     * @param iouDuplicado si dos cajas se solapan más que esto, se queda la de mayor confianza (RF-DETR casi
     *   no duplica, pero con teselado sí puede pasar). Usa 1.0 para no filtrar.
     */
    fun decodificar(
        cajas: FloatArray, logits: FloatArray, columnas: Int, clases: Int = columnas - 1,
        umbral: Float = 0.40f, iouDuplicado: Float = 0.5f,
    ): List<CajaDetectada> {
        require(clases in 1..columnas) { "clases debe estar entre 1 y columnas" }
        require(cajas.size % 4 == 0) { "cajas debe tener N × 4 valores" }
        val n = cajas.size / 4
        require(logits.size == n * columnas) { "logits debe tener ${n * columnas} valores y tiene ${logits.size}" }

        val candidatas = ArrayList<CajaDetectada>()
        for (i in 0 until n) {
            var mejorClase = 0
            var mejorLogit = logits[i * columnas]
            for (c in 1 until clases) {
                val v = logits[i * columnas + c]
                if (v > mejorLogit) { mejorLogit = v; mejorClase = c }
            }
            val confianza = sigmoide(mejorLogit)
            if (confianza < umbral) continue
            val cx = cajas[i * 4]; val cy = cajas[i * 4 + 1]; val w = cajas[i * 4 + 2]; val h = cajas[i * 4 + 3]
            val x0 = (cx - w / 2).coerceIn(0f, 1f); val y0 = (cy - h / 2).coerceIn(0f, 1f)
            val x1 = (cx + w / 2).coerceIn(0f, 1f); val y1 = (cy + h / 2).coerceIn(0f, 1f)
            if (x1 <= x0 || y1 <= y0) continue
            candidatas += CajaDetectada(mejorClase, confianza, x0, y0, x1 - x0, y1 - y0)
        }
        return if (iouDuplicado >= 1f) candidatas.sortedByDescending { it.confianza } else quitarDuplicados(candidatas, iouDuplicado)
    }

    /** Supresión de no máximos sin importar la clase: dos cajas sobre el mismo individuo cuentan una vez. */
    fun quitarDuplicados(cajas: List<CajaDetectada>, iouMaximo: Float): List<CajaDetectada> {
        val elegidas = ArrayList<CajaDetectada>()
        for (c in cajas.sortedByDescending { it.confianza }) {
            if (elegidas.none { iou(it, c) > iouMaximo }) elegidas += c
        }
        return elegidas
    }

    fun iou(a: CajaDetectada, b: CajaDetectada): Float {
        val ix = max(0f, min(a.x + a.ancho, b.x + b.ancho) - max(a.x, b.x))
        val iy = max(0f, min(a.y + a.alto, b.y + b.alto) - max(a.y, b.y))
        val inter = ix * iy
        val union = a.ancho * a.alto + b.ancho * b.alto - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun sigmoide(v: Float): Float = (1.0 / (1.0 + exp(-v.toDouble()))).toFloat()
}
