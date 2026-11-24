package cl.aldemar.musselapp.backend.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Muestra de choritos registrada en terreno por un operario o ingresada vía API REST.
 * El campo {@code idCliente} garantiza idempotencia al sincronizar desde la app móvil (WorkManager).
 */
@Entity
@Table(name = "muestras")
public class Muestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador único generado por el teléfono (ej. "a1b2c3d4_15") para evitar duplicados. */
    @Column(name = "id_cliente", unique = true, length = 80)
    private String idCliente;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "centro_id", nullable = false, length = 40)
    private String centroId;

    @Column(nullable = false, length = 10)
    private String tren;

    @Column(nullable = false)
    private int linea;

    @Column(name = "profundidad_m", nullable = false)
    private int profundidadM;

    @Column(name = "talla_mm", nullable = false)
    private int tallaMm;

    @Column(name = "conteo_ind_m", nullable = false)
    private int conteoIndM;

    @Column(name = "cholgas_ind_m")
    private Integer cholgasIndM;

    @Column(name = "tramo_cm")
    private Integer tramoCm;

    @Column(length = 500)
    private String observacion = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMuestra estado = EstadoMuestra.PENDIENTE;

    @Column(name = "registrada_en", nullable = false)
    private long registradaEn;

    @Column(nullable = false, length = 100)
    private String operador;

    @Column(name = "operador_rut", nullable = false, length = 20)
    private String operadorRut;

    private Double lat;
    private Double lon;

    @Column(name = "precision_m")
    private Float precisionM;

    @Column(name = "distancia_centro_m")
    private Integer distanciaCentroM;

    @Column(name = "clima_temp_aire_c")
    private Double climaTempAireC;

    @Column(name = "clima_viento_kn")
    private Double climaVientoKn;

    @Column(name = "clima_ola_m")
    private Double climaOlaM;

    @Column(name = "clima_temp_agua_c")
    private Double climaTempAguaC;

    @Column(name = "clima_descripcion", length = 80)
    private String climaDescripcion;

    @Column(name = "revisada_por", length = 100)
    private String revisadaPor;

    @Column(name = "revisada_en")
    private Long revisadaEn;

    @Column(name = "comentario_supervisor", length = 500)
    private String comentarioSupervisor;

    /** Lo que registró el operario, si el supervisor corrigió los valores; null si no se corrigieron. */
    @Column(name = "conteo_operario")
    private Integer conteoOperario;

    @Column(name = "talla_operario")
    private Integer tallaOperario;

    @Column(name = "cholgas_operario")
    private Integer cholgasOperario;

    @Column(name = "tiene_foto", nullable = false)
    private boolean tieneFoto = false;

    @Column(name = "actualizada_en", nullable = false)
    private long actualizadaEn;

    public Muestra() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCentroId() { return centroId; }
    public void setCentroId(String centroId) { this.centroId = centroId; }

    public String getTren() { return tren; }
    public void setTren(String tren) { this.tren = tren; }

    public int getLinea() { return linea; }
    public void setLinea(int linea) { this.linea = linea; }

    public int getProfundidadM() { return profundidadM; }
    public void setProfundidadM(int profundidadM) { this.profundidadM = profundidadM; }

    public int getTallaMm() { return tallaMm; }
    public void setTallaMm(int tallaMm) { this.tallaMm = tallaMm; }

    public int getConteoIndM() { return conteoIndM; }
    public void setConteoIndM(int conteoIndM) { this.conteoIndM = conteoIndM; }

    public Integer getCholgasIndM() { return cholgasIndM; }
    public void setCholgasIndM(Integer cholgasIndM) { this.cholgasIndM = cholgasIndM; }

    public Integer getTramoCm() { return tramoCm; }
    public void setTramoCm(Integer tramoCm) { this.tramoCm = tramoCm; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public EstadoMuestra getEstado() { return estado; }
    public void setEstado(EstadoMuestra estado) { this.estado = estado; }

    public long getRegistradaEn() { return registradaEn; }
    public void setRegistradaEn(long registradaEn) { this.registradaEn = registradaEn; }

    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }

    public String getOperadorRut() { return operadorRut; }
    public void setOperadorRut(String operadorRut) { this.operadorRut = operadorRut; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public Float getPrecisionM() { return precisionM; }
    public void setPrecisionM(Float precisionM) { this.precisionM = precisionM; }

    public Integer getDistanciaCentroM() { return distanciaCentroM; }
    public void setDistanciaCentroM(Integer distanciaCentroM) { this.distanciaCentroM = distanciaCentroM; }

    public Double getClimaTempAireC() { return climaTempAireC; }
    public void setClimaTempAireC(Double climaTempAireC) { this.climaTempAireC = climaTempAireC; }

    public Double getClimaVientoKn() { return climaVientoKn; }
    public void setClimaVientoKn(Double climaVientoKn) { this.climaVientoKn = climaVientoKn; }

    public Double getClimaOlaM() { return climaOlaM; }
    public void setClimaOlaM(Double climaOlaM) { this.climaOlaM = climaOlaM; }

    public Double getClimaTempAguaC() { return climaTempAguaC; }
    public void setClimaTempAguaC(Double climaTempAguaC) { this.climaTempAguaC = climaTempAguaC; }

    public String getClimaDescripcion() { return climaDescripcion; }
    public void setClimaDescripcion(String climaDescripcion) { this.climaDescripcion = climaDescripcion; }

    public String getRevisadaPor() { return revisadaPor; }
    public void setRevisadaPor(String revisadaPor) { this.revisadaPor = revisadaPor; }

    public Long getRevisadaEn() { return revisadaEn; }
    public void setRevisadaEn(Long revisadaEn) { this.revisadaEn = revisadaEn; }

    public String getComentarioSupervisor() { return comentarioSupervisor; }
    public void setComentarioSupervisor(String comentarioSupervisor) { this.comentarioSupervisor = comentarioSupervisor; }

    public Integer getConteoOperario() { return conteoOperario; }
    public void setConteoOperario(Integer conteoOperario) { this.conteoOperario = conteoOperario; }

    public Integer getTallaOperario() { return tallaOperario; }
    public void setTallaOperario(Integer tallaOperario) { this.tallaOperario = tallaOperario; }

    public Integer getCholgasOperario() { return cholgasOperario; }
    public void setCholgasOperario(Integer cholgasOperario) { this.cholgasOperario = cholgasOperario; }

    public boolean isTieneFoto() { return tieneFoto; }
    public void setTieneFoto(boolean tieneFoto) { this.tieneFoto = tieneFoto; }

    public long getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(long actualizadaEn) { this.actualizadaEn = actualizadaEn; }

    /** Marca el cambio con la hora actual; siempre crece, aunque dos cambios caigan en el mismo milisegundo. */
    public void tocar() { this.actualizadaEn = Math.max(System.currentTimeMillis(), actualizadaEn + 1); }

    public boolean yaDictaminada() { return estado != EstadoMuestra.PENDIENTE; }
}
