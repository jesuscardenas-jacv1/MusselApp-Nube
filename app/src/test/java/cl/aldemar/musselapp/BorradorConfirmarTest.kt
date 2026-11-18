package cl.aldemar.musselapp

import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.data.model.Posicion
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario
import cl.aldemar.musselapp.data.repository.MuestraRepository
import cl.aldemar.musselapp.ui.muestra.BorradorMuestra
import cl.aldemar.musselapp.ui.muestra.BorradorMuestraViewModel
import cl.aldemar.musselapp.ui.muestra.ParametrosMuestra
import cl.aldemar.musselapp.ui.muestra.validarBorrador
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Guardar la muestra desde la Vista previa: validaciones, posición del GPS y errores de la base. */
@OptIn(ExperimentalCoroutinesApi::class)
class BorradorConfirmarTest {

    @get:Rule
    val main = ReglaDispatcherPrincipal()

    private val operario = Usuario("111111111", "Carla Vera", Rol.OPERARIO)
    private val repo = MuestrasEnMemoria()
    private val borradas = mutableListOf<String>()

    private fun vm(posicion: Posicion? = null) = BorradorMuestraViewModel(
        repository = repo,
        borrarFoto = { borradas += it },
        ubicador = { posicion },
    )

    private fun BorradorMuestraViewModel.completar() {
        guardarParametros(ParametrosMuestra("dalcahue", "B", 4, 3))
        guardarFoto("/fotos/muestra.jpg")
        actualizarConteo(420)
        actualizarTalla(65)
        actualizarCholgas(0)
        actualizarObservacion("  Buena fijación  ")
    }

    @Test
    fun validarBorrador_revisaCadaCampoEnOrden() {
        val completo = BorradorMuestra(ParametrosMuestra("dalcahue", "B", 4, 3), "/f.jpg", 420, 65, 0)
        assertNull(validarBorrador(completo))
        assertEquals("El conteo debe estar entre 1 y 5000 ind/m", validarBorrador(completo.copy(conteoIndM = 0)))
        assertEquals("Ingresa la talla media en mm", validarBorrador(completo.copy(tallaMm = null)))
        assertEquals("La talla debe estar entre 1 y 200 mm", validarBorrador(completo.copy(tallaMm = 201)))
        assertEquals("Ingresa el conteo de cholgas (0 si no hay)", validarBorrador(completo.copy(cholgasIndM = null)))
        assertEquals("Las cholgas deben estar entre 0 y 5000 ind/m", validarBorrador(completo.copy(cholgasIndM = -1)))
    }

    @Test
    fun confirmarIncompleto_avisaElMotivoYNoGuarda() = runTest(main.dispatcher) {
        val vm = vm()
        var motivo: String? = null
        vm.confirmar(operario, alGuardar = {}, alFallar = { motivo = it })
        assertEquals("Faltan centro, tren, línea y estrato", motivo)
        assertTrue(repo.muestras.value.isEmpty())
    }

    @Test
    fun confirmarCompleto_guardaPendienteConPosicion_yLimpia() = runTest(main.dispatcher) {
        val gps = Posicion(-42.37, -73.62, 8.0)
        val vm = vm(gps)
        vm.completar()
        vm.buscarUbicacion()
        var guardada = false
        vm.confirmar(operario, alGuardar = { guardada = true }, alFallar = { error(it) })
        assertTrue(guardada)
        val m = repo.muestras.value.single()
        assertEquals(EstadoMuestra.PENDIENTE, m.estado)
        assertFalse(m.sincronizada)
        assertEquals("Buena fijación", m.observacion)
        assertEquals("Carla Vera", m.operador)
        assertEquals("111111111", m.operadorRut)
        assertEquals(gps, m.posicion)
        assertEquals("/fotos/muestra.jpg", m.fotoRuta)
        // El borrador queda vacío y la foto se conserva (ahora es de la muestra)
        assertEquals(BorradorMuestra(), vm.borrador.value)
        assertTrue(borradas.isEmpty())
        assertFalse(vm.guardando.value)
    }

    @Test
    fun sinGps_guardaIgualSinPosicion() = runTest(main.dispatcher) {
        val vm = vm(posicion = null)
        vm.completar()
        vm.buscarUbicacion()
        assertFalse(vm.ubicacion.value.buscando)
        vm.confirmar(operario, alGuardar = {}, alFallar = { error(it) })
        assertNull(repo.muestras.value.single().posicion)
    }

    @Test
    fun buscarUbicacion_noRepiteUnaBusquedaEnCurso() = runTest(main.dispatcher) {
        val respuesta = CompletableDeferred<Posicion?>()
        var lecturas = 0
        val vm = BorradorMuestraViewModel(repo, borrarFoto = {}, ubicador = { lecturas++; respuesta.await() })
        vm.buscarUbicacion()
        vm.buscarUbicacion()
        assertTrue(vm.ubicacion.value.buscando)
        assertEquals(1, lecturas)
        respuesta.complete(Posicion(-42.0, -73.0, 5.0))
        assertFalse(vm.ubicacion.value.buscando)
        assertEquals(-42.0, vm.ubicacion.value.posicion!!.lat, 0.0)
    }

    @Test
    fun errorDeLaBase_avisaYConservaElBorrador() = runTest(main.dispatcher) {
        val repoQueFalla = object : MuestraRepository by repo {
            override suspend fun agregarMuestra(muestra: Muestra) = error("disco lleno")
        }
        val vm = BorradorMuestraViewModel(repoQueFalla, borrarFoto = {}, ubicador = { null })
        vm.completar()
        var motivo: String? = null
        vm.confirmar(operario, alGuardar = { error("no debía guardar") }, alFallar = { motivo = it })
        assertEquals("No se pudo guardar la muestra: disco lleno", motivo)
        assertEquals(420, vm.borrador.value.conteoIndM)
        assertFalse(vm.guardando.value)
    }

    @Test
    fun actualizarParametros_cambiaSinPerderLoDemas() {
        val vm = vm()
        vm.completar()
        vm.actualizarParametros(ParametrosMuestra("calbuco", "A", 1, 5))
        assertEquals("calbuco", vm.borrador.value.parametros?.centroId)
        assertEquals(420, vm.borrador.value.conteoIndM)
    }
}
