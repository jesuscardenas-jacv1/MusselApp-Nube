package cl.aldemar.musselapp.ui.login

import cl.aldemar.musselapp.data.model.CentroCultivo
import cl.aldemar.musselapp.data.model.Usuario

/**
 * Estado completo de la pantalla de Login: credenciales y centro de la jornada.
 * El perfil (operario o supervisor) no se elige: viene del usuario que ingresa.
 */
data class LoginUiState(
    val rut: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val centro: CentroCultivo = CentroCultivo.disponibles.first(),
    val rutError: String? = null,
    val passwordError: String? = null,
    val cargando: Boolean = false,
    val mensajeError: String? = null,
    val usuario: Usuario? = null,
    /** La animación de bienvenida terminó: recién entonces se pasa a la pantalla principal. */
    val transicionTerminada: Boolean = false,
)
