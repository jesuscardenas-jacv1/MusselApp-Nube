package cl.aldemar.musselapp.data.model

/**
 * Trenes de un centro y las líneas de cada uno, tal como están en la base (tablas `trenes` y `lineas`).
 * El formulario de Nueva muestra y la Vista previa arman sus listas con esto.
 */
data class CatalogoCultivo(
    /** Código del tren ("A", "B", "C") con los números de sus líneas. */
    val lineasPorTren: Map<String, List<Int>>,
) {

    /** Códigos de los trenes, en orden. */
    val trenes: List<String> get() = lineasPorTren.keys.sorted()

    /**
     * Líneas que se ofrecen para [tren]. Mientras no se elige un tren (o si no es de este centro)
     * se ofrecen todas las líneas del centro, igual que la lista fija que había antes.
     */
    fun lineasDe(tren: String?): List<Int> =
        (lineasPorTren[tren] ?: lineasPorTren.values.flatten()).distinct().sorted()

    fun tieneTren(tren: String?): Boolean = tren != null && tren in lineasPorTren

    fun tieneLinea(tren: String?, linea: Int?): Boolean = linea != null && linea in lineasDe(tren)

    companion object {
        /**
         * Lo que `DatosIniciales` carga en cada centro: trenes A, B y C con 12 líneas.
         * Se usa mientras la base responde, en las vistas previas y en las pruebas.
         */
        val porDefecto = CatalogoCultivo(listOf("A", "B", "C").associateWith { (1..12).toList() })

        /**
         * Arma el catálogo con las filas (tren, línea) de un centro.
         * Si el centro no tiene trenes en la base se usa [porDefecto], para que el formulario
         * nunca quede sin opciones.
         */
        fun desde(filas: List<Pair<String, Int>>): CatalogoCultivo =
            if (filas.isEmpty()) porDefecto
            else CatalogoCultivo(filas.groupBy({ it.first }, { it.second }))
    }
}
