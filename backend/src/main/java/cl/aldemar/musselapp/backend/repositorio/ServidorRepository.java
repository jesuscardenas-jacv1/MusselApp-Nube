package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Servidor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServidorRepository extends JpaRepository<Servidor, Integer> {
}
