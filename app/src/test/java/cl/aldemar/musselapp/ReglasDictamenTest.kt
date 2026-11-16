package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.ui.revision.ReglasDictamen
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** El comentario al corregir sigue la misma regla que el portal web: mínimo 5 caracteres. */
class ReglasDictamenTest {

    @Test
    fun corregirSinComentario_daError() {
        assertNotNull(ReglasDictamen.errorComentario(EstadoMuestra.CORREGIDO, ""))
    }

    @Test
    fun corregirConMenosDeCincoCaracteres_daError() {
        assertNotNull(ReglasDictamen.errorComentario(EstadoMuestra.CORREGIDO, "tall"))
    }

    @Test
    fun corregirConEspaciosAlrededor_noLosCuenta() {
        assertNotNull(ReglasDictamen.errorComentario(EstadoMuestra.CORREGIDO, "  ok   "))
    }

    @Test
    fun corregirConCincoCaracteres_esValido() {
        assertNull(ReglasDictamen.errorComentario(EstadoMuestra.CORREGIDO, "talla"))
    }

    @Test
    fun validarSinComentario_esValido() {
        assertNull(ReglasDictamen.errorComentario(EstadoMuestra.VALIDADO, ""))
    }

    @Test
    fun sinDictamenElegido_noMarcaErrorDeComentario() {
        assertNull(ReglasDictamen.errorComentario(null, ""))
    }
}
