package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Tren de cultivo (longline) perteneciente a un centro. */
@Entity
@Table(name = "trenes", uniqueConstraints = @UniqueConstraint(columnNames = {"centro_id", "codigo"}))
public class Tren {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Column(nullable = false, length = 10)
    private String codigo;

    public Tren() {
    }

    public Tren(Centro centro, String codigo) {
        this.centro = centro;
        this.codigo = codigo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Centro getCentro() { return centro; }
    public void setCentro(Centro centro) { this.centro = centro; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
