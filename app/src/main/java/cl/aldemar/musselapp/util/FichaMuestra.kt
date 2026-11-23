package cl.aldemar.musselapp.util

import cl.aldemar.musselapp.data.model.Alerta
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Muestra
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Grupo de datos de la ficha (por ejemplo "Condiciones al muestrear"). */
data class SeccionFicha(val titulo: String, val filas: List<Pair<String, String>>)

/** Dato destacado de la ficha: valor grande con su unidad. */
data class MetricaFicha(val etiqueta: String, val valor: String, val unidad: String)

/**
 * Contenido de la ficha técnica de una muestra: lo usan el PDF (descargar e imprimir)
 * y el texto que acompaña al compartir. No depende de Android, así se prueba en la JVM.
 */
object FichaMuestra {

    /** Pie de la ficha: el caso es académico y los datos son ficticios. */
    const val PIE = "MusselApp · Caso académico AquaSample / ALDEMAR SpA · Datos ficticios · Clima: Open-Meteo"

    fun titulo(muestra: Muestra): String = "Ficha técnica · ${muestra.codigo}"

    /** Nombre del archivo PDF, por ejemplo "Ficha_Muestra_012.pdf". */
    fun nombreArchivo(muestra: Muestra): String = "Ficha_${muestra.codigo.replace(' ', '_')}.pdf"

    /** Filas "etiqueta → valor" de la ficha, en el orden en que se muestran. */
    fun filas(muestra: Muestra, zona: TimeZone = TimeZone.getDefault()): List<Pair<String, String>> = listOfNotNull(
        "Centro de cultivo" to nombreCentro(muestra.centroId),
        "Tren / Línea" to "Tren ${muestra.tren} · ${muestra.codigoLinea}",
        "Profundidad" to "${muestra.profundidadM} m",
        "Conteo" to "${muestra.conteoIndM} ind/m",
        "Cholgas" to (muestra.cholgasIndM?.let { "$it ind/m" } ?: "No registrado"),
        "Tramo muestreado" to (muestra.tramoCm?.let { "$it cm" } ?: "No registrado"),
        "Talla media" to "${muestra.tallaMm} mm",
        "Rendimiento estimado" to "${muestra.rendimientoKgM} kg/m",
        "Ubicación" to CodigoTiempo.posicion(muestra.posicion),
        "Distancia al centro" to distanciaAlCentro(muestra),
        "Tiempo" to CodigoTiempo.tiempo(muestra.clima),
        "Mar" to CodigoTiempo.mar(muestra.clima),
        "Clima consultado" to CodigoTiempo.consultado(muestra.clima, zona),
        "Alertas al registrar" to alertas(muestra),
        "Estado" to muestra.estado.etiqueta,
        "Registrada" to fecha(muestra.registradaEn, zona),
        "Operario" to muestra.operador.ifBlank { "—" },
        "Revisada por" to (muestra.revisadaPor ?: "Sin revisión"),
        "Comentario del supervisor" to (muestra.comentarioSupervisor?.takeIf { it.isNotBlank() } ?: "—"),
        valoresOperario(muestra)?.let { "Registrado por el operario" to it },
        "Observación de terreno" to muestra.observacion.ifBlank { "—" },
        "Sincronizada" to if (muestra.sincronizada) "Sí" else "No (pendiente de envío)",
    )

    /** Mediciones destacadas del PDF: densidad, talla, cholgas y rendimiento. */
    fun metricas(muestra: Muestra): List<MetricaFicha> = listOf(
        MetricaFicha("Densidad", "${muestra.conteoIndM}", "ind/m"),
        MetricaFicha("Talla media", "${muestra.tallaMm}", "mm"),
        MetricaFicha("Cholgas", muestra.cholgasIndM?.toString() ?: "—", if (muestra.cholgasIndM != null) "ind/m" else "sin registro"),
        MetricaFicha("Rendimiento est.", String.format(ES, "%.1f", muestra.rendimientoKgM), "kg/m"),
    )

    /** "+54 % sobre el estándar (380 ind/m)" o "−8 % bajo el estándar (380 ind/m)". */
    fun desvio(muestra: Muestra): String {
        val pct = muestra.desvioDensidadPct
        val estandar = "estándar (${Muestra.DENSIDAD_ESTANDAR_IND_M} ind/m)"
        return when {
            pct > 0 -> "+$pct % sobre el $estandar"
            pct < 0 -> "−${-pct} % bajo el $estandar"
            else -> "Igual al $estandar"
        }
    }

    /**
     * Lo que registró el operario si el supervisor corrigió los valores en el portal, por ejemplo
     * "420 ind/m · 52 mm · 8 cholgas/m". null si no hubo corrección.
     */
    fun valoresOperario(muestra: Muestra): String? = muestra.valoresOperario?.let { v ->
        val cholgas = v.cholgasIndM?.let { "$it cholgas/m" } ?: "cholgas sin registrar"
        "${v.conteoIndM} ind/m · ${v.tallaMm} mm · $cholgas"
    }

    /** Alertas de la muestra, las críticas primero (para pintarlas en el PDF). */
    fun listaAlertas(muestra: Muestra): List<Alerta> = AlertasMuestra.evaluar(muestra)

    /** Datos del PDF agrupados por tema, en el orden en que se muestran. */
    fun secciones(muestra: Muestra, zona: TimeZone = TimeZone.getDefault()): List<SeccionFicha> = listOf(
        SeccionFicha(
            "Identificación",
            listOf(
                "Centro de cultivo" to nombreCentro(muestra.centroId),
                "Tren / Línea" to "Tren ${muestra.tren} · ${muestra.codigoLinea}",
                "Profundidad" to "${muestra.profundidadM} m",
                "Operario" to muestra.operador.ifBlank { "—" },
                "Registrada" to fecha(muestra.registradaEn, zona),
            ),
        ),
        SeccionFicha(
            "Condiciones al muestrear",
            listOf(
                "Ubicación" to CodigoTiempo.posicion(muestra.posicion),
                "Distancia al centro" to distanciaAlCentro(muestra),
                "Tiempo" to CodigoTiempo.tiempo(muestra.clima),
                "Mar" to CodigoTiempo.mar(muestra.clima),
                "Clima consultado" to CodigoTiempo.consultado(muestra.clima, zona),
            ),
        ),
        SeccionFicha(
            "Revisión del supervisor",
            listOfNotNull(
                "Estado" to muestra.estado.etiqueta,
                "Revisada por" to (muestra.revisadaPor ?: "Sin revisión"),
                "Fecha de revisión" to (muestra.revisadaEn?.let { fecha(it, zona) } ?: "—"),
                "Comentario" to (muestra.comentarioSupervisor?.takeIf { it.isNotBlank() } ?: "—"),
                valoresOperario(muestra)?.let { "Registrado por el operario" to it },
            ),
        ),
        SeccionFicha(
            "Trazabilidad",
            listOf(
                "Código" to muestra.codigo,
                "Envío a la nube" to if (muestra.sincronizada) "Sincronizada" else "Pendiente de envío",
                "Fotografía" to if (muestra.fotoRuta != null) "Adjunta" else "Sin fotografía",
            ),
        ),
    )

    /** Fecha y hora en formato chileno, por ejemplo "17/11/2026 19:36". */
    fun fecha(instante: Long, zona: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", ES).apply { timeZone = zona }.format(Date(instante))

    /** Resumen en texto para compartir junto con el PDF. */
    fun textoCompartir(muestra: Muestra, zona: TimeZone = TimeZone.getDefault()): String =
        buildString {
            appendLine(titulo(muestra))
            filas(muestra, zona).forEach { (etiqueta, valor) -> appendLine("$etiqueta: $valor") }
            append(PIE)
        }

    /** "180 m (dentro del área)" o "3,2 km (fuera del área)". */
    fun distanciaAlCentro(muestra: Muestra): String {
        val centro = CentroCultivo.porId(muestra.centroId)
        val metros = Distancia.alCentro(muestra, centro) ?: return "No registrada"
        val area = if (centro != null && metros > centro.radioM) "fuera del área" else "dentro del área"
        return "${Distancia.texto(metros)} ($area)"
    }

    /** true dentro del radio del centro, false fuera; null si falta la posición o el centro. */
    fun dentroDelArea(muestra: Muestra): Boolean? {
        val centro = CentroCultivo.porId(muestra.centroId) ?: return null
        val metros = Distancia.alCentro(muestra, centro) ?: return null
        return metros <= centro.radioM
    }

    /** "541 m del centro" o "3,2 km del centro"; null si no hay posición. */
    fun distanciaCorta(muestra: Muestra): String? =
        Distancia.alCentro(muestra)?.let { "${Distancia.texto(it)} del centro" }

    /** Desvío de la densidad frente a la referencia, en dos partes: "+18 %" y "sobre la referencia de 380 ind/m". */
    fun desvioPartes(muestra: Muestra): Pair<String, String> {
        val pct = muestra.desvioDensidadPct
        val referencia = "la referencia de ${Muestra.DENSIDAD_ESTANDAR_IND_M} ind/m"
        return when {
            pct > 0 -> "+$pct %" to "sobre $referencia"
            pct < 0 -> "−${-pct} %" to "bajo $referencia"
            else -> "0 %" to "igual a $referencia"
        }
    }

    fun alertas(muestra: Muestra): String =
        AlertasMuestra.evaluar(muestra).joinToString(", ") { it.etiqueta }.ifEmpty { "Sin alertas" }

    fun nombreCentro(id: String): String =
        CentroCultivo.disponibles.firstOrNull { it.id == id }?.nombre ?: id

    private val ES = Locale("es", "CL")
}
