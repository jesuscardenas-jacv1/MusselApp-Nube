package cl.aldemar.musselapp.backend.dto;

import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dictamen técnico emitido por el supervisor sobre una muestra. */
@Schema(description = "Dictamen del supervisor: VALIDADO o CORREGIDO (con comentario obligatorio al corregir)")
public record RevisionRequest(
        @NotNull(message = "El estado es obligatorio (VALIDADO o CORREGIDO)")
        @Schema(description = "Nuevo estado asignado", example = "VALIDADO")
        EstadoMuestra estado,

        @Size(max = 500, message = "El comentario admite hasta 500 caracteres")
        @Schema(description = "Comentario técnico (obligatorio con al menos 5 caracteres al corregir)", example = "Conteo y talla dentro de estándar")
        String comentario,

        @Schema(description = "Nombre del supervisor ficticio", example = "Diego Muñoz")
        String supervisorNombre,

        @Schema(description = "RUT normalizado del supervisor ficticio", example = "222222222")
        String supervisorRut
) {
}
