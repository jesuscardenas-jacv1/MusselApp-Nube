package cl.aldemar.musselapp.data.model

/**
 * Tiempo y estado del mar al registrar la muestra (fuente: Open-Meteo, dato modelado).
 * Cada valor puede faltar por separado: el servicio de mar no cubre todos los puntos.
 */
data class Clima(
    val tempAireC: Double? = null,
    /** Viento medio a 10 m, en nudos. */
    val vientoKn: Double? = null,
    val rachaKn: Double? = null,
    /** Dirección desde donde sopla el viento, en grados (0 = norte). */
    val vientoDir: Int? = null,
    /** Código WMO del tiempo; ver [cl.aldemar.musselapp.util.CodigoTiempo]. */
    val codigo: Int? = null,
    val olaM: Double? = null,
    /** Temperatura superficial del mar. */
    val tempAguaC: Double? = null,
    /** Nivel del mar respecto al nivel medio, en metros (marea). */
    val mareaM: Double? = null,
    val obtenidoEn: Long,
)
