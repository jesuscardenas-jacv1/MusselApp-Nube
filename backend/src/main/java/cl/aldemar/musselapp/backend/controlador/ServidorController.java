package cl.aldemar.musselapp.backend.controlador;

import cl.aldemar.musselapp.backend.repositorio.ServidorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servidor")
@Tag(name = "6. Servidor", description = "Identidad de esta base de datos, para la sincronización del teléfono")
public class ServidorController {

    private final ServidorRepository repositorio;

    public ServidorController(ServidorRepository repositorio) { this.repositorio = repositorio; }

    @GetMapping
    @Operation(summary = "Identificador único de esta base de datos")
    public ServidorInfo obtener() {
        return new ServidorInfo(repositorio.findById(1).orElseThrow().getUuid());
    }

    public record ServidorInfo(String id) {
    }
}
