package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Linea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineaRepository extends JpaRepository<Linea, Long> {
    List<Linea> findByTrenIdOrderByNumeroAsc(Long trenId);
    boolean existsByTrenCentroIdAndTrenCodigoIgnoreCaseAndNumero(String centroId, String trenCodigo, int numero);
}
