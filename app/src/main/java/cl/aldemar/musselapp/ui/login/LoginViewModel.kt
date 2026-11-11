package cl.aldemar.musselapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.aldemar.musselapp.MusselApplication
import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.repository.AuthRepository
import cl.aldemar.musselapp.util.RutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = MusselApplication.contenedor.authRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Paso 1: elige el perfil y avanza al ingreso de credenciales. */
    fun onRolChange(rol: Rol) = _uiState.update { it.copy(rol = rol, mensajeError = null) }

    /** Vuelve al paso 1 descartando el PIN y los errores. */
    fun onCambiarPerfil() = _uiState.update {
        it.copy(rol = null, password = "", passwordVisible = false, rutError = null, passwordError = null, mensajeError = null)
    }

    /**
     * Guarda solo dígitos y K (la pantalla agrega puntos y guion al mostrarlo).
     * La K solo puede ir al final, como dígito verificador.
     */
    fun onRutChange(rut: String) {
        val limpio = RutValidator.normalizar(rut).take(LARGO_MAXIMO_RUT)
        if ('K' in limpio.dropLast(1)) return
        _uiState.update { it.copy(rut = limpio, rutError = null, mensajeError = null) }
    }

    /** El PIN es numérico: se descartan otros caracteres y se limita su largo. */
    fun onPasswordChange(password: String) {
        val pin = password.filter(Char::isDigit).take(LARGO_MAXIMO_PIN)
        _uiState.update { it.copy(password = pin, passwordError = null, mensajeError = null) }
    }

    fun onTogglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onCentroChange(centro: CentroCultivo) = _uiState.update { it.copy(centro = centro) }

    fun onMensajeMostrado() = _uiState.update { it.copy(mensajeError = null) }

    fun onCerrarSesion() = _uiState.update { LoginUiState(centro = it.centro) }

    fun ingresar() {
        val estado = _uiState.value
        val rol = estado.rol ?: return
        if (estado.cargando) return

        val rutError = when {
            estado.rut.isBlank() -> "Ingrese su RUT"
            !RutValidator.esValido(estado.rut) -> "RUT inválido"
            else -> null
        }
        val passwordError = if (estado.password.isBlank()) "Ingrese su PIN" else null
        if (rutError != null || passwordError != null) {
            _uiState.update { it.copy(rutError = rutError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(cargando = true, mensajeError = null) }
        viewModelScope.launch {
            authRepository.login(estado.rut, estado.password, rol)
                .onSuccess { usuario ->
                    _uiState.update { it.copy(cargando = false, usuario = usuario, password = "") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(cargando = false, mensajeError = error.message ?: "No se pudo ingresar")
                    }
                }
        }
    }

    private companion object {
        /** 8 dígitos más el verificador: "111111111". */
        const val LARGO_MAXIMO_RUT = 9

        /** Largo máximo del PIN numérico. */
        const val LARGO_MAXIMO_PIN = 8
    }
}
