package cl.aldemar.musselapp.backend.dto;

import cl.aldemar.musselapp.backend.entidad.FotoMuestra;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con fotografía y metadatos de una muestra")
public record FotoResponse(
        Long id,
        Long muestraId,
        String nombreArchivo,
        String tipoMime,
        long tamanoBytes,
        long registradaEn,
        String contenidoBase64
) {
    public static FotoResponse desde(FotoMuestra f) {
        return new FotoResponse(
                f.getId(),
                f.getMuestraId(),
                f.getNombreArchivo(),
                f.getTipoMime(),
                f.getTamanoBytes(),
                f.getRegistradaEn(),
                f.getContenidoBase64()
        );
    }
}
