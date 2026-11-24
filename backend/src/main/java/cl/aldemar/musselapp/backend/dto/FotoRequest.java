package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Metadatos y contenido de la fotografía de evidencia asociada a una muestra. */
@Schema(description = "Fotografía y metadatos asociados a una muestra")
public record FotoRequest(
        @NotBlank(message = "El nombre de archivo es obligatorio")
        @Size(max = 160)
        @Schema(description = "Nombre del archivo de evidencia", example = "muestra_018.jpg")
        String nombreArchivo,

        @Size(max = 60)
        @Schema(description = "Tipo MIME de la imagen", example = "image/jpeg")
        String tipoMime,

        @NotBlank(message = "El contenidoBase64 es obligatorio")
        @Size(max = 1_400_000, message = "La foto no puede superar 1 MB")
        @Schema(description = "Imagen codificada en Base64 (con o sin prefijo data:image/jpeg;base64,)")
        String contenidoBase64
) {
}
