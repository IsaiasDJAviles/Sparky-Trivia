package org.example.sparkytrivia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "respuestasjugador")
public class RespuestasJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "respuestaid")
    private Integer respuestaId;

    @ManyToOne
    @JoinColumn(name = "fkparticipante", nullable = false)
    private Participantes participante;

    @ManyToOne
    @JoinColumn(name = "fkpregunta", nullable = false)
    private Preguntas pregunta;

    @ManyToOne
    @JoinColumn(name = "fksala", nullable = false)
    private Sala sala;

    @ManyToOne
    @JoinColumn(name = "opcionseleccionada")
    private OpcionesRespuesta opcionSeleccionada;

    @Column(name = "escorrecta")
    private Boolean esCorrecta;

    @Column(name = "tiempotomado")
    private Integer tiempoTomado; // En segundos

    @Column(name = "puntosganados")
    private Integer puntosGanados = 0;

    @Column(name = "respondioen")
    private LocalDateTime respondioEn;

    // Constructor vacío (requerido por JPA)
    public RespuestasJugador() {
    }

    // Constructor con campos obligatorios
    public RespuestasJugador(Participantes participante, Preguntas pregunta, Sala sala) {
        this.participante = participante;
        this.pregunta = pregunta;
        this.sala = sala;
    }

    // Callback antes de persistir
    @PrePersist
    protected void onCreate() {
        this.respondioEn = LocalDateTime.now();
    }

    // GETTERS Y SETTERS

    public Integer getRespuestaId() {
        return respuestaId;
    }

    public void setRespuestaId(Integer respuestaId) {
        this.respuestaId = respuestaId;
    }

    public Participantes getParticipante() {
        return participante;
    }

    public void setParticipante(Participantes participante) {
        this.participante = participante;
    }

    public Preguntas getPregunta() {
        return pregunta;
    }

    public void setPregunta(Preguntas pregunta) {
        this.pregunta = pregunta;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public OpcionesRespuesta getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public void setOpcionSeleccionada(OpcionesRespuesta opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(Boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }

    public Integer getTiempoTomado() {
        return tiempoTomado;
    }

    public void setTiempoTomado(Integer tiempoTomado) {
        this.tiempoTomado = tiempoTomado;
    }

    public Integer getPuntosGanados() {
        return puntosGanados;
    }

    public void setPuntosGanados(Integer puntosGanados) {
        this.puntosGanados = puntosGanados;
    }

    public LocalDateTime getRespondioEn() {
        return respondioEn;
    }

    public void setRespondioEn(LocalDateTime respondioEn) {
        this.respondioEn = respondioEn;
    }

    @Override
    public String toString() {
        return "RespuestasJugador{" +
                "respuestaId=" + respuestaId +
                ", esCorrecta=" + esCorrecta +
                ", tiempoTomado=" + tiempoTomado +
                ", puntosGanados=" + puntosGanados +
                '}';
    }
}