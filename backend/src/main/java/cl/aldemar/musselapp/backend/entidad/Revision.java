package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Registro histórico de cada dictamen emitido por un supervisor sobre una muestra. */
@Entity
@Table(name = "revisiones")
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "muestra_id", nullable = false)
    private Muestra muestra;

    @Column(name = "supervisor_nombre", nullable = false, length = 100)
    private String supervisorNombre;

    @Column(name = "supervisor_rut", nullable = false, length = 20)
    private String supervisorRut;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_asignado", nullable = false, length = 20)
    private EstadoMuestra estadoAsignado;

    @Column(length = 500)
    private String comentario;

    @Column(name = "revisada_en", nullable = false)
    private long revisadaEn;

    public Revision() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Muestra getMuestra() { return muestra; }
    public void setMuestra(Muestra muestra) { this.muestra = muestra; }

    public String getSupervisorNombre() { return supervisorNombre; }
    public void setSupervisorNombre(String supervisorNombre) { this.supervisorNombre = supervisorNombre; }

    public String getSupervisorRut() { return supervisorRut; }
    public void setSupervisorRut(String supervisorRut) { this.supervisorRut = supervisorRut; }

    public EstadoMuestra getEstadoAsignado() { return estadoAsignado; }
    public void setEstadoAsignado(EstadoMuestra estadoAsignado) { this.estadoAsignado = estadoAsignado; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public long getRevisadaEn() { return revisadaEn; }
    public void setRevisadaEn(long revisadaEn) { this.revisadaEn = revisadaEn; }
}
