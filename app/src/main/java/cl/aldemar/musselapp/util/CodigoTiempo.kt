package cl.aldemar.musselapp.util

import cl.aldemar.musselapp.data.model.Clima
import cl.aldemar.musselapp.data.model.Posicion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Un dato del clima en su recuadro: rótulo, número y unidad (por ejemplo "Racha", "12", "kn"). */
data class DatoCondicion(val etiqueta: String, val valor: String, val unidad: String)

/** Textos del clima para la ficha y las pantallas. No depende de Android: se prueba en la JVM. */
object CodigoTiempo {

    /** Sin dato: el recuadro muestra una raya, igual que los campos vacíos de la app. */
    const val SIN_DATO = "—"

    const val FUENTE = "Open-Meteo (dato modelado)"

    private val es = Locale("es", "CL")

    /** Cuándo se consultó el clima, por ejemplo "21/11/2026 14:32", o "No registrado". */
    fun consultado(c: Clima?, zona: TimeZone = TimeZone.getDefault()): String =
        c?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", es).apply { timeZone = zona }.format(Date(it.obtenidoEn)) }
            ?: "No registrado"

    /**
     * Fuente del clima con la hora de la consulta (Anexo D: el dato es estimado y tiene fecha),
     * por ejemplo "Open-Meteo (dato modelado, consultado el 21/11/2026 a las 14:32)".
     */
    fun fuente(c: Clima?, zona: TimeZone = TimeZone.getDefault()): String {
        if (c == null) return FUENTE
        val (dia, hora) = consultado(c, zona).split(" ")
        return "Open-Meteo (dato modelado, consultado el $dia a las $hora)"
    }

    /** Código WMO del tiempo (el que entrega Open-Meteo) en palabras. */
    fun descripcion(codigo: Int?): String = when (codigo) {
        null -> "Sin dato"
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55, 56, 57 -> "Llovizna"
        61, 63, 66 -> "Lluvia"
        65, 67 -> "Lluvia fuerte"
        71, 73, 75, 77 -> "Nieve"
        80, 81 -> "Chubascos"
        82 -> "Chubascos fuertes"
        85, 86 -> "Chubascos de nieve"
        95, 96, 99 -> "Tormenta"
        else -> "Sin dato"
    }

    /** Punto cardinal desde donde sopla el viento: 0° = N, 90° = E. */
    fun cardinal(grados: Int?): String? {
        if (grados == null) return null
        val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return puntos[(((grados % 360 + 360) % 360 + 22.5) / 45).toInt() % 8]
    }

    /** "Nublado · 17 °C · viento 2 kn del N (racha 5 kn)". */
    fun tiempo(c: Clima?): String {
        if (c == null) return "No registrado"
        val partes = mutableListOf<String>()
        if (c.codigo != null) partes += descripcion(c.codigo)
        c.tempAireC?.let { partes += "${n0(it)} °C" }
        c.vientoKn?.let { viento ->
            val desde = cardinal(c.vientoDir)?.let { " del $it" } ?: ""
            val racha = c.rachaKn?.let { " (racha ${n0(it)} kn)" } ?: ""
            partes += "viento ${n0(viento)} kn$desde$racha"
        }
        return partes.joinToString(" · ").ifEmpty { "No registrado" }
    }

    /** "Ola 0,1 m · agua 16,3 °C · marea +0,28 m". */
    fun mar(c: Clima?): String {
        if (c == null) return "No registrado"
        val partes = mutableListOf<String>()
        c.olaM?.let { partes += "Ola ${n1(it)} m" }
        c.tempAguaC?.let { partes += "agua ${n1(it)} °C" }
        c.mareaM?.let { partes += "marea ${String.format(es, "%+.2f", it)} m" }
        return partes.joinToString(" · ").ifEmpty { "No registrado" }
    }

    /** Aire, viento (con su dirección) y racha, para los recuadros de la tarjeta de condiciones. */
    fun datosTiempo(c: Clima): List<DatoCondicion> = listOf(
        DatoCondicion("Aire", c.tempAireC?.let(::n0) ?: SIN_DATO, "°C"),
        DatoCondicion("Viento", c.vientoKn?.let(::n0) ?: SIN_DATO, cardinal(c.vientoDir)?.let { "kn del $it" } ?: "kn"),
        DatoCondicion("Racha", c.rachaKn?.let(::n0) ?: SIN_DATO, "kn"),
    )

    /** Ola, temperatura del agua y marea. El servicio de mar no cubre todos los puntos: cada uno puede faltar. */
    fun datosMar(c: Clima): List<DatoCondicion> = listOf(
        DatoCondicion("Ola", c.olaM?.let(::n1) ?: SIN_DATO, "m"),
        DatoCondicion("Agua", c.tempAguaC?.let(::n1) ?: SIN_DATO, "°C"),
        DatoCondicion("Marea", c.mareaM?.let { String.format(es, "%+.2f", it) } ?: SIN_DATO, "m"),
    )

    /** true si llegó al menos un dato del mar. */
    fun hayMar(c: Clima): Boolean = c.olaM != null || c.tempAguaC != null || c.mareaM != null

    /** "-42.3741, -73.6220 (±12 m)". */
    fun posicion(p: Posicion?): String {
        if (p == null) return "No registrada"
        val precision = p.precisionM?.let { " (±${n0(it)} m)" } ?: ""
        return String.format(Locale.US, "%.4f, %.4f", p.lat, p.lon) + precision
    }

    private fun n0(v: Double) = String.format(es, "%.0f", v)
    private fun n1(v: Double) = String.format(es, "%.1f", v)
}
