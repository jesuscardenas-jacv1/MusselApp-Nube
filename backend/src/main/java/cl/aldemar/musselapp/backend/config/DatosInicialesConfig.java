package cl.aldemar.musselapp.backend.config;

import cl.aldemar.musselapp.backend.entidad.Centro;
import cl.aldemar.musselapp.backend.entidad.EstadoMuestra;
import cl.aldemar.musselapp.backend.entidad.Linea;
import cl.aldemar.musselapp.backend.entidad.Muestra;
import cl.aldemar.musselapp.backend.entidad.Revision;
import cl.aldemar.musselapp.backend.entidad.Servidor;
import cl.aldemar.musselapp.backend.entidad.Tren;
import cl.aldemar.musselapp.backend.repositorio.CentroRepository;
import cl.aldemar.musselapp.backend.repositorio.LineaRepository;
import cl.aldemar.musselapp.backend.repositorio.MuestraRepository;
import cl.aldemar.musselapp.backend.repositorio.RevisionRepository;
import cl.aldemar.musselapp.backend.repositorio.ServidorRepository;
import cl.aldemar.musselapp.backend.repositorio.TrenRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

/**
 * Pobla la base de datos con el mismo catálogo ficticio de la app Android (4 centros, trenes A–C, líneas 1–12)
 * y 3 muestras iniciales de demostración si la base está vacía.
 */
@Configuration
public class DatosInicialesConfig {

    @Bean
    public CommandLineRunner cargarDatosIniciales(
            CentroRepository centroRepository,
            TrenRepository trenRepository,
            LineaRepository lineaRepository,
            MuestraRepository muestraRepository,
            RevisionRepository revisionRepository,
            ServidorRepository servidorRepository
    ) {
        return args -> {
            if (servidorRepository.count() == 0) {
                servidorRepository.save(new Servidor(UUID.randomUUID().toString()));
            }
            if (centroRepository.count() == 0) {
                List<Centro> centros = List.of(
                        new Centro("dalcahue", "Centro Canal Dalcahue - Mód. A (Chiloé Central)", -42.374, -73.622, 1500),
                        new Centro("calbuco", "Centro Calbuco - Isla Huapi (Seno de Reloncaví)", -41.800, -73.170, 1500),
                        new Centro("quellon", "Centro Quellón Sur - Ensenada Yaldad", -43.148, -73.722, 1500),
                        new Centro("melinka", "Centro Melinka - Fiordos de las Guaitecas", -43.905, -73.720, 1500)
                );
                centroRepository.saveAll(centros);

                for (Centro centro : centros) {
                    for (String codigoTren : List.of("A", "B", "C")) {
                        Tren tren = trenRepository.save(new Tren(centro, codigoTren));
                        for (int numero = 1; numero <= 12; numero++) {
                            lineaRepository.save(new Linea(tren, numero));
                        }
                    }
                }
            }

            if (muestraRepository.count() == 0) {
                long ahora = System.currentTimeMillis();
                long horaMs = 3_600_000L;

                Muestra m1 = crearMuestraEjemplo(
                        "seed_001", "Muestra 001", "dalcahue", "A", 4, 3, 56, 410, 8, 100,
                        "Fijación uniforme en estrato medio", EstadoMuestra.VALIDADO, ahora - 24 * horaMs
                );
                m1.setRevisadaPor("Diego Muñoz");
                m1.setRevisadaEn(ahora - 20 * horaMs);
                m1.setComentarioSupervisor("Registro dentro del estándar del centro");
                m1 = muestraRepository.save(m1);

                Revision r1 = new Revision();
                r1.setMuestra(m1);
                r1.setSupervisorNombre("Diego Muñoz");
                r1.setSupervisorRut("222222222");
                r1.setEstadoAsignado(EstadoMuestra.VALIDADO);
                r1.setComentario(m1.getComentarioSupervisor());
                r1.setRevisadaEn(m1.getRevisadaEn());
                revisionRepository.save(r1);

                Muestra m2 = crearMuestraEjemplo(
                        "seed_002", "Muestra 002", "calbuco", "B", 7, 5, 48, 320, 14, 100,
                        "Baja densidad por desprendimiento parcial", EstadoMuestra.CORREGIDO, ahora - 12 * horaMs
                );
                m2.setRevisadaPor("Diego Muñoz");
                m2.setRevisadaEn(ahora - 10 * horaMs);
                m2.setComentarioSupervisor("Recontar tramo superior en siguiente ronda");
                m2 = muestraRepository.save(m2);

                Revision r2 = new Revision();
                r2.setMuestra(m2);
                r2.setSupervisorNombre("Diego Muñoz");
                r2.setSupervisorRut("222222222");
                r2.setEstadoAsignado(EstadoMuestra.CORREGIDO);
                r2.setComentario(m2.getComentarioSupervisor());
                r2.setRevisadaEn(m2.getRevisadaEn());
                revisionRepository.save(r2);

                Muestra m3 = crearMuestraEjemplo(
                        "seed_003", "Muestra 003", "quellon", "C", 2, 1, 52, 395, 5, 100,
                        "Muestra tomada con marea baja", EstadoMuestra.PENDIENTE, ahora - 2 * horaMs
                );
                muestraRepository.save(m3);
            }
        };
    }

    private Muestra crearMuestraEjemplo(
            String idCliente, String codigo, String centroId, String tren, int linea,
            int profundidadM, int tallaMm, int conteoIndM, Integer cholgasIndM, Integer tramoCm,
            String observacion, EstadoMuestra estado, long registradaEn
    ) {
        Muestra m = new Muestra();
        m.setIdCliente(idCliente);
        m.setCodigo(codigo);
        m.setCentroId(centroId);
        m.setTren(tren);
        m.setLinea(linea);
        m.setProfundidadM(profundidadM);
        m.setTallaMm(tallaMm);
        m.setConteoIndM(conteoIndM);
        m.setCholgasIndM(cholgasIndM);
        m.setTramoCm(tramoCm);
        m.setObservacion(observacion);
        m.setEstado(estado);
        m.setRegistradaEn(registradaEn);
        m.setOperador("Carla Vera");
        m.setOperadorRut("111111111");
        m.setActualizadaEn(registradaEn);
        return m;
    }
}
