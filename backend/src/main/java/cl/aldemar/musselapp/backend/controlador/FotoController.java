package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.dto.FotoRequest;
import cl.aldemar.musselapp.backend.dto.FotoResponse;
import cl.aldemar.musselapp.backend.servicio.FotoService;
import cl.aldemar.musselapp.backend.servicio.PermisosRol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/muestras/{muestraId}/foto")
@Tag(name = "3. Fotos y metadatos", description = "Gestión de fotografías de evidencia asociadas a cada muestra")
public class FotoController {

    private final FotoService servicio;

    public FotoController(FotoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Obtener fotografía y metadatos de una muestra")
    public FotoResponse obtener(@PathVariable Long muestraId) {
        return servicio.obtenerPorMuestra(muestraId);
    }

    @PutMapping
    @Operation(summary = "Subir o reemplazar la fotografía de una muestra")
    public FotoResponse guardar(
            @PathVariable Long muestraId,
            @Parameter(description = "Rol del usuario (OPERARIO o SUPERVISOR)", example = "OPERARIO")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody FotoRequest request
    ) {
        PermisosRol.exigir(rol, "subir fotos", PermisosRol.OPERARIO, PermisosRol.SUPERVISOR);
        return servicio.guardar(muestraId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar la fotografía de una muestra (solo SUPERVISOR)")
    public void eliminar(
            @PathVariable Long muestraId,
            @Parameter(description = "Rol del usuario (solo SUPERVISOR)", example = "SUPERVISOR")
            @RequestHeader(value = "X-Rol", required = false) String rol
    ) {
        PermisosRol.exigir(rol, "eliminar fotos", PermisosRol.SUPERVISOR);
        servicio.eliminar(muestraId);
    }
}
