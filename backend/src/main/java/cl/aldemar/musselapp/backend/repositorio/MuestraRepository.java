package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Muestra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MuestraRepository extends JpaRepository<Muestra, Long> {
    Optional<Muestra> findByIdCliente(String idCliente);
    List<Muestra> findAllByOrderByRegistradaEnDesc();
}
