package org.example.sparkytrivia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preguntas")
public class Preguntas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preguntaid")
    private Integer preguntaId;

    // Relación: Muchas preguntas pertenecen a UNA trivia
    @ManyToOne
    @JoinColumn(name = "fktrivia", nullable = false)
    private Trivia trivia;

    @Column(name = "orderpregunta", nullable = false)
    private Integer orderPregunta;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(length = 20)
    private String tipo = "opcion_multiple"; // opcion_multiple, verdadero_falso, abierta

    @Column
    private Integer puntos = 100;

    @Column(name = "limitetiempo")
    private Integer limiteTiempo = 30; // segundos

    @Column(length = 20)
    private String dificultad = "medio"; // facil, medio, dificil

    @Column(name = "imagenpregunta", length = 500)
    private String imagenPregunta;

    @Column(columnDefinition = "TEXT")
    private String explicacion;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    // Relación: Una pregunta tiene MUCHAS opciones de respuesta
    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpcionesRespuesta> opciones = new ArrayList<>();

    // ========== CONSTRUCTORES ==========

    public Preguntas() {
    }

    // Constructor con campos esenciales
    public Preguntas(Trivia trivia, Integer orderPregunta, String contenido) {
        this.trivia = trivia;
        this.orderPregunta = orderPregunta;
        this.contenido = contenido;
    }

    // ========== CALLBACKS JPA ==========

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    // ========== GETTERS Y SETTERS ==========

    public Integer getPreguntaId() {
        return preguntaId;
    }

    public void setPreguntaId(Integer preguntaId) {
        this.preguntaId = preguntaId;
    }

    public Trivia getTrivia() {
        return trivia;
    }

    public void setTrivia(Trivia trivia) {
        this.trivia = trivia;
    }

    public Integer getOrderPregunta() {
        return orderPregunta;
    }

    public void setOrderPregunta(Integer orderPregunta) {
        this.orderPregunta = orderPregunta;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }

    public Integer getLimiteTiempo() {
        return limiteTiempo;
    }

    public void setLimiteTiempo(Integer limiteTiempo) {
        this.limiteTiempo = limiteTiempo;
    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public String getImagenPregunta() {
        return imagenPregunta;
    }

    public void setImagenPregunta(String imagenPregunta) {
        this.imagenPregunta = imagenPregunta;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public void setExplicacion(String explicacion) {
        this.explicacion = explicacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<OpcionesRespuesta> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<OpcionesRespuesta> opciones) {
        this.opciones = opciones;
    }

    // ========== MÉTODOS ÚTILES ==========

    /**
     * Agregar una opción de respuesta a esta pregunta
     */
    public void agregarOpcion(OpcionesRespuesta opcion) {
        opciones.add(opcion);
        opcion.setPregunta(this);
    }

    /**
     * Remover una opción de respuesta
     */
    public void removerOpcion(OpcionesRespuesta opcion) {
        opciones.remove(opcion);
        opcion.setPregunta(null);
    }
}