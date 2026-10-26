package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.EstadoMuestra

/** Dictamen del supervisor que el operario aún no ha visto en pantalla. */
data class AvisoRevision(
    val codigo: String,
    val linea: String,
    val estado: EstadoMuestra,
    val comentario: String?,
)
