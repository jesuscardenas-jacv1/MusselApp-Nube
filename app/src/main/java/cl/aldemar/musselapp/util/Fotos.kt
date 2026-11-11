package cl.aldemar.musselapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

/**
 * Lectura de las fotos de muestras. Toda la app usa esta función para que la evidencia
 * se vea igual (y derecha) en Captura, Vista previa, Detalle, Revisión y la ficha PDF.
 *
 * Es trabajo de disco y CPU: llamar siempre fuera del hilo principal.
 */
object Fotos {

    /**
     * Decodifica la foto en [ruta] reducida para que su lado mayor quede cerca de [ladoMaxPx]
     * (la original de la cámara pesa varios MB) y la gira según la orientación EXIF.
     * Devuelve null si el archivo no existe o no es una imagen.
     */
    fun decodificar(ruta: String, ladoMaxPx: Int = 1280): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestreo(limites.outWidth, limites.outHeight, ladoMaxPx)
        }
        val bitmap = BitmapFactory.decodeFile(ruta, opciones) ?: return null
        val grados = gradosExif(ruta)
        if (grados == 0f) return bitmap
        return girar(bitmap, grados)
    }

    /** Gira [bitmap] en [grados] (múltiplos de 90); devuelve el mismo si no hay giro. */
    fun girar(bitmap: Bitmap, grados: Float): Bitmap {
        if (grados % 360f == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    /** Mayor potencia de 2 que deja el lado mayor de la imagen en al menos [ladoMaxPx]. */
    internal fun muestreo(ancho: Int, alto: Int, ladoMaxPx: Int): Int {
        var muestreo = 1
        while (maxOf(ancho, alto) / (muestreo * 2) >= ladoMaxPx) muestreo *= 2
        return muestreo
    }

    private fun gradosExif(ruta: String): Float = try {
        when (
            ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        0f // Sin metadatos legibles: se muestra tal como viene
    }
}
