package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.entidad.Centro;
import cl.aldemar.musselapp.backend.repositorio.CentroRepository;
import cl.aldemar.musselapp.backend.repositorio.LineaRepository;
import cl.aldemar.musselapp.backend.repositorio.TrenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "1. Catálogo", description = "Centros de cultivo, trenes y líneas disponibles")
public class CatalogoController {

    private final CentroRepository centroRepo;
    private final TrenRepository trenRepo;
    private final LineaRepository lineaRepo;

    public CatalogoController(CentroRepository centroRepo, TrenRepository trenRepo, LineaRepository lineaRepo) {
        this.centroRepo = centroRepo;
        this.trenRepo = trenRepo;
        this.lineaRepo = lineaRepo;
    }

    @GetMapping("/centros")
    @Operation(summary = "Listar todos los centros")
    public List<Centro> listarCentros() {
        return centroRepo.findAll();
    }

    @GetMapping("/centros/{centroId}/trenes")
    @Operation(summary = "Listar los trenes de un centro")
    public List<TrenItem> listarTrenes(@PathVariable String centroId) {
        if (!centroRepo.existsById(centroId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Centro no existe: " + centroId);
        }
        return trenRepo.findByCentroIdOrderByCodigoAsc(centroId).stream()
                .map(t -> new TrenItem(t.getId(), t.getCentro().getId(), t.getCodigo()))
                .toList();
    }

    @GetMapping("/trenes/{trenId}/lineas")
    @Operation(summary = "Listar las líneas de un tren")
    public List<LineaItem> listarLineas(@PathVariable Long trenId) {
        if (!trenRepo.existsById(trenId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tren no existe: " + trenId);
        }
        return lineaRepo.findByTrenIdOrderByNumeroAsc(trenId).stream()
                .map(l -> new LineaItem(l.getId(), l.getTren().getId(), l.getNumero()))
                .toList();
    }

    public record TrenItem(Long id, String centroId, String codigo) {}
    public record LineaItem(Long id, Long trenId, int numero) {}
}
