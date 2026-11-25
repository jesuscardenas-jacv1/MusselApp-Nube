package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.EstadoMuestra

/**
 * Reglas para que el teléfono y el portal no se pisen los dictámenes. Vale el primero que llegó a la nube.
 *
 * Caso 1 (reintento): la muestra se subió, pero el teléfono no alcanzó a recibir la confirmación
 * (se cortó internet o venció el tiempo de espera) y la dejó como pendiente. Mientras tanto el
 * supervisor la dictaminó desde el portal. Si el reintento escribiera `estado = PENDIENTE` de nuevo,
 * borraría el dictamen.
 *
 * Caso 2 (dos dictámenes): el supervisor revisó la muestra en el teléfono sin internet y, antes de que
 * ese dictamen se subiera, la muestra también se dictaminó en el portal. El del portal llegó primero
 * a la nube: el teléfono no lo pisa y lo adopta.
 *
 * Son funciones puras, sin Firestore, para poder probarlas en la JVM.
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

    /**
     * Resultado del conteo automático (fase final). Una vez dictaminada la muestra, queda fijo: lo que vio el
     * supervisor no puede cambiar por detrás (las reglas de Firestore también lo impiden, ver firestore.rules).
     */
    val CAMPOS_DEL_MODELO: Set<String> = setOf("ml", "detecciones")

    /** true si en la nube la muestra ya tiene un dictamen (cualquier estado distinto de PENDIENTE). */
    fun yaDictaminada(estadoRemoto: String?): Boolean =
        estadoRemoto != null && estadoRemoto != EstadoMuestra.PENDIENTE.name

    /**
     * Datos que el teléfono puede escribir. Si en la nube la muestra ya tiene dictamen, se quitan los
     * campos del supervisor (el primer dictamen no se pisa) y el resultado del conteo automático; si no, se escriben todos, incluido el
     * dictamen que se haya hecho en el teléfono.
     */
    fun filtrar(datos: Map<String, Any?>, estadoRemoto: String?): Map<String, Any?> =
        if (yaDictaminada(estadoRemoto)) datos - CAMPOS_DEL_SUPERVISOR - CAMPOS_DEL_MODELO else datos

    /**
     * true si el teléfono quiere subir un dictamen y la nube ya tiene otro distinto. En ese caso el de la
     * nube llegó primero y es el que vale: el teléfono debe adoptarlo en vez de dar la subida por hecha.
     */
    fun prevaleceRemoto(estadoLocal: EstadoMuestra, estadoRemoto: String?): Boolean =
        estadoLocal != EstadoMuestra.PENDIENTE && yaDictaminada(estadoRemoto) && estadoRemoto != estadoLocal.name

    /**
     * Qué hace el teléfono con un dictamen que llega de la nube:
     * - si la muestra local sigue pendiente, lo aplica;
     * - si ya tiene un dictamen propio que todavía no se subió y el de la nube es distinto, vale el de la nube;
     * - si el dictamen local ya está sincronizado, se ignora (es el mismo, o uno repetido).
     */
    fun debeAplicarRemoto(estadoLocal: EstadoMuestra, sincronizadaLocal: Boolean, estadoRemoto: EstadoMuestra): Boolean =
        when {
            estadoRemoto == EstadoMuestra.PENDIENTE -> false
            estadoLocal == EstadoMuestra.PENDIENTE -> true
            else -> !sincronizadaLocal && estadoLocal != estadoRemoto
        }
}
