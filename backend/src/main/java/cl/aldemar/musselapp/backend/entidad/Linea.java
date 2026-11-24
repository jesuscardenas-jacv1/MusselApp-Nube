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

/** Línea de cultivo dentro de un tren (1 a 12). */
@Entity
@Table(name = "lineas", uniqueConstraints = @UniqueConstraint(columnNames = {"tren_id", "numero"}))
public class Linea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tren_id", nullable = false)
    private Tren tren;

    @Column(nullable = false)
    private int numero;

    public Linea() {
    }

    public Linea(Tren tren, int numero) {
        this.tren = tren;
        this.numero = numero;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tren getTren() { return tren; }
    public void setTren(Tren tren) { this.tren = tren; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
}
