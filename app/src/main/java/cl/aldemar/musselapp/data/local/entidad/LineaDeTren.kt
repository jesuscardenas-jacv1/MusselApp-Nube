package cl.aldemar.musselapp.data.local.entidad

/** Fila de la consulta que lista las líneas de un centro junto al código de su tren. */
data class LineaDeTren(
    val tren: String,
    val linea: Int,
)
