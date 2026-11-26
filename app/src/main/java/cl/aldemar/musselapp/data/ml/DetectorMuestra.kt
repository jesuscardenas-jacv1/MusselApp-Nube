package cl.aldemar.musselapp.data.ml

import android.graphics.Bitmap

/** Cuenta los individuos de una foto. Hoy lo implementa [DetectorSimulado]; después, el modelo real. */
interface DetectorMuestra {
    suspend fun detectar(foto: Bitmap): ResultadoDeteccion
}
