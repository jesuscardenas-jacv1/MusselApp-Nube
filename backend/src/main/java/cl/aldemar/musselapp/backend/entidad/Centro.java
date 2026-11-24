package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Centro de cultivo de choritos (catálogo ficticio). */
@Entity
@Table(name = "centros")
public class Centro {

    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "radio_m", nullable = false)
    private int radioM = 1500;

    public Centro() {
    }

    public Centro(String id, String nombre, double lat, double lon, int radioM) {
        this.id = id;
        this.nombre = nombre;
        this.lat = lat;
        this.lon = lon;
        this.radioM = radioM;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public int getRadioM() { return radioM; }
    public void setRadioM(int radioM) { this.radioM = radioM; }
}
