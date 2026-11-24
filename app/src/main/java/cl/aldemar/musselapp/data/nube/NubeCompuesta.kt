package cl.aldemar.musselapp.data.nube

import cl.aldemar.musselapp.data.model.Muestra

/**
 * Sincronización en modo dual: combina el backend propio Spring Boot ([primaria], vía Retrofit)
 * con Firebase Firestore ([secundaria], que alimenta el portal web del supervisor en vivo).
 *
 * Ambas se intentan siempre en cada subida para no perder compatibilidad con el portal web ni con Swagger.
 * Si cualquiera falla (por ejemplo al apagar el servidor Spring Boot o al activar modo avión),
 * [subir] devuelve false para que la muestra quede pendiente en Room y WorkManager la reintente.
 */
class NubeCompuesta(
    private val primaria: ServicioNube,
    private val secundaria: ServicioNube,
) : ServicioNube {

    override suspend fun subir(m: Muestra): Boolean {
        val okPrimaria = primaria.subir(m)
        val okSecundaria = secundaria.subir(m)
        return okPrimaria && okSecundaria
    }

    override fun escucharVeredictos(alEliminar: (Long) -> Unit, alAplicar: (VeredictoNube) -> Unit) {
        primaria.escucharVeredictos(alEliminar, alAplicar)
        secundaria.escucharVeredictos(alEliminar, alAplicar)
    }

    override suspend fun descargarCambios(): List<CambioRemoto> =
        primaria.descargarCambios() + secundaria.descargarCambios()

    override fun registrarIdLocal(idRemoto: String, idLocal: Long) {
        primaria.registrarIdLocal(idRemoto, idLocal)
        secundaria.registrarIdLocal(idRemoto, idLocal)
    }
}
