package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.nube.ProteccionDictamen
import cl.aldemar.musselapp.data.nube.ServicioNube
import cl.aldemar.musselapp.data.nube.VeredictoNube

/**
 * Nube falsa para las pruebas: no usa red. Permite forzar que la subida falle,
 * registra las muestras subidas y emite dictámenes a mano como si vinieran del supervisor.
 * También recuerda el dictamen que quedó "en la nube" para cada muestra y, como [NubeSync],
 * no deja que una subida pise un dictamen que llegó primero.
 */
class FakeServicioNube(
    /** Si es false, [subir] devuelve false y la muestra queda pendiente de sincronizar. */
    @Volatile var subidaOk: Boolean = true,
    /** Dictámenes que se emiten en cuanto el repositorio se suscribe (simula un snapshot inicial). */
    private val veredictosIniciales: List<VeredictoNube> = emptyList(),
) : ServicioNube {

    /** Ids de las muestras cuya subida se intentó, en orden. */
    val subidas = mutableListOf<Long>()

    /** Dictamen que tiene cada muestra en la nube, por id local. */
    private val dictamenes = mutableMapOf<Long, VeredictoNube>()

    private var alAplicar: ((VeredictoNube) -> Unit)? = null
    private var alEliminar: ((Long) -> Unit)? = null

    override suspend fun subir(m: Muestra): Boolean {
        synchronized(subidas) { subidas += m.id }
        if (!subidaOk) return false
        val previo = synchronized(dictamenes) {
            val enLaNube = dictamenes[m.id]
            when {
                // Gana el primero: la nube ya tiene otro dictamen y no se pisa
                ProteccionDictamen.prevaleceRemoto(m.estado, enLaNube?.estado?.name) -> enLaNube
                else -> {
                    if (enLaNube == null && m.estado != EstadoMuestra.PENDIENTE) {
                        dictamenes[m.id] = VeredictoNube(m.id, m.estado, m.comentarioSupervisor, m.revisadaEn ?: 0L)
                    }
                    null
                }
            }
        }
        if (previo != null) {
            alAplicar?.invoke(previo)
            return false
        }
        return true
    }

    /** Simula que el supervisor dictaminó en el portal sin que este teléfono se enterara (estaba sin internet). */
    fun dictaminarEnPortal(veredicto: VeredictoNube) {
        synchronized(dictamenes) { dictamenes[veredicto.muestraId] = veredicto }
    }

    /** Dictamen que quedó en la nube para la muestra, o null si sigue pendiente. */
    fun dictamenEnLaNube(muestraId: Long): VeredictoNube? = synchronized(dictamenes) { dictamenes[muestraId] }

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
