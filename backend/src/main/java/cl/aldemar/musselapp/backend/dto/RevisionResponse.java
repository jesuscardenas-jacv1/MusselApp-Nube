package cl.aldemar.musselapp.backend.dto;

import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Revision;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registro histórico de una revisión de supervisor")
public record RevisionResponse(
        Long id,
        Long muestraId,
        String supervisorNombre,
        String supervisorRut,
        EstadoMuestra estadoAsignado,
        String comentario,
        long revisadaEn
) {
    public static RevisionResponse desde(Revision r) {
        return new RevisionResponse(
                r.getId(),
                r.getMuestra().getId(),
                r.getSupervisorNombre(),
                r.getSupervisorRut(),
                r.getEstadoAsignado(),
                r.getComentario(),
                r.getRevisadaEn()
        );
    }
}
