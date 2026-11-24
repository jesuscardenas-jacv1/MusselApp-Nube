package cl.aldemar.musselapp.backend.repositorio;

import cl.aldemar.musselapp.backend.entidad.Revision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionRepository extends JpaRepository<Revision, Long> {
    List<Revision> findByMuestraIdOrderByRevisadaEnDescIdDesc(Long muestraId);
    void deleteByMuestraId(Long muestraId);
}
