package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.dto.ReporteLineaResponse;
import cl.aldemar.musselapp.backend.dto.ReporteResumenResponse;
import cl.aldemar.musselapp.backend.servicio.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
@Tag(name = "5. Reportes", description = "Indicadores agregados y comparación histórica por línea de cultivo")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen general de muestras e indicadores por centro")
    public ReporteResumenResponse resumen(@RequestParam(required = false) String centroId) {
        return servicio.resumenGeneral(centroId);
    }

    @GetMapping("/por-linea")
    @Operation(summary = "Comparación histórica agrupada por centro, tren y línea")
    public List<ReporteLineaResponse> porLinea(
            @RequestParam(required = false) String centroId,
            @RequestParam(required = false) String tren,
            @RequestParam(required = false) Integer linea
    ) {
        return servicio.evolucionPorLinea(centroId, tren, linea);
    }
}
