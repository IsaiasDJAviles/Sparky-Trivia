package org.example.sparkytrivia.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity//indica que esta clase es una entidad JPA
@Table(name = "trivia") //mapea a la tabla trivia
public class Trivia {

    //ATRIBUTOS
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)//autoincremento de postgres
    @Column(name = "triviaid")
    private Integer triviaId;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne// Muchas trivias pertenecen a un usuario
    @JoinColumn(name = "fkhostuser", nullable = false)
    private Usuario host;//usuaurioa que creo la trivia
    @Column(length = 50)
    private String categoria = "general";

    @Column(length = 20)
    private String dificultad = "medio";

    @Column(name = "preguntastotales")
    private Integer preguntasTotales = 0; // Lo actualizaremos en Java

    @Column(name = "tiempoestimado")
    private Integer tiempoEstimado = 15; // Minutos

    @Column(name = "fotoportada", length = 500)
    private String fotoPortada;

    @Column(name = "espublico")
    private Boolean esPublico = true;

    @Column(length = 20)
    private String status = "borrador"; // borrador, activo, archivado

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fechaactualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "vecesjugada")
    private Integer vecesJugada = 0;

    // Constructor vacío (requerido por JPA)
    public Trivia() {
    }

    // Constructor con campos esenciales
    public Trivia(String titulo, String descripcion, Usuario host) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.host = host;
    }

    @PrePersist // Antes de INSERT
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate // Antes de UPDATE
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters

    public Integer getTriviaId() {
        return triviaId;
    }

    public void setTriviaId(Integer triviaId) {
        this.triviaId = triviaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Usuario getHost() {
        return host;
    }

    public void setHost(Usuario host) {
        this.host = host;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public Integer getPreguntasTotales() {
        return preguntasTotales;
    }

    public void setPreguntasTotales(Integer preguntasTotales) {
        this.preguntasTotales = preguntasTotales;
    }

    public Integer getTiempoEstimado() {
        return tiempoEstimado;
    }

    public void setTiempoEstimado(Integer tiempoEstimado) {
        this.tiempoEstimado = tiempoEstimado;
    }

    public String getFotoPortada() {
        return fotoPortada;
    }

    public void setFotoPortada(String fotoPortada) {
        this.fotoPortada = fotoPortada;
    }

    public Boolean getEsPublico() {
        return esPublico;
    }

    public void setEsPublico(Boolean esPublico) {
        this.esPublico = esPublico;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Integer getVecesJugada() {
        return vecesJugada;
    }

    public void setVecesJugada(Integer vecesJugada) {
        this.vecesJugada = vecesJugada;
    }
}