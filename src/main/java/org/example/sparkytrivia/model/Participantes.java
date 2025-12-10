package org.example.sparkytrivia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participantes")
public class Participantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participanteid")
    private Integer participanteId;

    @ManyToOne
    @JoinColumn(name = "fksala", nullable = false)
    private Sala sala;

    @ManyToOne
    @JoinColumn(name = "fkusuario", nullable = false)
    private Usuario usuario;

    @Column(name = "nicknamejuego", length = 50)
    private String nicknameJuego;

    @Column(name = "unio")
    private LocalDateTime unio;

    @Column(name = "abandono")
    private LocalDateTime abandono;

    @Column(name = "esactivo")
    private Boolean esActivo = true;

    @Column(name = "eshost")
    private Boolean esHost = false;

    @Column(name = "puntajefinal")
    private Integer puntajeFinal = 0;

    @Column(name = "rangofinal")
    private Integer rangoFinal;

    @Column(name = "preguntarespuesta")
    private Integer preguntaRespuesta = 0;

    @Column(name = "preguntacorrecta")
    private Integer preguntaCorrecta = 0;

    // Constructor vacío
    public Participantes() {
    }

    // Constructor con campos obligatorios
    public Participantes(Sala sala, Usuario usuario, Boolean esHost) {
        this.sala = sala;
        this.usuario = usuario;
        this.esHost = esHost;
        this.nicknameJuego = usuario.getNickName();
    }

    @PrePersist
    protected void onCreate() {
        this.unio = LocalDateTime.now();
    }

    // GETTERS Y SETTERS COMPLETOS
    public Integer getParticipanteId() {
        return participanteId;
    }

    public void setParticipanteId(Integer participanteId) {
        this.participanteId = participanteId;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNicknameJuego() {
        return nicknameJuego;
    }

    public void setNicknameJuego(String nicknameJuego) {
        this.nicknameJuego = nicknameJuego;
    }

    public LocalDateTime getUnio() {
        return unio;
    }

    public void setUnio(LocalDateTime unio) {
        this.unio = unio;
    }

    public LocalDateTime getAbandono() {
        return abandono;
    }

    public void setAbandono(LocalDateTime abandono) {
        this.abandono = abandono;
    }

    public Boolean getEsActivo() {
        return esActivo;
    }

    public void setEsActivo(Boolean esActivo) {
        this.esActivo = esActivo;
    }

    public Boolean getEsHost() {
        return esHost;
    }

    public void setEsHost(Boolean esHost) {
        this.esHost = esHost;
    }

    public Integer getPuntajeFinal() {
        return puntajeFinal;
    }

    public void setPuntajeFinal(Integer puntajeFinal) {
        this.puntajeFinal = puntajeFinal;
    }

    public Integer getRangoFinal() {
        return rangoFinal;
    }

    public void setRangoFinal(Integer rangoFinal) {
        this.rangoFinal = rangoFinal;
    }

    public Integer getPreguntaRespuesta() {
        return preguntaRespuesta;
    }

    public void setPreguntaRespuesta(Integer preguntaRespuesta) {
        this.preguntaRespuesta = preguntaRespuesta;
    }

    public Integer getPreguntaCorrecta() {
        return preguntaCorrecta;
    }

    public void setPreguntaCorrecta(Integer preguntaCorrecta) {
        this.preguntaCorrecta = preguntaCorrecta;
    }
}