package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Indicadores agregados de muestreo para dashboard y exportación. */
@Schema(description = "Resumen general de muestras e indicadores por centro")
public record ReporteResumenResponse(
        long totalMuestras,
        long pendientes,
        long validadas,
        long corregidas,
        double promedioConteoIndM,
        double promedioTallaMm,
        List<ResumenCentro> porCentro
) {
    public record ResumenCentro(
            String centroId,
            long cantidad,
            long validadas,
            double promedioConteoIndM,
            double promedioTallaMm
    ) {
    }
}
