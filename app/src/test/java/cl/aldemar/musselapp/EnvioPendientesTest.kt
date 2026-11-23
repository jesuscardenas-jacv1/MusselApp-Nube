package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.nube.EnvioPendientes
import cl.aldemar.musselapp.data.nube.ResultadoEnvio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Decisión del envío en segundo plano con WorkManager: terminar, reintentar o dejarlo. */
class EnvioPendientesTest {

    private fun conPendientes() = MuestrasEnMemoria(
        listOf(muestraDePrueba(1, sincronizada = false), muestraDePrueba(2, sincronizada = true)),
    )

    @Test
    fun conInternet_subeLasPendientesYTermina() = runTest {
        val repo = conPendientes()
        assertEquals(ResultadoEnvio.LISTO, EnvioPendientes.ejecutar(repo, intento = 0))
        assertEquals(1, repo.llamadasSincronizar)
        assertEquals(0, repo.contarPendientes())
    }

    @Test
    fun sinPendientes_terminaIgual() = runTest {
        val repo = MuestrasEnMemoria(listOf(muestraDePrueba(1, sincronizada = true)))
        assertEquals(ResultadoEnvio.LISTO, EnvioPendientes.ejecutar(repo, intento = 0))
    }

    @Test
    fun siFallaElEnvio_reintentaYAvisaElError() = runTest {
        val repo = conPendientes().apply { fallarAlSincronizar = true }
        val errores = mutableListOf<Exception>()
        assertEquals(ResultadoEnvio.REINTENTAR, EnvioPendientes.ejecutar(repo, intento = 0) { errores += it })
        assertEquals(1, errores.size)
        assertEquals(1, repo.contarPendientes()) // la muestra sigue pendiente, no se pierde
    }

    @Test
    fun alAgotarLosIntentos_loDeja() = runTest {
        val repo = conPendientes().apply { fallarAlSincronizar = true }
        val ultimo = EnvioPendientes.MAX_INTENTOS - 1
        assertEquals(ResultadoEnvio.REINTENTAR, EnvioPendientes.ejecutar(repo, intento = ultimo - 1))
        assertEquals(ResultadoEnvio.RENDIRSE, EnvioPendientes.ejecutar(repo, intento = ultimo))
    }

    @Test
    fun siWorkManagerLoDetiene_noLoTomaComoError() = runTest {
        val repo = object : cl.aldemar.musselapp.data.repository.MuestraRepository by conPendientes() {
            override suspend fun sincronizar(): Int = throw CancellationException("se cortó internet")
        }
        var avisos = 0
        try {
            EnvioPendientes.ejecutar(repo, intento = 0) { avisos++ }
            fail("La cancelación debe llegar a WorkManager")
        } catch (e: CancellationException) {
            assertTrue(e.message!!.contains("internet"))
        }
        assertEquals(0, avisos)
    }
}
