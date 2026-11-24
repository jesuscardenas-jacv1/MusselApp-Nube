package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Tren;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrenRepository extends JpaRepository<Tren, Long> {
    List<Tren> findByCentroIdOrderByCodigoAsc(String centroId);
    Optional<Tren> findByCentroIdAndCodigoIgnoreCase(String centroId, String codigo);
}
