package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Evolución de muestreos agrupada por centro, tren y línea. */
@Schema(description = "Comparación histórica de muestras de una misma línea de cultivo")
public record ReporteLineaResponse(
        String centroId,
        String tren,
        int linea,
        long totalMuestras,
        double promedioConteoIndM,
        double promedioTallaMm,
        List<MuestraResponse> muestras
) {
}
