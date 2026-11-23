package cl.aldemar.musselapp.util

/** Mensajes que dependen de una cantidad (singular o plural). */
object Textos {

    /**
     * Resultado del botón Sincronizar: [enviadas] son las que llegaron a la nube y [enCola] las que
     * siguen pendientes después del intento (sin internet el envío falla y no se envía ninguna).
     */
    fun resultadoSincronizacion(enviadas: Int, enCola: Int): String = when {
        enviadas == 0 && enCola == 0 -> "Todo estaba sincronizado"
        enviadas == 0 -> "No se pudo enviar: ${muestras(enCola)} en cola. Se ${if (enCola == 1) "enviará sola" else "enviarán solas"} al volver internet"
        enCola == 0 -> sincronizadas(enviadas)
        else -> "${sincronizadas(enviadas)}, $enCola en cola"
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
