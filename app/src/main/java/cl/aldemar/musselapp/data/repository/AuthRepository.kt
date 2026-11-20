package cl.aldemar.musselapp.data.repository

import cl.aldemar.musselapp.data.model.Rol
import cl.aldemar.musselapp.data.model.Usuario

/** Acceso a la autenticación de usuarios. */
interface AuthRepository {
    /**
     * Valida RUT y PIN y devuelve el usuario con su rol. El perfil sale de la base:
     * si se indica [rol], además se exige que coincida.
     */
    suspend fun login(rut: String, password: String, rol: Rol? = null): Result<Usuario>
}

/** Error de autenticación con un mensaje listo para mostrar en pantalla. */
class AuthException(message: String) : Exception(message)
