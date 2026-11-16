package cl.aldemar.musselapp.data.model

/** Posición del teléfono al guardar la muestra. */
data class Posicion(
    val lat: Double,
    val lon: Double,
    /** Precisión que informó el GPS, en metros; null si no la entregó. */
    val precisionM: Double? = null,
)
