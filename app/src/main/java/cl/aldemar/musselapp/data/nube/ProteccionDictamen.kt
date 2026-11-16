package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.EstadoMuestra

/**
 * Regla para que un reintento de subida no pise lo que el supervisor ya escribió en la nube.
 *
 * Caso: la muestra se subió, pero el teléfono no alcanzó a recibir la confirmación (se cortó
 * internet o venció el tiempo de espera) y la dejó como pendiente. Mientras tanto el supervisor
 * la dictaminó desde el portal. Si el reintento escribiera `estado = PENDIENTE` de nuevo,
 * borraría el dictamen. Esta regla deja fuera esos campos: el dictamen llega después por el listener.
 *
 * Es una función pura, sin Firestore, para poder probarla en la JVM.
 */
object ProteccionDictamen {

    /**
     * Campos que escribe el supervisor desde el portal. Además del dictamen, el portal permite
     * corregir el conteo, la talla y las cholgas, y guarda los valores originales en `valoresOperario`.
     */
    val CAMPOS_DEL_SUPERVISOR: Set<String> = setOf(
        "estado", "revisadaPor", "revisadaEn", "comentarioSupervisor",
        "conteoIndM", "tallaMm", "cholgasIndM", "valoresOperario",
    )

    /** true si en la nube la muestra ya tiene un dictamen (cualquier estado distinto de PENDIENTE). */
    fun yaDictaminada(estadoRemoto: String?): Boolean =
        estadoRemoto != null && estadoRemoto != EstadoMuestra.PENDIENTE.name

    /**
     * Datos que el teléfono puede escribir. Si la muestra local sigue PENDIENTE pero en la nube ya
     * tiene dictamen, se quitan los campos del supervisor; en cualquier otro caso se escriben todos.
     */
    fun filtrar(datos: Map<String, Any?>, estadoLocal: EstadoMuestra, estadoRemoto: String?): Map<String, Any?> =
        if (estadoLocal == EstadoMuestra.PENDIENTE && yaDictaminada(estadoRemoto)) datos - CAMPOS_DEL_SUPERVISOR
        else datos
}
