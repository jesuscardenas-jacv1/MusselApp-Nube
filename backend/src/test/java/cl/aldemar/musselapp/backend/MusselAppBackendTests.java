package cl.aldemar.musselapp.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MusselAppBackendTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void catalogoCargadoAlIniciar() throws Exception {
        mockMvc.perform(get("/api/centros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/centros/dalcahue/trenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void flujoCrudIdempotenciaFotoRevisionYPermisos() throws Exception {
        String cuerpoMuestra = """
                {
                  "idCliente": "tel_test_99",
                  "codigo": "Muestra 099",
                  "centroId": "dalcahue",
                  "tren": "B",
                  "linea": 5,
                  "profundidadM": 3,
                  "tallaMm": 55,
                  "conteoIndM": 420,
                  "cholgasIndM": 12,
                  "tramoCm": 100,
                  "observacion": "Prueba integrada",
                  "operador": "Carla Vera",
                  "operadorRut": "111111111"
                }
                """;

        // 1. Analista no puede crear
        mockMvc.perform(post("/api/muestras")
                        .header("X-Rol", "ANALISTA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra))
                .andExpect(status().isForbidden());

        // 2. Operario crea muestra (201)
        String respuestaCrear = mockMvc.perform(post("/api/muestras")
                        .header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("Muestra 099"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode nodo = objectMapper.readTree(respuestaCrear);
        long id = nodo.get("id").asLong();

        // 3. Reintento con mismo idCliente actualiza sin duplicar (200)
        mockMvc.perform(post("/api/muestras")
                        .header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra.replace("420", "435")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.conteoIndM").value(435));

        // 4. Subir y consultar foto
        String cuerpoFoto = """
                {
                  "nombreArchivo": "muestra_099.jpg",
                  "tipoMime": "image/jpeg",
                  "contenidoBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
                }
                """;
        mockMvc.perform(put("/api/muestras/" + id + "/foto")
                        .header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoFoto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muestraId").value(id))
                .andExpect(jsonPath("$.nombreArchivo").value("muestra_099.jpg"));

        mockMvc.perform(get("/api/muestras/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFoto").value(true));

        // 5. Operario no puede dictaminar (403)
        String dictamenCorregirCorto = """
                {"estado": "CORREGIDO", "comentario": "mal"}
                """;
        mockMvc.perform(post("/api/muestras/" + id + "/revisiones")
                        .header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dictamenCorregirCorto))
                .andExpect(status().isForbidden());

        // 6. Supervisor al corregir debe escribir >= 5 caracteres (400)
        mockMvc.perform(post("/api/muestras/" + id + "/revisiones")
                        .header("X-Rol", "SUPERVISOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dictamenCorregirCorto))
                .andExpect(status().isBadRequest());

        // 7. Supervisor valida muestra (200)
        String dictamenValidar = """
                {"estado": "VALIDADO", "comentario": "Datos consistentes"}
                """;
        mockMvc.perform(post("/api/muestras/" + id + "/revisiones")
                        .header("X-Rol", "SUPERVISOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dictamenValidar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.revisadaPor").value("Diego Muñoz"));

        // 8. Reportes y OpenAPI responden correctamente
        mockMvc.perform(get("/api/reportes/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMuestras", greaterThanOrEqualTo(4)));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", containsString("MusselApp")));

        // 9. Operario no puede eliminar (403), supervisor sí (204)
        mockMvc.perform(delete("/api/muestras/" + id).header("X-Rol", "OPERARIO"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/muestras/" + id).header("X-Rol", "SUPERVISOR"))
                .andExpect(status().isNoContent());
    }
}
