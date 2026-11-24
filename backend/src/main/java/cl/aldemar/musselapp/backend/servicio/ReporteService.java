package cl.aldemar.musselapp.backend.servicio;

import cl.aldemar.musselapp.backend.dto.MuestraResponse;
import cl.aldemar.musselapp.backend.dto.ReporteLineaResponse;
import cl.aldemar.musselapp.backend.dto.ReporteResumenResponse;
import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import cl.aldemar.musselapp.backend.repositorio.MuestraRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Cálculo de indicadores y reportes sobre las muestras registradas. */
@Service
public class ReporteService {

    private final MuestraRepository muestraRepository;

    public ReporteService(MuestraRepository muestraRepository) {
        this.muestraRepository = muestraRepository;
    }

    public ReporteResumenResponse resumenGeneral(String centroId) {
        List<Muestra> muestras = muestraRepository.findAllByOrderByRegistradaEnDesc().stream()
                .filter(m -> centroId == null || centroId.isBlank() || m.getCentroId().equalsIgnoreCase(centroId.trim()))
                .toList();

        long pendientes = muestras.stream().filter(m -> m.getEstado() == EstadoMuestra.PENDIENTE).count();
        long validadas = muestras.stream().filter(m -> m.getEstado() == EstadoMuestra.VALIDADO).count();
        long corregidas = muestras.stream().filter(m -> m.getEstado() == EstadoMuestra.CORREGIDO).count();

        Map<String, List<Muestra>> porCentroMap = muestras.stream()
                .collect(Collectors.groupingBy(Muestra::getCentroId));

        List<ReporteResumenResponse.ResumenCentro> porCentro = porCentroMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ReporteResumenResponse.ResumenCentro(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().filter(m -> m.getEstado() == EstadoMuestra.VALIDADO).count(),
                        redondear(entry.getValue().stream().mapToInt(Muestra::getConteoIndM).average().orElse(0)),
                        redondear(entry.getValue().stream().mapToInt(Muestra::getTallaMm).average().orElse(0))
                ))
                .toList();

        return new ReporteResumenResponse(
                muestras.size(),
                pendientes,
                validadas,
                corregidas,
                redondear(muestras.stream().mapToInt(Muestra::getConteoIndM).average().orElse(0)),
                redondear(muestras.stream().mapToInt(Muestra::getTallaMm).average().orElse(0)),
                porCentro
        );
    }

    public List<ReporteLineaResponse> evolucionPorLinea(String centroId, String tren, Integer linea) {
        List<Muestra> filtradas = muestraRepository.findAllByOrderByRegistradaEnDesc().stream()
                .filter(m -> centroId == null || centroId.isBlank() || m.getCentroId().equalsIgnoreCase(centroId.trim()))
                .filter(m -> tren == null || tren.isBlank() || m.getTren().equalsIgnoreCase(tren.trim()))
                .filter(m -> linea == null || m.getLinea() == linea)
                .toList();

        Map<String, List<Muestra>> grupos = filtradas.stream()
                .collect(Collectors.groupingBy(m -> m.getCentroId() + "|" + m.getTren() + "|" + m.getLinea()));

        return grupos.values().stream()
                .map(lista -> {
                    Muestra ref = lista.get(0);
                    List<MuestraResponse> ordenadas = lista.stream()
                            .sorted(Comparator.comparingLong(Muestra::getRegistradaEn))
                            .map(MuestraResponse::desde)
                            .toList();
                    return new ReporteLineaResponse(
                            ref.getCentroId(),
                            ref.getTren(),
                            ref.getLinea(),
                            lista.size(),
                            redondear(lista.stream().mapToInt(Muestra::getConteoIndM).average().orElse(0)),
                            redondear(lista.stream().mapToInt(Muestra::getTallaMm).average().orElse(0)),
                            ordenadas
                    );
                })
                .sorted(Comparator.comparing(ReporteLineaResponse::centroId)
                        .thenComparing(ReporteLineaResponse::tren)
                        .thenComparingInt(ReporteLineaResponse::linea))
                .toList();
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
