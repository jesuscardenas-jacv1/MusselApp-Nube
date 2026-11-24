package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.dto.MuestraRequest;
import cl.aldemar.musselapp.backend.dto.MuestraResponse;
import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.servicio.MuestraService;
import cl.aldemar.musselapp.backend.servicio.PermisosRol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/muestras")
@CrossOrigin(origins = "*")
@Tag(name = "2. Muestras", description = "CRUD de muestras de choritos con idempotencia para sincronización móvil")
public class MuestraController {

    private final MuestraService servicio;

    public MuestraController(MuestraService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar muestras", description = "Filtra por centro, tren, línea, estado o rango de fechas (epoch ms)")
    public List<MuestraResponse> listar(
            @RequestParam(required = false) String centroId,
            @RequestParam(required = false) String tren,
            @RequestParam(required = false) Integer linea,
            @RequestParam(required = false) EstadoMuestra estado,
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) Long hasta
    ) {
        return servicio.listar(centroId, tren, linea, estado, desde, hasta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener muestra por ID")
    public MuestraResponse obtener(@PathVariable Long id) {
        return servicio.obtenerPorId(id);
    }

    @PostMapping
    @Operation(summary = "Crear muestra (o sincronizar sin duplicar por idCliente)")
    public ResponseEntity<MuestraResponse> crear(
            @Parameter(description = "Rol del usuario (OPERARIO o SUPERVISOR)", example = "OPERARIO")
            @RequestHeader(value = "X-Rol", defaultValue = "OPERARIO") String rol,
            @Valid @RequestBody MuestraRequest request
    ) {
        PermisosRol.exigir(rol, "registrar muestras", PermisosRol.OPERARIO, PermisosRol.SUPERVISOR);
        var resultado = servicio.crearOActualizarPorCliente(request);
        HttpStatus estado = resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.muestra());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una muestra existente")
    public MuestraResponse actualizar(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario (OPERARIO o SUPERVISOR)", example = "SUPERVISOR")
            @RequestHeader(value = "X-Rol", defaultValue = "SUPERVISOR") String rol,
            @Valid @RequestBody MuestraRequest request
    ) {
        PermisosRol.exigir(rol, "actualizar muestras", PermisosRol.OPERARIO, PermisosRol.SUPERVISOR);
        return servicio.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una muestra (solo SUPERVISOR)")
    public void eliminar(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario (solo SUPERVISOR)", example = "SUPERVISOR")
            @RequestHeader(value = "X-Rol", defaultValue = "") String rol
    ) {
        PermisosRol.exigir(rol, "eliminar muestras", PermisosRol.SUPERVISOR);
        servicio.eliminar(id);
    }
}
