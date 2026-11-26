package cl.aldemar.musselapp.data.ml

import android.graphics.Bitmap
import kotlin.random.Random

/**
 * Detector de mentira para construir y probar la pantalla de revisión sin el modelo.
 * Siempre devuelve lo mismo para una foto del mismo tamaño: [CHORITOS] choritos, [CHOLGAS] cholgas
 * y la referencia de 40 mm en la esquina superior izquierda.
 */
class DetectorSimulado : DetectorMuestra {

    override suspend fun detectar(foto: Bitmap): ResultadoDeteccion =
        ResultadoDeteccion(generar(foto.width, foto.height), VERSION, milisegundos = 0)

    companion object {
        const val VERSION = "simulado"
        const val CHORITOS = 36
        const val CHOLGAS = 4

        private const val COLUMNAS = 8
        private const val FILAS = 5
        private const val SEMILLA = 2026

        /** Lado de la referencia, como fracción del lado menor de la foto. */
        private const val LADO_REFERENCIA = 0.12f

        /** Detecciones de una foto de [anchoPx] × [altoPx]. Separado de [detectar] para probarlo sin Android. */
        fun generar(anchoPx: Int, altoPx: Int): List<Deteccion> {
            require(anchoPx > 0 && altoPx > 0) { "La foto no tiene tamaño" }
            val azar = Random(SEMILLA)

            // Cuadrada en píxeles (si no, Calibre.mmPorPixel la rechaza): el lado normalizado depende de la proporción
            val ladoPx = minOf(anchoPx, altoPx) * LADO_REFERENCIA
            val referencia = Deteccion(
                Especie.REFERENCIA, confianza = 0.95f,
                x = 0.02f, y = 0.02f, ancho = ladoPx / anchoPx, alto = ladoPx / altoPx,
            )

            // Una grilla de 8 × 5 debajo de la referencia; las cholgas, repartidas (cada décima posición)
            val arriba = referencia.y + referencia.alto + 0.02f
            val celdaAncho = 0.96f / COLUMNAS
            val celdaAlto = (0.98f - arriba) / FILAS
            val moluscos = (0 until COLUMNAS * FILAS).map { i ->
                val ancho = celdaAncho * (0.55f + 0.3f * azar.nextFloat())
                val alto = celdaAlto * (0.45f + 0.3f * azar.nextFloat())
                Deteccion(
                    especie = if (i % 10 == 9) Especie.CHOLGA else Especie.CHORITO,
                    confianza = 0.45f + 0.5f * azar.nextFloat(),
                    x = 0.02f + (i % COLUMNAS) * celdaAncho + (celdaAncho - ancho) * azar.nextFloat(),
                    y = arriba + (i / COLUMNAS) * celdaAlto + (celdaAlto - alto) * azar.nextFloat(),
                    ancho = ancho,
                    alto = alto,
                )
            }
            return moluscos + referencia
        }
    }
}
