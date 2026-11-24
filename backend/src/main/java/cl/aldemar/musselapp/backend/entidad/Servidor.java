package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Identidad de esta base de datos: cada computador tiene la suya y el teléfono no debe confundirlas. */
@Entity
@Table(name = "servidor")
public class Servidor {

    @Id
    private int id = 1;

    @Column(nullable = false, length = 36)
    private String uuid;

    public Servidor() {
    }

    public Servidor(String uuid) { this.uuid = uuid; }

    public int getId() { return id; }
    public String getUuid() { return uuid; }
}
