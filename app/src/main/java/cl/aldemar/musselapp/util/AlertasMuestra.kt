package cl.aldemar.musselapp.util

import cl.aldemar.musselapp.data.model.Alerta
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Muestra
import kotlin.math.abs

/**
 * Límites de las alertas, juntos para cambiarlos en un solo lugar.
 * Son supuestos del caso académico, no normas del rubro.
 */
object Umbrales {
    /** Sobre esta precisión la posición no sirve para decidir si está dentro del área. */
    const val PRECISION_MAX_M = 100.0
    /** Desvío tolerado respecto a la densidad estándar (misma regla que el portal). */
    const val DESVIO_DENSIDAD_PCT = 20
    /** Cholgas sobre este porcentaje del conteo de choritos. */
    const val CHOLGAS_MAX_PCT = 10
    const val TALLA_MIN_MM = 30
    const val TALLA_MAX_MM = 90
    const val RACHA_MAX_KN = 25.0
    const val OLA_MAX_M = 1.0
}

/** Reglas que marcan una muestra sin esperar la revisión. Funciones puras: se prueban en la JVM. */
object AlertasMuestra {

    /** Alertas de la muestra, las críticas primero. */
    fun evaluar(
        muestra: Muestra,
        centro: CentroCultivo? = CentroCultivo.porId(muestra.centroId),
    ): List<Alerta> {
        val alertas = mutableListOf<Alerta>()

        val posicion = muestra.posicion
        if (posicion == null) {
            alertas += Alerta.SIN_UBICACION
        } else {
            val distancia = Distancia.alCentro(muestra, centro)
            if (distancia != null && centro != null && distancia > centro.radioM) alertas += Alerta.FUERA_DE_AREA
            if ((posicion.precisionM ?: 0.0) > Umbrales.PRECISION_MAX_M) alertas += Alerta.UBICACION_IMPRECISA
        }

        if (muestra.conteoIndM < Muestra.DENSIDAD_MINIMA_IND_M) {
            alertas += Alerta.DENSIDAD_BAJA
        } else if (abs(muestra.conteoIndM - Muestra.DENSIDAD_ESTANDAR_IND_M) * 100 >
            Umbrales.DESVIO_DENSIDAD_PCT * Muestra.DENSIDAD_ESTANDAR_IND_M
        ) {
            // Bajo el mínimo ya es "Densidad baja": aquí solo queda el exceso sobre el estándar
            alertas += Alerta.DESVIO_DENSIDAD
        }

        val cholgas = muestra.cholgasIndM
        if (cholgas != null && cholgas * 100 > Umbrales.CHOLGAS_MAX_PCT * muestra.conteoIndM) {
            alertas += Alerta.CHOLGAS_ALTAS
        }

        if (muestra.tallaMm < Umbrales.TALLA_MIN_MM || muestra.tallaMm > Umbrales.TALLA_MAX_MM) {
            alertas += Alerta.TALLA_FUERA_DE_RANGO
        }

        val clima = muestra.clima
        if (clima != null &&
            ((clima.rachaKn ?: 0.0) > Umbrales.RACHA_MAX_KN || (clima.olaM ?: 0.0) > Umbrales.OLA_MAX_M)
        ) {
            alertas += Alerta.MAL_TIEMPO
        }

        return alertas.sortedByDescending { it.critica }
    }
}
