package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.ui.theme.colores
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cada estado tiene su propio color: Validado verde, Pendiente ámbar y Corregido azul (igual que en el portal). */
class ColoresEstadoTest {

    @Test
    fun cadaEstado_tieneSuPropioColor() {
        val distintos = EstadoMuestra.entries.map { it.colores }.toSet()
        assertEquals(EstadoMuestra.entries.size, distintos.size)
    }

    @Test
    fun fondoTextoYAcento_tambienSonDistintosEntreEstados() {
        listOf<(EstadoMuestra) -> Any>({ it.colores.fondo }, { it.colores.texto }, { it.colores.acento }).forEach { parte ->
            assertEquals(EstadoMuestra.entries.size, EstadoMuestra.entries.map(parte).toSet().size)
        }
    }
}
