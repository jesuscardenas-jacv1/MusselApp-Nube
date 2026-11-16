package cl.aldemar.musselapp.data.model

/** Centro de cultivo de choritos donde trabaja el usuario (datos ficticios). */
data class CentroCultivo(
    val id: String,
    val nombre: String,
    /** Punto de referencia del centro (coordenadas ficticias aproximadas, sobre el agua). */
    val lat: Double,
    val lon: Double,
    /** Radio del área de cultivo: una muestra tomada más lejos se avisa al supervisor. */
    val radioM: Int = RADIO_AREA_M,
) {
    companion object {
        /** Radio por defecto del área de un centro, en metros. */
        const val RADIO_AREA_M = 1500

        // Las mismas coordenadas que usa el mapa del portal del supervisor (INFO_CENTRO)
        val disponibles = listOf(
            CentroCultivo("dalcahue", "Centro Canal Dalcahue - Mód. A (Chiloé Central)", -42.374, -73.622),
            CentroCultivo("calbuco", "Centro Calbuco - Isla Huapi (Seno de Reloncaví)", -41.800, -73.170),
            CentroCultivo("quellon", "Centro Quellón Sur - Ensenada Yaldad", -43.148, -73.722),
            CentroCultivo("melinka", "Centro Melinka - Fiordos de las Guaitecas", -43.905, -73.720),
        )

        fun porId(id: String): CentroCultivo? = disponibles.firstOrNull { it.id == id }
    }
}
