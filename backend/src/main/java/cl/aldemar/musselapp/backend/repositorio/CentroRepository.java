package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Centro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CentroRepository extends JpaRepository<Centro, String> {
}
