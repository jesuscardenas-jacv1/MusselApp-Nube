package cl.aldemar.musselapp.backend.servicio;

import cl.aldemar.musselapp.backend.dto.MuestraRequest;
import cl.aldemar.musselapp.backend.dto.MuestraResponse;
import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import cl.aldemar.musselapp.backend.repositorio.FotoMuestraRepository;
import cl.aldemar.musselapp.backend.repositorio.LineaRepository;
import cl.aldemar.musselapp.backend.repositorio.MuestraRepository;
import cl.aldemar.musselapp.backend.repositorio.RevisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Lógica de negocio y persistencia de muestras.
 */
@Service
public class MuestraService {

    private final MuestraRepository muestraRepository;
    private final LineaRepository lineaRepository;
    private final FotoMuestraRepository fotoRepository;
    private final RevisionRepository revisionRepository;

    public MuestraService(
            MuestraRepository muestraRepository,
            LineaRepository lineaRepository,
            FotoMuestraRepository fotoRepository,
            RevisionRepository revisionRepository
    ) {
        this.muestraRepository = muestraRepository;
        this.lineaRepository = lineaRepository;
        this.fotoRepository = fotoRepository;
        this.revisionRepository = revisionRepository;
    }

    public List<MuestraResponse> listar(
            String centroId,
            String tren,
            Integer linea,
            EstadoMuestra estado,
            Long desde,
            Long hasta
    ) {
        return muestraRepository.findAllByOrderByRegistradaEnDesc().stream()
                .filter(m -> centroId == null || centroId.isBlank() || m.getCentroId().equalsIgnoreCase(centroId.trim()))
                .filter(m -> tren == null || tren.isBlank() || m.getTren().equalsIgnoreCase(tren.trim()))
                .filter(m -> linea == null || m.getLinea() == linea)
                .filter(m -> estado == null || m.getEstado() == estado)
                .filter(m -> desde == null || m.getRegistradaEn() >= desde)
                .filter(m -> hasta == null || m.getRegistradaEn() <= hasta)
                .map(MuestraResponse::desde)
                .toList();
    }

    public MuestraResponse obtenerPorId(Long id) {
        return MuestraResponse.desde(buscarEntidad(id));
    }

    /**
     * Crea una muestra nueva. Si trae {@code idCliente} y ya existe en la BD, actualiza ese mismo registro
     * sin crear duplicados (idempotencia para reintentos offline de WorkManager).
     */
    @Transactional
    public ResultadoCreacion crearOActualizarPorCliente(MuestraRequest req) {
        String centroId = req.centroId().trim().toLowerCase(Locale.ROOT);
        String tren = req.tren().trim().toUpperCase(Locale.ROOT);
        validarCatalogo(centroId, tren, req.linea());

        long ahora = System.currentTimeMillis();
        if (req.idCliente() != null && !req.idCliente().isBlank()) {
            var existente = muestraRepository.findByIdCliente(req.idCliente().trim());
            if (existente.isPresent()) {
                Muestra m = existente.get();
                aplicarCampos(m, req, centroId, tren, ahora);
                return new ResultadoCreacion(MuestraResponse.desde(muestraRepository.save(m)), false);
            }
        }

        Muestra nueva = new Muestra();
        if (req.idCliente() != null && !req.idCliente().isBlank()) {
            nueva.setIdCliente(req.idCliente().trim());
        }
        nueva.setEstado(EstadoMuestra.PENDIENTE);
        nueva.setRegistradaEn(req.registradaEn() != null ? req.registradaEn() : ahora);
        aplicarCampos(nueva, req, centroId, tren, ahora);

        if (nueva.getCodigo() == null || nueva.getCodigo().isBlank()) {
            nueva.setCodigo("Muestra 000");
        }
        Muestra guardada = muestraRepository.save(nueva);
        if (req.codigo() == null || req.codigo().isBlank()) {
            guardada.setCodigo(String.format("Muestra %03d", guardada.getId()));
            guardada = muestraRepository.save(guardada);
        }
        return new ResultadoCreacion(MuestraResponse.desde(guardada), true);
    }

    @Transactional
    public MuestraResponse actualizar(Long id, MuestraRequest req) {
        Muestra m = buscarEntidad(id);
        String centroId = req.centroId().trim().toLowerCase(Locale.ROOT);
        String tren = req.tren().trim().toUpperCase(Locale.ROOT);
        validarCatalogo(centroId, tren, req.linea());

        aplicarCampos(m, req, centroId, tren, System.currentTimeMillis());
        return MuestraResponse.desde(muestraRepository.save(m));
    }

    @Transactional
    public void eliminar(Long id) {
        Muestra m = buscarEntidad(id);
        revisionRepository.deleteByMuestraId(m.getId());
        fotoRepository.deleteByMuestraId(m.getId());
        muestraRepository.delete(m);
    }

    public Muestra buscarEntidad(Long id) {
        return muestraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la muestra con id " + id));
    }

    private void validarCatalogo(String centroId, String tren, int linea) {
        boolean existe = lineaRepository.existsByTrenCentroIdAndTrenCodigoIgnoreCaseAndNumero(centroId, tren, linea);
        if (!existe) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No existe la línea " + linea + " del tren " + tren + " en el centro '" + centroId + "'"
            );
        }
    }

    private void aplicarCampos(Muestra m, MuestraRequest req, String centroId, String tren, long ahora) {
        if (req.codigo() != null && !req.codigo().isBlank()) {
            m.setCodigo(req.codigo().trim());
        }
        m.setCentroId(centroId);
        m.setTren(tren);
        m.setLinea(req.linea());
        m.setProfundidadM(req.profundidadM());
        m.setTallaMm(req.tallaMm());
        m.setConteoIndM(req.conteoIndM());
        m.setCholgasIndM(req.cholgasIndM());
        m.setTramoCm(req.tramoCm());
        m.setObservacion(req.observacion() != null ? req.observacion().trim() : "");
        m.setOperador(req.operador() != null && !req.operador().isBlank() ? req.operador().trim() : "Carla Vera");
        m.setOperadorRut(req.operadorRut() != null && !req.operadorRut().isBlank() ? req.operadorRut().trim() : "111111111");
        m.setLat(req.lat());
        m.setLon(req.lon());
        m.setPrecisionM(req.precisionM());
        m.setDistanciaCentroM(req.distanciaCentroM());
        m.setClimaTempAireC(req.climaTempAireC());
        m.setClimaVientoKn(req.climaVientoKn());
        m.setClimaOlaM(req.climaOlaM());
        m.setClimaTempAguaC(req.climaTempAguaC());
        m.setClimaDescripcion(req.climaDescripcion());
        m.setActualizadaEn(ahora);
    }

    public record ResultadoCreacion(MuestraResponse muestra, boolean creada) {
    }
}
