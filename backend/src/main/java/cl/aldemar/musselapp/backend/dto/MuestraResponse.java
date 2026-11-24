package cl.aldemar.musselapp.backend.dto;

import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import io.swagger.v3.oas.annotations.media.Schema;

/** Representación de salida de una muestra registrada en el backend. */
@Schema(description = "Muestra de choritos persistida en la base de datos")
public record MuestraResponse(
        Long id,
        String idCliente,
        String codigo,
        String centroId,
        String tren,
        int linea,
        int profundidadM,
        int tallaMm,
        int conteoIndM,
        Integer cholgasIndM,
        Integer tramoCm,
        String observacion,
        EstadoMuestra estado,
        long registradaEn,
        String operador,
        String operadorRut,
        Double lat,
        Double lon,
        Float precisionM,
        Integer distanciaCentroM,
        Double climaTempAireC,
        Double climaVientoKn,
        Double climaOlaM,
        Double climaTempAguaC,
        String climaDescripcion,
        String revisadaPor,
        Long revisadaEn,
        String comentarioSupervisor,
        Integer conteoOperario,
        Integer tallaOperario,
        Integer cholgasOperario,
        boolean tieneFoto,
        long actualizadaEn
) {
    public static MuestraResponse desde(Muestra m) {
        return new MuestraResponse(
                m.getId(),
                m.getIdCliente(),
                m.getCodigo(),
                m.getCentroId(),
                m.getTren(),
                m.getLinea(),
                m.getProfundidadM(),
                m.getTallaMm(),
                m.getConteoIndM(),
                m.getCholgasIndM(),
                m.getTramoCm(),
                m.getObservacion(),
                m.getEstado(),
                m.getRegistradaEn(),
                m.getOperador(),
                m.getOperadorRut(),
                m.getLat(),
                m.getLon(),
                m.getPrecisionM(),
                m.getDistanciaCentroM(),
                m.getClimaTempAireC(),
                m.getClimaVientoKn(),
                m.getClimaOlaM(),
                m.getClimaTempAguaC(),
                m.getClimaDescripcion(),
                m.getRevisadaPor(),
                m.getRevisadaEn(),
                m.getComentarioSupervisor(),
                m.getConteoOperario(),
                m.getTallaOperario(),
                m.getCholgasOperario(),
                m.isTieneFoto(),
                m.getActualizadaEn()
        );
    }
}
