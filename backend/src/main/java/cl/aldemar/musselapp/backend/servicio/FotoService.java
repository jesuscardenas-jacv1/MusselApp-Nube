package cl.aldemar.musselapp.backend.servicio;

import cl.aldemar.musselapp.backend.dto.FotoRequest;
import cl.aldemar.musselapp.backend.dto.FotoResponse;
import cl.aldemar.musselapp.backend.entidad.FotoMuestra;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import cl.aldemar.musselapp.backend.repositorio.FotoMuestraRepository;
import cl.aldemar.musselapp.backend.repositorio.MuestraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/** Gestión de fotografías de evidencia y sus metadatos por muestra. */
@Service
public class FotoService {

    private final FotoMuestraRepository fotoRepository;
    private final MuestraRepository muestraRepository;
    private final MuestraService muestraService;

    public FotoService(
            FotoMuestraRepository fotoRepository,
            MuestraRepository muestraRepository,
            MuestraService muestraService
    ) {
        this.fotoRepository = fotoRepository;
        this.muestraRepository = muestraRepository;
        this.muestraService = muestraService;
    }

    public FotoResponse obtenerPorMuestra(Long muestraId) {
        muestraService.buscarEntidad(muestraId);
        FotoMuestra foto = fotoRepository.findByMuestraId(muestraId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La muestra " + muestraId + " no tiene fotografía asociada"
                ));
        return FotoResponse.desde(foto);
    }

    @Transactional
    public FotoResponse guardar(Long muestraId, FotoRequest req) {
        Muestra muestra = muestraService.buscarEntidad(muestraId);
        FotoMuestra foto = fotoRepository.findByMuestraId(muestraId).orElseGet(FotoMuestra::new);
        foto.setMuestraId(muestraId);
        foto.setNombreArchivo(req.nombreArchivo().trim());
        foto.setTipoMime(req.tipoMime() != null && !req.tipoMime().isBlank() ? req.tipoMime().trim() : "image/jpeg");
        foto.setContenidoBase64(req.contenidoBase64());
        foto.setTamanoBytes(req.contenidoBase64().getBytes(StandardCharsets.UTF_8).length);
        foto.setRegistradaEn(System.currentTimeMillis());

        FotoMuestra guardada = fotoRepository.save(foto);
        if (!muestra.isTieneFoto()) {
            muestra.setTieneFoto(true);
            muestra.tocar();
            muestraRepository.save(muestra);
        }
        return FotoResponse.desde(guardada);
    }

    @Transactional
    public void eliminar(Long muestraId) {
        Muestra muestra = muestraService.buscarEntidad(muestraId);
        fotoRepository.deleteByMuestraId(muestraId);
        muestra.setTieneFoto(false);
        muestra.tocar();
        muestraRepository.save(muestra);
    }
}
