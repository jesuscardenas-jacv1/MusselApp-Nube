package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.ml.ResumenCalibre
import kotlin.math.roundToInt

/**
 * Convierte el resultado del conteo automático en los campos `ml` y `detecciones` que se suben a Firestore,
 * con el formato del contrato (docs/plan-fase-final.md §5) y dentro de los límites de firestore.rules.
 *
 * Usa tipos simples para no depender de cómo guarde las detecciones la pantalla de revisión: quien sube
 * (NubeSync) convierte sus detecciones vigentes (sin las eliminadas ni la referencia) a [DeteccionNube].
 * Funciones puras, sin Firestore, para probarlas en la JVM.
 */
object ResultadoMlNube {

    /** Máximo de detecciones que aceptan las reglas de Firestore. */
    const val MAX_DETECCIONES = 600

    /** Máximo por conteo en las reglas (ml.total, ml.choritos, ml.cholgas). */
    const val MAX_CONTEO = 2000

    data class DeteccionNube(
        val esCholga: Boolean,
        /** Confianza del modelo (0..1); null si la agregó el operario. */
        val confianza: Float?,
        /** Recuadro normalizado 0..1 respecto del ancho y alto de la foto. */
        val x: Float, val y: Float, val ancho: Float, val alto: Float,
        val largoMm: Double?,
        val agregadaPorOperario: Boolean,
    )

    /**
     * Devuelve `mapOf("ml" to …, "detecciones" to …)` para mezclar con los datos de la muestra.
     * Si hay más de [MAX_DETECCIONES], los totales cuentan todas, pero solo se suben las primeras.
     */
    fun aCampos(version: String, detecciones: List<DeteccionNube>, mmPorPx: Double?, editado: Boolean): Map<String, Any> {
        val choritos = detecciones.count { !it.esCholga }
        val cholgas = detecciones.count { it.esCholga }
        val ml = buildMap<String, Any> {
            put("version", version.take(40))
            put("total", (choritos + cholgas).coerceAtMost(MAX_CONTEO))
            put("choritos", choritos.coerceAtMost(MAX_CONTEO))
            put("cholgas", cholgas.coerceAtMost(MAX_CONTEO))
            mmPorPx?.takeIf { it.isFinite() && it > 0 }?.let { put("mmPorPx", redondear(it, 4)) }
            promedio(detecciones.filter { !it.esCholga })?.let { put("tallaChoritoMm", it) }
            promedio(detecciones.filter { it.esCholga })?.let { put("tallaCholgaMm", it) }
            put("editado", editado)
        }
        return mapOf("ml" to ml, "detecciones" to detecciones.take(MAX_DETECCIONES).map(::aMapa))
    }

    private fun aMapa(d: DeteccionNube): Map<String, Any> = buildMap {
        put("e", if (d.esCholga) "K" else "C")
        d.confianza?.let { put("p", redondear(it.toDouble().coerceIn(0.0, 1.0), 2)) }
        put("x", escala(d.x)); put("y", escala(d.y)); put("w", escala(d.ancho)); put("h", escala(d.alto))
        d.largoMm?.takeIf { it.isFinite() && it > 0 }?.let { put("mm", it.roundToInt()) }
        put("o", if (d.agregadaPorOperario) "O" else "M")
    }

    private fun promedio(lista: List<DeteccionNube>): Double? =
        ResumenCalibre.de(lista.mapNotNull { it.largoMm }).promedioMm?.let { redondear(it, 1) }

    /** De 0..1 a entero 0..1000: ocupa menos espacio en el documento que un decimal. */
    private fun escala(v: Float): Int = (v.coerceIn(0f, 1f) * 1000).roundToInt()

    private fun redondear(v: Double, decimales: Int): Double {
        var f = 1.0
        repeat(decimales) { f *= 10 }
        return (v * f).roundToInt() / f
    }
}
