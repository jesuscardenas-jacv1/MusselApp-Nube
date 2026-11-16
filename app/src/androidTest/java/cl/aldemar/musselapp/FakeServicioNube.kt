package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.nube.VeredictoNube

/**
 * Nube falsa para las pruebas: no usa red. Permite forzar que la subida falle,
 * registra las muestras subidas y emite dictámenes a mano como si vinieran del supervisor.
 */
class FakeServicioNube(
    /** Si es false, [subir] devuelve false y la muestra queda pendiente de sincronizar. */
    @Volatile var subidaOk: Boolean = true,
    /** Dictámenes que se emiten en cuanto el repositorio se suscribe (simula un snapshot inicial). */
    private val veredictosIniciales: List<VeredictoNube> = emptyList(),
) : ServicioNube {

    /** Ids de las muestras cuya subida se intentó, en orden. */
    val subidas = mutableListOf<Long>()

    private var alAplicar: ((VeredictoNube) -> Unit)? = null
    private var alEliminar: ((Long) -> Unit)? = null

    override suspend fun subir(m: Muestra): Boolean {
        synchronized(subidas) { subidas += m.id }
        return subidaOk
    }

    override fun escucharVeredictos(alEliminar: (Long) -> Unit, alAplicar: (VeredictoNube) -> Unit) {
        this.alEliminar = alEliminar
        this.alAplicar = alAplicar
        veredictosIniciales.forEach(alAplicar)
    }

    /** Simula un dictamen del supervisor llegado desde la nube. */
    fun emitir(veredicto: VeredictoNube) {
        val destino = alAplicar ?: error("El repositorio todavía no se suscribió a los veredictos")
        destino(veredicto)
    }

    /** Simula que el supervisor borró en el portal la muestra con este id local. */
    fun eliminar(muestraId: Long) {
        val destino = alEliminar ?: error("El repositorio todavía no se suscribió a la nube")
        destino(muestraId)
    }

    fun idsSubidos(): List<Long> = synchronized(subidas) { subidas.toList() }
}
