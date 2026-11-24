package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.dto.MuestraResponse;
import cl.aldemar.musselapp.backend.dto.RevisionRequest;
import cl.aldemar.musselapp.backend.dto.RevisionResponse;
import cl.aldemar.musselapp.backend.servicio.PermisosRol;
import cl.aldemar.musselapp.backend.servicio.RevisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/muestras/{muestraId}/revisiones")
@CrossOrigin(origins = "*")
@Tag(name = "4. Revisiones y estados", description = "Dictámenes del supervisor (VALIDADO / CORREGIDO) e historial")
public class RevisionController {

    private final RevisionService servicio;

    public RevisionController(RevisionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar historial de revisiones de una muestra")
    public List<RevisionResponse> listar(@PathVariable Long muestraId) {
        return servicio.listarPorMuestra(muestraId);
    }

    @PostMapping
    @Operation(summary = "Emitir dictamen sobre una muestra (solo SUPERVISOR)")
    public MuestraResponse dictaminar(
            @PathVariable Long muestraId,
            @Parameter(description = "Rol del usuario (solo SUPERVISOR)", example = "SUPERVISOR")
            @RequestHeader(value = "X-Rol", defaultValue = "") String rol,
            @Valid @RequestBody RevisionRequest request
    ) {
        PermisosRol.exigir(rol, "dictaminar muestras", PermisosRol.SUPERVISOR);
        return servicio.registrarDictamen(muestraId, request);
    }
}
