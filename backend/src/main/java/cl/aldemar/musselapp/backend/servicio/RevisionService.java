package cl.aldemar.musselapp.backend.servicio;

import cl.aldemar.musselapp.backend.dto.MuestraResponse;
import cl.aldemar.musselapp.backend.dto.RevisionRequest;
import cl.aldemar.musselapp.backend.dto.RevisionResponse;
import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import cl.aldemar.musselapp.backend.entidad.Revision;
import cl.aldemar.musselapp.backend.repositorio.MuestraRepository;
import cl.aldemar.musselapp.backend.repositorio.RevisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Gestión de dictámenes y trazabilidad de revisiones del supervisor. */
@Service
public class RevisionService {

    private static final int MIN_COMENTARIO_CORREGIR = 5;

    private final RevisionRepository revisionRepository;
    private final MuestraRepository muestraRepository;
    private final MuestraService muestraService;

    public RevisionService(
            RevisionRepository revisionRepository,
            MuestraRepository muestraRepository,
            MuestraService muestraService
    ) {
        this.revisionRepository = revisionRepository;
        this.muestraRepository = muestraRepository;
        this.muestraService = muestraService;
    }

    public List<RevisionResponse> listarPorMuestra(Long muestraId) {
        muestraService.buscarEntidad(muestraId);
        return revisionRepository.findByMuestraIdOrderByRevisadaEnDescIdDesc(muestraId)
                .stream()
                .map(RevisionResponse::desde)
                .toList();
    }

    @Transactional
    public MuestraResponse registrarDictamen(Long muestraId, RevisionRequest req) {
        if (req.estado() == EstadoMuestra.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El dictamen debe asignar estado VALIDADO o CORREGIDO"
            );
        }
        String comentario = req.comentario() == null ? "" : req.comentario().trim();
        if (req.estado() == EstadoMuestra.CORREGIDO && comentario.length() < MIN_COMENTARIO_CORREGIR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Al corregir una muestra el comentario técnico debe tener al menos " + MIN_COMENTARIO_CORREGIR + " caracteres"
            );
        }

        boolean corrigeValores = req.conteoIndM() != null || req.tallaMm() != null || req.cholgasIndM() != null;
        if (corrigeValores && req.estado() != EstadoMuestra.CORREGIDO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los valores solo se cambian con estado CORREGIDO");
        }

        Muestra muestra = muestraService.buscarEntidad(muestraId);
        muestraService.exigirPendiente(muestra, "dictaminar");
        long ahora = System.currentTimeMillis();
        String supervisorNombre = req.supervisorNombre() != null && !req.supervisorNombre().isBlank()
                ? req.supervisorNombre().trim()
                : "Diego Muñoz";
        String supervisorRut = req.supervisorRut() != null && !req.supervisorRut().isBlank()
                ? req.supervisorRut().trim()
                : "222222222";

        Revision revision = new Revision();
        revision.setMuestra(muestra);
        revision.setSupervisorNombre(supervisorNombre);
        revision.setSupervisorRut(supervisorRut);
        revision.setEstadoAsignado(req.estado());
        revision.setComentario(comentario.isEmpty() ? null : comentario);
        revision.setRevisadaEn(ahora);
        revisionRepository.save(revision);

        if (corrigeValores) {
            muestra.setConteoOperario(muestra.getConteoIndM());
            muestra.setTallaOperario(muestra.getTallaMm());
            muestra.setCholgasOperario(muestra.getCholgasIndM());
            if (req.conteoIndM() != null) muestra.setConteoIndM(req.conteoIndM());
            if (req.tallaMm() != null) muestra.setTallaMm(req.tallaMm());
            if (req.cholgasIndM() != null) muestra.setCholgasIndM(req.cholgasIndM());
        }
        muestra.setEstado(req.estado());
        muestra.setRevisadaPor(supervisorNombre);
        muestra.setRevisadaEn(ahora);
        muestra.setComentarioSupervisor(comentario.isEmpty() ? null : comentario);
        muestra.tocar();

        return MuestraResponse.desde(muestraRepository.save(muestra));
    }
}
