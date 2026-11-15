package cl.aldemar.musselapp.util

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Muestra
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Distancias sobre la superficie terrestre. No depende de Android: se prueba en la JVM. */
object Distancia {

    private const val RADIO_TIERRA_M = 6_371_000.0

    /** Distancia en metros entre dos puntos (fórmula de Haversine). */
    fun metros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Metros entre la posición de la muestra y su centro; null si falta la posición o el centro. */
    fun alCentro(muestra: Muestra, centro: CentroCultivo? = CentroCultivo.porId(muestra.centroId)): Int? {
        val p = muestra.posicion ?: return null
        val c = centro ?: return null
        return metros(p.lat, p.lon, c.lat, c.lon).roundToInt()
    }

    /** "180 m" bajo el kilómetro; "3,2 km" sobre él. */
    fun texto(metros: Int): String =
        if (metros < 1000) "$metros m"
        else String.format(Locale("es", "CL"), "%.1f km", metros / 1000.0)
}
