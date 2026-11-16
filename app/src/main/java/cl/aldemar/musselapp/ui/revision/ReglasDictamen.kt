package cl.aldemar.musselapp.ui.revision

import cl.aldemar.musselapp.data.model.EstadoMuestra

/**
 * Reglas del comentario del supervisor, iguales en la app y en el portal web
 * (`web-supervisor/index.html`): al corregir, el comentario debe tener al menos
 * [MIN_CARACTERES_CORRECCION] caracteres, sin contar los espacios de los extremos.
 */
object ReglasDictamen {

    const val MIN_CARACTERES_CORRECCION = 5

    /** Devuelve el mensaje de error del comentario, o null si el dictamen se puede guardar. */
    fun errorComentario(estado: EstadoMuestra?, comentario: String): String? =
        if (estado == EstadoMuestra.CORREGIDO && comentario.trim().length < MIN_CARACTERES_CORRECCION) {
            "Para corregir, escribe qué se debe corregir (mínimo $MIN_CARACTERES_CORRECCION caracteres)"
        } else {
            null
        }
}
