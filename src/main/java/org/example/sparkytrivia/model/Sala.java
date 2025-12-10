package org.example.sparkytrivia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sala")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salaid")
    private Integer salaId;

    @Column(name = "codigosala", unique = true, nullable = false, length = 10)
    private String codigoSala;

    @Column(name = "nombresala", length = 255)
    private String nombreSala;

    @ManyToOne
    @JoinColumn(name = "fktrivia", nullable = false)
    private Trivia trivia;

    @ManyToOne
    @JoinColumn(name = "fkusuario", nullable = false)
    private Usuario host;

    @Column(name = "max_usuario")
    private Integer maxUsuario = 50;

    @Column(name = "usuariosactuales")
    private Integer usuariosActuales = 0;

    @Column(name = "status", length = 20)
    private String status = "esperando"; // esperando, en_progreso, completada, cancelada

    @Column(name = "preguntaactual")
    private Integer preguntaActual = 0;

    @Column(name = "espublico")
    private Boolean esPublico = true;

    @Column(name = "unirsedespues")
    private Boolean unirseDespues = false;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "inicio")
    private LocalDateTime inicio;

    @Column(name = "finalizacion")
    private LocalDateTime finalizacion;

    // Constructor vacío (requerido por JPA)
    public Sala() {
    }

    // Constructor con campos obligatorios
    public Sala(String codigoSala, String nombreSala, Trivia trivia, Usuario host) {
        this.codigoSala = codigoSala;
        this.nombreSala = nombreSala;
        this.trivia = trivia;
        this.host = host;
    }

    // Callback antes de persistir
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // GETTERS Y SETTERS

    public Integer getSalaId() {
        return salaId;
    }

    public void setSalaId(Integer salaId) {
        this.salaId = salaId;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getNombreSala() {
        return nombreSala;
    }

    public void setNombreSala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public Trivia getTrivia() {
        return trivia;
    }

    public void setTrivia(Trivia trivia) {
        this.trivia = trivia;
    }

    public Usuario getHost() {
        return host;
    }

    public void setHost(Usuario host) {
        this.host = host;
    }

    public Integer getMaxUsuario() {
        return maxUsuario;
    }

    public void setMaxUsuario(Integer maxUsuario) {
        this.maxUsuario = maxUsuario;
    }

    public Integer getUsuariosActuales() {
        return usuariosActuales;
    }

    public void setUsuariosActuales(Integer usuariosActuales) {
        this.usuariosActuales = usuariosActuales;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPreguntaActual() {
        return preguntaActual;
    }

    public void setPreguntaActual(Integer preguntaActual) {
        this.preguntaActual = preguntaActual;
    }

    public Boolean getEsPublico() {
        return esPublico;
    }

    public void setEsPublico(Boolean esPublico) {
        this.esPublico = esPublico;
    }

    public Boolean getUnirseDespues() {
        return unirseDespues;
    }

    public void setUnirseDespues(Boolean unirseDespues) {
        this.unirseDespues = unirseDespues;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFinalizacion() {
        return finalizacion;
    }

    public void setFinalizacion(LocalDateTime finalizacion) {
        this.finalizacion = finalizacion;
    }

    @Override
    public String toString() {
        return "Sala{" +
                "salaId=" + salaId +
                ", codigoSala='" + codigoSala + '\'' +
                ", nombreSala='" + nombreSala + '\'' +
                ", status='" + status + '\'' +
                ", usuariosActuales=" + usuariosActuales +
                '}';
    }
}