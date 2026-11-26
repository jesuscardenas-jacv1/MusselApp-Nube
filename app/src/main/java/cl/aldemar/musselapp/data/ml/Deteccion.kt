package cl.aldemar.musselapp.data.ml

/** Clases que entrega el modelo (mismo orden que sus columnas: 0 chorito, 1 cholga, 2 referencia). */
enum class Especie { CHORITO, CHOLGA, REFERENCIA }

/**
 * Un individuo (o la referencia de 40 mm) encontrado en la foto.
 * El recuadro está normalizado 0..1 respecto del ancho y alto de la foto; (x, y) es la esquina superior izquierda.
 */
data class Deteccion(
    val especie: Especie,
    val confianza: Float,
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
)

/** Lo que devuelve un detector para una foto. */
data class ResultadoDeteccion(
    val detecciones: List<Deteccion>,
    val modeloVersion: String,
    val milisegundos: Long,
)
