package cl.aldemar.musselapp.backend.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

/**
 * Validación de permisos por rol en el servidor (cabecera {@code X-Rol}).
 * Roles: OPERARIO y SUPERVISOR. El rol lo declara quien llama: es una simplificación del proyecto
 * académico, no reemplaza un inicio de sesión con token.
 */
public final class PermisosRol {

    public static final String OPERARIO = "OPERARIO";
    public static final String SUPERVISOR = "SUPERVISOR";

    private static final Set<String> ROLES_VALIDOS = Set.of(OPERARIO, SUPERVISOR);

    private PermisosRol() {
    }

    /** Verifica que el rol enviado en la cabecera esté dentro de los permitidos para la operación. */
    public static void exigir(String rolHeader, String accion, String... rolesPermitidos) {
        if (rolHeader == null || rolHeader.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Falta la cabecera X-Rol para " + accion + " (permitidos: " + String.join(", ", rolesPermitidos) + ")"
            );
        }
        String rol = rolHeader.trim().toUpperCase(Locale.ROOT);
        if (!ROLES_VALIDOS.contains(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol no reconocido: '" + rolHeader + "'. Usa OPERARIO o SUPERVISOR"
            );
        }
        for (String permitido : rolesPermitidos) {
            if (permitido.equals(rol)) {
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El rol " + rol + " no tiene permiso para " + accion + " (requiere: " + String.join(" o ", rolesPermitidos) + ")"
        );
    }
}
