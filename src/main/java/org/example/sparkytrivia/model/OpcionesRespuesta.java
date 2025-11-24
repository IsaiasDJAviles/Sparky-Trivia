package org.example.sparkytrivia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "opcionesrespuesta")
public class OpcionesRespuesta {

    // ========== ATRIBUTOS ==========

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opcionid")
    private Integer opcionId;

    // Relación: Muchas opciones pertenecen a UNA pregunta
    @ManyToOne
    @JoinColumn(name = "fkpregunta", nullable = false)
    private Preguntas pregunta;

    @Column(name = "orderpregunta", nullable = false)
    private Integer orderPregunta; // Orden: 1, 2, 3, 4 (a, b, c, d)

    @Column(name = "textoopcion", columnDefinition = "TEXT", nullable = false)
    private String textoOpcion;

    @Column(name = "iscorrecto")
    private Boolean isCorrecto = false;

    // ========== CONSTRUCTORES ==========

    public OpcionesRespuesta() {
    }

    // Constructor con campos esenciales
    public OpcionesRespuesta(Preguntas pregunta, Integer orderPregunta, String textoOpcion, Boolean isCorrecto) {
        this.pregunta = pregunta;
        this.orderPregunta = orderPregunta;
        this.textoOpcion = textoOpcion;
        this.isCorrecto = isCorrecto;
    }

    // ========== GETTERS Y SETTERS ==========

    public Integer getOpcionId() {
        return opcionId;
    }

    public void setOpcionId(Integer opcionId) {
        this.opcionId = opcionId;
    }

    public Preguntas getPregunta() {
        return pregunta;
    }

    public void setPregunta(Preguntas pregunta) {
        this.pregunta = pregunta;
    }

    public Integer getOrderPregunta() {
        return orderPregunta;
    }

    public void setOrderPregunta(Integer orderPregunta) {
        this.orderPregunta = orderPregunta;
    }

    public String getTextoOpcion() {
        return textoOpcion;
    }

    public void setTextoOpcion(String textoOpcion) {
        this.textoOpcion = textoOpcion;
    }

    public Boolean getIsCorrecto() {
        return isCorrecto;
    }

    public void setIsCorrecto(Boolean isCorrecto) {
        this.isCorrecto = isCorrecto;
    }
}