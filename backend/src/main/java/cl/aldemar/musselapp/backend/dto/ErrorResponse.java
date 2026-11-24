package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Formato estándar de respuesta de error")
public record ErrorResponse(
        int codigo,
        String error,
        String mensaje,
        long timestamp
) {
}
