package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Metadatos y contenido de la fotografía de evidencia asociada a una muestra. */
@Schema(description = "Fotografía y metadatos asociados a una muestra")
public record FotoRequest(
        @NotBlank(message = "El nombre de archivo es obligatorio")
        @Schema(description = "Nombre del archivo de evidencia", example = "muestra_018.jpg")
        String nombreArchivo,

        @Schema(description = "Tipo MIME de la imagen", example = "image/jpeg")
        String tipoMime,

        @NotBlank(message = "El contenidoBase64 es obligatorio")
        @Schema(description = "Imagen codificada en Base64 (con o sin prefijo data:image/jpeg;base64,)")
        String contenidoBase64
) {
}
