package cl.aldemar.musselapp.util

import cl.aldemar.musselapp.data.repository.ResultadoSincronizacion

/** Mensajes que dependen de una cantidad (singular o plural). */
object Textos {

    /**
     * Resultado del botón Sincronizar. [enCola] son las que siguen pendientes después del intento
     * (sin internet el envío falla y no se envía ninguna).
     */
    fun resultadoSincronizacion(r: ResultadoSincronizacion, enCola: Int, enLinea: Boolean = true): String {
        val propias = (r.enviadas - r.recibidas).coerceAtLeast(0)
        val mensaje = when {
            r.recibidas > 0 -> listOfNotNull(
                if (r.recibidas == 1) "1 muestra actualizada desde el servidor" else "${r.recibidas} muestras actualizadas desde el servidor",
                "$propias enviadas".takeIf { propias > 1 } ?: "1 enviada".takeIf { propias == 1 },
                "$enCola en cola".takeIf { enCola > 0 },
            ).joinToString(", ")
            r.enviadas == 0 && enCola == 0 && !enLinea -> "Sin conexión. No hay muestras pendientes de envío"
            r.enviadas == 0 && enCola == 0 -> "Todo al día: no hay muestras pendientes"
            r.enviadas == 0 -> "No se pudo enviar: ${muestras(enCola)} en cola. Se ${if (enCola == 1) "enviará sola" else "enviarán solas"} al volver internet"
            enCola == 0 -> sincronizadas(r.enviadas)
            else -> "${sincronizadas(r.enviadas)}, $enCola en cola"
        }
        return if (r.backendSinRespuesta) "$mensaje. El backend no responde" else mensaje
    }

    /** Texto junto a la luz de la barra superior: verde con internet y naranja sin internet. */
    fun estadoConexion(enLinea: Boolean, enCola: Int): String = when {
        enLinea && enCola == 0 -> "EN LÍNEA"
        enLinea -> "EN LÍNEA · $enCola EN COLA"
        enCola == 0 -> "SIN CONEXIÓN"
        else -> "$enCola EN COLA DE ENVÍO"
    }

    private fun muestras(n: Int) = if (n == 1) "1 muestra" else "$n muestras"

    private fun sincronizadas(n: Int) = if (n == 1) "1 muestra sincronizada" else "$n muestras sincronizadas"
}
