package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.Muestra

/**
 * Lo que el repositorio necesita de la nube. [NubeSync] lo implementa sobre Firestore;
 * las pruebas usan una implementación falsa para no depender de la red.
 */
interface ServicioNube {

    /**
     * Sube (o actualiza) la muestra, con su dictamen si se revisó en el teléfono.
     * Devuelve true si el servidor la confirmó. Devuelve false si no hubo conexión, y también si la
     * nube ya tenía un dictamen distinto: ese dictamen se entrega por [escucharVeredictos] y la muestra
     * queda pendiente hasta que el teléfono lo adopte.
     */
    suspend fun subir(m: Muestra): Boolean

    /**
     * Escucha en vivo los dictámenes del supervisor para las muestras de este teléfono.
     * [alEliminar] recibe el id local de cada muestra de este teléfono que el supervisor borró en el portal.
     */
    fun escucharVeredictos(alEliminar: (Long) -> Unit = {}, alAplicar: (VeredictoNube) -> Unit)
}
