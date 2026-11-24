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
import java.util.UUID;

/**
 * Lógica de negocio y persistencia de muestras.
 */
@Service
public class MuestraService {

    /** Prefijo del idCliente de las muestras creadas en el servidor (por ejemplo desde Swagger). */
    public static final String PREFIJO_API = "api_";

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
            String idCliente,
            String centroId,
            String tren,
            Integer linea,
            EstadoMuestra estado,
            Long desde,
            Long hasta
    ) {
        return muestraRepository.findAllByOrderByRegistradaEnDesc().stream()
                .filter(m -> idCliente == null || idCliente.isBlank() || idCliente.trim().equals(m.getIdCliente()))
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
     * Crea una muestra. Si trae un {@code idCliente} que ya existe es un reintento del teléfono: no se
     * duplica ni se pisan los valores guardados (podrían haberse corregido en el servidor); solo se
     * completan el GPS y el clima que faltaban.
     */
    @Transactional
    public ResultadoCreacion crear(MuestraRequest req) {
        String centroId = req.centroId().trim().toLowerCase(Locale.ROOT);
        String tren = req.tren().trim().toUpperCase(Locale.ROOT);
        validarCatalogo(centroId, tren, req.linea());

        String idCliente = texto(req.idCliente());
        if (idCliente != null) {
            var existente = muestraRepository.findByIdCliente(idCliente);
            if (existente.isPresent()) {
                Muestra m = existente.get();
                if (completarFaltantes(m, req)) {
                    m.tocar();
                }
                return new ResultadoCreacion(MuestraResponse.desde(muestraRepository.save(m)), false);
            }
        }
        if (texto(req.operador()) == null || texto(req.operadorRut()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Al crear una muestra se exigen operador y operadorRut");
        }

        Muestra nueva = new Muestra();
        // Identificador único en cualquier computador: el teléfono se guía por él y no por el id de la tabla
        nueva.setIdCliente(idCliente != null ? idCliente : PREFIJO_API + UUID.randomUUID());
        nueva.setEstado(EstadoMuestra.PENDIENTE);
        nueva.setRegistradaEn(req.registradaEn() != null ? req.registradaEn() : System.currentTimeMillis());
        nueva.setCodigo("Muestra 000");
        aplicarCampos(nueva, req, centroId, tren);
        completarFaltantes(nueva, req);
        nueva.tocar();
        Muestra guardada = muestraRepository.save(nueva);
        if (texto(req.codigo()) == null) {
            guardada.setCodigo(String.format("Muestra %03d", guardada.getId()));
            guardada = muestraRepository.save(guardada);
        }
        return new ResultadoCreacion(MuestraResponse.desde(guardada), true);
    }

    /** Edita una muestra pendiente. Los campos opcionales que no vienen se conservan. */
    @Transactional
    public MuestraResponse actualizar(Long id, MuestraRequest req) {
        Muestra m = buscarEntidad(id);
        exigirPendiente(m, "modificar");
        String centroId = req.centroId().trim().toLowerCase(Locale.ROOT);
        String tren = req.tren().trim().toUpperCase(Locale.ROOT);
        validarCatalogo(centroId, tren, req.linea());
        aplicarCampos(m, req, centroId, tren);
        m.tocar();
        return MuestraResponse.desde(muestraRepository.save(m));
    }

    /** Gana el primer dictamen: una muestra ya revisada no se cambia. */
    void exigirPendiente(Muestra m, String accion) {
        if (m.yaDictaminada()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede " + accion + ": la muestra " + m.getId() + " ya tiene dictamen (" + m.getEstado() + ")"
            );
        }
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

    private void aplicarCampos(Muestra m, MuestraRequest req, String centroId, String tren) {
        m.setCentroId(centroId);
        m.setTren(tren);
        m.setLinea(req.linea());
        m.setProfundidadM(req.profundidadM());
        m.setTallaMm(req.tallaMm());
        m.setConteoIndM(req.conteoIndM());
        if (texto(req.codigo()) != null) m.setCodigo(texto(req.codigo()));
        if (req.cholgasIndM() != null) m.setCholgasIndM(req.cholgasIndM());
        if (req.tramoCm() != null) m.setTramoCm(req.tramoCm());
        if (req.observacion() != null) m.setObservacion(req.observacion().trim());
        if (texto(req.operador()) != null) m.setOperador(texto(req.operador()));
        if (texto(req.operadorRut()) != null) m.setOperadorRut(texto(req.operadorRut()));
    }

    /** Agrega el GPS y el clima solo si la muestra no los tenía. Devuelve true si cambió algo. */
    private boolean completarFaltantes(Muestra m, MuestraRequest req) {
        boolean cambio = false;
        if (m.getLat() == null && req.lat() != null && req.lon() != null) {
            m.setLat(req.lat());
            m.setLon(req.lon());
            m.setPrecisionM(req.precisionM());
            m.setDistanciaCentroM(req.distanciaCentroM());
            cambio = true;
        }
        boolean traeClima = req.climaTempAireC() != null || req.climaVientoKn() != null || req.climaOlaM() != null
                || req.climaTempAguaC() != null || req.climaDescripcion() != null;
        boolean tieneClima = m.getClimaTempAireC() != null || m.getClimaVientoKn() != null || m.getClimaOlaM() != null
                || m.getClimaTempAguaC() != null || m.getClimaDescripcion() != null;
        if (traeClima && !tieneClima) {
            m.setClimaTempAireC(req.climaTempAireC());
            m.setClimaVientoKn(req.climaVientoKn());
            m.setClimaOlaM(req.climaOlaM());
            m.setClimaTempAguaC(req.climaTempAguaC());
            m.setClimaDescripcion(req.climaDescripcion());
            cambio = true;
        }
        return cambio;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record ResultadoCreacion(MuestraResponse muestra, boolean creada) {
    }
}
