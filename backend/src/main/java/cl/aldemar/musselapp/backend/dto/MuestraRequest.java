package cl.aldemar.musselapp.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de entrada para registrar o actualizar una muestra de choritos. */
@Schema(description = "Datos para crear o actualizar una muestra de choritos", example = """
        {"centroId":"dalcahue","tren":"B","linea":8,"profundidadM":3,"tallaMm":62,"conteoIndM":390,\
        "cholgasIndM":6,"tramoCm":40,"observacion":"Creada desde Swagger","operador":"Carla Vera","operadorRut":"111111111"}""")
public record MuestraRequest(
        @Size(max = 80, message = "El idCliente admite hasta 80 caracteres")
        @Schema(description = "Identificador único que pone el teléfono. Desde Swagger se omite: el servidor genera uno", nullable = true)
        String idCliente,

        @Size(max = 40, message = "El código admite hasta 40 caracteres")
        @Schema(description = "Código visible (si se omite, el servidor genera uno)", nullable = true)
        String codigo,

        @NotBlank(message = "El centroId es obligatorio")
        @Schema(description = "ID del centro de cultivo (dalcahue, calbuco, quellon, melinka)", example = "dalcahue")
        String centroId,

        @NotBlank(message = "El tren es obligatorio")
        @Schema(description = "Código del tren (A, B o C)", example = "B")
        String tren,

        @Min(value = 1, message = "La línea debe estar entre 1 y 12")
        @Max(value = 12, message = "La línea debe estar entre 1 y 12")
        @Schema(description = "Número de línea dentro del tren (1 a 12)", example = "8")
        int linea,

        @Min(value = 1, message = "La profundidad debe estar entre 1 y 6 m")
        @Max(value = 6, message = "La profundidad debe estar entre 1 y 6 m")
        @Schema(description = "Profundidad o estrato en metros (1, 3 o 5)", example = "3")
        int profundidadM,

        @Min(value = 1, message = "La talla debe estar entre 1 y 100 mm")
        @Max(value = 100, message = "La talla debe estar entre 1 y 100 mm")
        @Schema(description = "Talla promedio en milímetros", example = "54")
        int tallaMm,

        @Min(value = 1, message = "El conteo debe ser mayor a 0")
        @Max(value = 5000, message = "El conteo no puede superar 5000 ind/m")
        @Schema(description = "Densidad de choritos en individuos por metro", example = "412")
        int conteoIndM,

        @Min(value = 0, message = "Las cholgas no pueden ser negativas")
        @Max(value = 5000, message = "Las cholgas no pueden superar 5000 ind/m")
        @Schema(description = "Cholgas acompañantes en ind/m (opcional)", example = "10")
        Integer cholgasIndM,

        @Min(value = 1, message = "El tramo debe estar entre 1 y 500 cm")
        @Max(value = 500, message = "El tramo debe estar entre 1 y 500 cm")
        @Schema(description = "Largo del tramo de cuerda muestreado en cm", example = "100")
        Integer tramoCm,

        @Size(max = 500, message = "La observación admite hasta 500 caracteres")
        @Schema(description = "Observaciones técnicas del muestreo", example = "Buena fijación y tamaño parejo")
        String observacion,

        @Schema(description = "Marca temporal del registro en milisegundos (opcional, usa hora actual)")
        Long registradaEn,

        @Size(max = 100)
        @Schema(description = "Nombre del operario ficticio (obligatorio al crear)", example = "Carla Vera")
        String operador,

        @Size(max = 20)
        @Schema(description = "RUT normalizado del operario ficticio (obligatorio al crear)", example = "111111111")
        String operadorRut,

        @Schema(description = "Latitud GPS al muestrear (opcional)", example = "-42.3741")
        Double lat,

        @Schema(description = "Longitud GPS al muestrear (opcional)", example = "-73.6220")
        Double lon,

        @Schema(description = "Precisión del GPS en metros (opcional)", example = "12.0")
        Float precisionM,

        @Schema(description = "Distancia calculada al punto del centro en metros (opcional)", example = "180")
        Integer distanciaCentroM,

        @Schema(description = "Temperatura del aire en °C (Open-Meteo, referencial)", example = "12.5")
        Double climaTempAireC,

        @Schema(description = "Velocidad del viento en nudos (Open-Meteo, referencial)", example = "9.4")
        Double climaVientoKn,

        @Schema(description = "Altura de ola en metros (Open-Meteo Marine, referencial)", example = "0.6")
        Double climaOlaM,

        @Schema(description = "Temperatura superficial del agua en °C (Open-Meteo Marine)", example = "11.2")
        Double climaTempAguaC,

        @Size(max = 80)
        @Schema(description = "Descripción del tiempo (Open-Meteo)", example = "Parcialmente nublado")
        String climaDescripcion
) {
}
