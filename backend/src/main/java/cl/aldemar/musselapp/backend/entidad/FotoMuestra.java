package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Fotografía de evidencia y sus metadatos, separada de la muestra para agilizar listados. */
@Entity
@Table(name = "fotos_muestra")
public class FotoMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "muestra_id", nullable = false, unique = true)
    private Long muestraId;

    @Column(name = "nombre_archivo", nullable = false, length = 160)
    private String nombreArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 60)
    private String tipoMime = "image/jpeg";

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "registrada_en", nullable = false)
    private long registradaEn;

    @Lob
    @Column(name = "contenido_base64", nullable = false)
    private String contenidoBase64;

    public FotoMuestra() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMuestraId() { return muestraId; }
    public void setMuestraId(Long muestraId) { this.muestraId = muestraId; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public long getRegistradaEn() { return registradaEn; }
    public void setRegistradaEn(long registradaEn) { this.registradaEn = registradaEn; }

    public String getContenidoBase64() { return contenidoBase64; }
    public void setContenidoBase64(String contenidoBase64) { this.contenidoBase64 = contenidoBase64; }
}
