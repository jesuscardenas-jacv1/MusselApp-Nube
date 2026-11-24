package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.FotoMuestra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FotoMuestraRepository extends JpaRepository<FotoMuestra, Long> {
    Optional<FotoMuestra> findByMuestraId(Long muestraId);
    void deleteByMuestraId(Long muestraId);
}
