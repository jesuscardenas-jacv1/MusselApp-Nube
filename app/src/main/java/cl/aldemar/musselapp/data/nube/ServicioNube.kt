package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.Muestra

/**
 * Lo que el repositorio necesita de la nube. [NubeSync] lo implementa sobre Firestore;
 * las pruebas usan una implementación falsa para no depender de la red.
 */
interface ServicioNube {

    /** Sube (o actualiza) la muestra. Devuelve true si el servidor la confirmó. */
    suspend fun subir(m: Muestra): Boolean

    /** Escucha en vivo los dictámenes del supervisor para las muestras de este teléfono. */
    fun escucharVeredictos(alAplicar: (VeredictoNube) -> Unit)
}
