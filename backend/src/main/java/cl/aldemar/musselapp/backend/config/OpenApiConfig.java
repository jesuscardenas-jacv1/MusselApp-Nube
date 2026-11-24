package cl.aldemar.musselapp.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de Swagger UI / OpenAPI 3 para la documentación interactiva del backend. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mussellAppOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MusselApp API · AquaSample (ALDEMAR SpA)")
                        .version("1.0.0")
                        .description("""
                                Backend REST con Spring Boot + JPA + H2 para el registro, revisión y trazabilidad de muestras de choritos.
                                
                                **Permisos por rol (cabecera `X-Rol`):**
                                - `OPERARIO`: consulta catálogo/muestras y crea o edita muestras y fotos.
                                - `SUPERVISOR`: todo lo anterior, además de dictaminar (`VALIDADO` / `CORREGIDO`) y eliminar muestras.
                                - `ANALISTA`: consulta muestras, revisiones y reportes (solo lectura).
                                
                                *Proyecto académico DSY1105 Duoc UC · solo datos ficticios.*
                                """)
                        .contact(new Contact().name("Equipo Pipeño · Sección 004V")));
    }
}
