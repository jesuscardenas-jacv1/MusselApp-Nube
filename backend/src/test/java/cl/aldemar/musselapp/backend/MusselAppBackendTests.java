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

        // 1. Sin rol no se puede crear, y un rol inventado se rechaza
        mockMvc.perform(post("/api/muestras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/muestras")
                        .header("X-Rol", "GERENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra))
                .andExpect(status().isBadRequest());

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

        // 3. Reintento con el mismo idCliente: no duplica ni pisa lo guardado (200)
        mockMvc.perform(post("/api/muestras")
                        .header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMuestra.replace("420", "435")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.conteoIndM").value(420));

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

    private static final String VALIDA = """
            {"centroId":"calbuco","tren":"A","linea":2,"profundidadM":3,"tallaMm":62,"conteoIndM":390,"cholgasIndM":6,
             "tramoCm":40,"observacion":"x","operador":"Pedro Soto","operadorRut":"333333333","lat":-41.8,"lon":-73.1}
            """;

    private JsonNode crear(String cuerpo) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/muestras").header("X-Rol", "OPERARIO")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions editar(long id, String rol, String cuerpo) throws Exception {
        var pedido = put("/api/muestras/" + id).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mockMvc.perform(rol == null ? pedido : pedido.header("X-Rol", rol));
    }

    private org.springframework.test.web.servlet.ResultActions dictaminar(long id, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/muestras/" + id + "/revisiones").header("X-Rol", "SUPERVISOR")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void sinCabeceraDeRolNoSeModificaNada() throws Exception {
        long id = crear(VALIDA).get("id").asLong();
        editar(id, null, VALIDA).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/muestras/" + id + "/foto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreArchivo\":\"a.jpg\",\"contenidoBase64\":\"AA\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/muestras/" + id)).andExpect(status().isForbidden());
    }

    @Test
    void ganaElPrimerDictamen() throws Exception {
        long id = crear(VALIDA).get("id").asLong();
        dictaminar(id, "{\"estado\":\"VALIDADO\",\"comentario\":\"ok\"}").andExpect(status().isOk());
        dictaminar(id, "{\"estado\":\"CORREGIDO\",\"comentario\":\"otro dictamen\"}").andExpect(status().isConflict());
        editar(id, "OPERARIO", VALIDA.replace("390", "111")).andExpect(status().isConflict());
        mockMvc.perform(get("/api/muestras/" + id)).andExpect(jsonPath("$.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.conteoIndM").value(390));
    }

    @Test
    void corregirGuardaLoQueRegistroElOperario() throws Exception {
        long id = crear(VALIDA).get("id").asLong();
        dictaminar(id, "{\"estado\":\"VALIDADO\",\"conteoIndM\":380}").andExpect(status().isBadRequest());
        dictaminar(id, "{\"estado\":\"CORREGIDO\",\"comentario\":\"recuento\",\"conteoIndM\":380}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteoIndM").value(380))
                .andExpect(jsonPath("$.conteoOperario").value(390))
                .andExpect(jsonPath("$.tallaOperario").value(62));
    }

    @Test
    void editarConservaLoQueNoViene() throws Exception {
        JsonNode m = crear(VALIDA);
        editar(m.get("id").asLong(), "SUPERVISOR",
                "{\"centroId\":\"calbuco\",\"tren\":\"A\",\"linea\":2,\"profundidadM\":3,\"tallaMm\":62,\"conteoIndM\":385}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteoIndM").value(385))
                .andExpect(jsonPath("$.operador").value("Pedro Soto"))
                .andExpect(jsonPath("$.lat").value(-41.8))
                .andExpect(jsonPath("$.cholgasIndM").value(6))
                .andExpect(jsonPath("$.tramoCm").value(40))
                .andExpect(jsonPath("$.actualizadaEn").value(org.hamcrest.Matchers.greaterThan(m.get("actualizadaEn").asLong())));
    }

    @Test
    void unReintentoDelTelefonoNoPisaElPutYCompletaElClima() throws Exception {
        String delTelefono = VALIDA.replace("{\"centroId\"", "{\"idCliente\":\"tel01_77\",\"centroId\"");
        long id = crear(delTelefono).get("id").asLong();
        editar(id, "SUPERVISOR", VALIDA.replace("390", "380")).andExpect(status().isOk());
        mockMvc.perform(post("/api/muestras").header("X-Rol", "OPERARIO").contentType(MediaType.APPLICATION_JSON)
                        .content(delTelefono.replace("\"lat\"", "\"climaTempAireC\":11.5,\"lat\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.conteoIndM").value(380))
                .andExpect(jsonPath("$.climaTempAireC").value(11.5));
        mockMvc.perform(get("/api/muestras").param("idCliente", "tel01_77")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void desdeSwaggerCadaCreacionEsUnaMuestraConIdentificadorPropio() throws Exception {
        JsonNode a = crear(VALIDA);
        JsonNode b = crear(VALIDA);
        org.junit.jupiter.api.Assertions.assertTrue(a.get("idCliente").asText().startsWith("api_"));
        org.junit.jupiter.api.Assertions.assertNotEquals(a.get("idCliente").asText(), b.get("idCliente").asText());
    }

    @Test
    void mismosLimitesQueLaApp() throws Exception {
        crear(VALIDA.replace("\"tramoCm\":40", "\"tramoCm\":1"));
        for (String mala : new String[]{
                VALIDA.replace("\"tallaMm\":62", "\"tallaMm\":101"),
                VALIDA.replace("\"profundidadM\":3", "\"profundidadM\":7"),
                VALIDA.replace("\"tramoCm\":40", "\"tramoCm\":501"),
                VALIDA.replace("\"conteoIndM\":390", "\"conteoIndM\":5001"),
                VALIDA.replace("Pedro Soto", ""),
                VALIDA.replace("\"linea\":2", "\"linea\":13")}) {
            mockMvc.perform(post("/api/muestras").header("X-Rol", "OPERARIO").contentType(MediaType.APPLICATION_JSON).content(mala))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        }
    }

    @Test
    void erroresLegiblesYSinCorsAbierto() throws Exception {
        long id = crear(VALIDA).get("id").asLong();
        dictaminar(id, "{\"estado\":\"OBSERVADO\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        mockMvc.perform(get("/api/muestras/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        mockMvc.perform(get("/api/muestras/999999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/muestras").header("Origin", "https://otro-sitio.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mockMvc.perform(get("/api/servidor")).andExpect(jsonPath("$.id").isNotEmpty());
    }
}
