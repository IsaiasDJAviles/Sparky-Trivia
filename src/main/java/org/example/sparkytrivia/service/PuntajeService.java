package org.example.sparkytrivia.service;

import org.example.sparkytrivia.dao.ParticipantesDAO;
import org.example.sparkytrivia.model.Participantes;

import java.util.List;

public class PuntajeService {

    private ParticipantesDAO participantesDAO = new ParticipantesDAO();

    /**
     * CALCULAR PUNTAJE SEGÚN TIEMPO DE RESPUESTA
     *
     * Fórmula progresiva:
     * - 0-5 segundos   → 100% del puntaje base
     * - 6-10 segundos  → 90% del puntaje base
     * - 11-15 segundos → 80% del puntaje base
     * - 16-20 segundos → 70% del puntaje base
     * - 21-25 segundos → 60% del puntaje base
     * - 26-30 segundos → 50% del puntaje base
     * - 31+ segundos   → 40% del puntaje base
     *
     * @param tiempoRespuesta Tiempo en segundos que tardó en responder
     * @param puntosBase Puntos base de la pregunta (ej: 100)
     * @param limiteTiempo Tiempo límite de la pregunta (ej: 30)
     * @return Puntos calculados
     */
    public int calcularPuntaje(int tiempoRespuesta, int puntosBase, int limiteTiempo) {

        // Si no respondió o se pasó del tiempo, 0 puntos
        if (tiempoRespuesta <= 0 || tiempoRespuesta > limiteTiempo) {
            return 0;
        }

        // Calcular porcentaje según tiempo
        double porcentaje;

        if (tiempoRespuesta <= 5) {
            porcentaje = 1.0; // 100%
        } else if (tiempoRespuesta <= 10) {
            porcentaje = 0.9; // 90%
        } else if (tiempoRespuesta <= 15) {
            porcentaje = 0.8; // 80%
        } else if (tiempoRespuesta <= 20) {
            porcentaje = 0.7; // 70%
        } else if (tiempoRespuesta <= 25) {
            porcentaje = 0.6; // 60%
        } else if (tiempoRespuesta <= 30) {
            porcentaje = 0.5; // 50%
        } else {
            porcentaje = 0.4; // 40%
        }

        // Calcular puntos finales
        int puntosFinales = (int) Math.round(puntosBase * porcentaje);

        return puntosFinales;
    }

    /**
     * CALCULAR PUNTAJE CON FÓRMULA LINEAL
     * Alternativa: decremento lineal según tiempo
     *
     * Puntos = PuntosBase * (1 - (TiempoRespuesta / LimiteTiempo) * 0.5)
     *
     * Ejemplo: Si responde a la mitad del tiempo, obtiene 75% de puntos
     */
    public int calcularPuntajeLineal(int tiempoRespuesta, int puntosBase, int limiteTiempo) {

        if (tiempoRespuesta <= 0 || tiempoRespuesta > limiteTiempo) {
            return 0;
        }

        // Fórmula lineal: mientras más rápido, más puntos
        double factor = 1.0 - ((double) tiempoRespuesta / limiteTiempo * 0.5);
        int puntosFinales = (int) Math.round(puntosBase * factor);

        // Mínimo 10% del puntaje base
        int minimo = (int) (puntosBase * 0.1);
        return Math.max(puntosFinales, minimo);
    }

    /**
     * CALCULAR RANKING FINAL DE UNA SALA
     * Ordena a todos los participantes por puntaje y asigna posiciones
     *
     * @param salaId ID de la sala
     */
    public void calcularRanking(Integer salaId) {

        // Obtener todos los participantes ordenados por puntaje
        List<Participantes> participantes = participantesDAO.obtenerRanking(salaId);

        // Asignar rangos (1, 2, 3, etc.)
        int rango = 1;
        for (Participantes participante : participantes) {
            participante.setRangoFinal(rango);
            participantesDAO.actualizar(participante);
            rango++;
        }
    }

    /**
     * ACTUALIZAR PUNTAJE DE UN PARTICIPANTE
     * Suma puntos ganados en la pregunta actual al puntaje total
     *
     * @param participanteId ID del participante
     * @param puntosGanados Puntos de esta respuesta
     */
    public void actualizarPuntaje(Integer participanteId, int puntosGanados) {

        Participantes participante = participantesDAO.buscarPorId(participanteId);
        if (participante == null) {
            throw new RuntimeException("Participante no encontrado");
        }

        // Sumar puntos al total
        int puntajeActual = participante.getPuntajeFinal();
        participante.setPuntajeFinal(puntajeActual + puntosGanados);

        participantesDAO.actualizar(participante);
    }

    /**
     * INCREMENTAR CONTADOR DE RESPUESTAS
     *
     * @param participanteId ID del participante
     * @param esCorrecta Si la respuesta fue correcta
     */
    public void registrarRespuesta(Integer participanteId, boolean esCorrecta) {

        Participantes participante = participantesDAO.buscarPorId(participanteId);
        if (participante == null) {
            throw new RuntimeException("Participante no encontrado");
        }

        // Incrementar contador de preguntas respondidas
        participante.setPreguntaRespuesta(participante.getPreguntaRespuesta() + 1);

        // Si es correcta, incrementar contador de correctas
        if (esCorrecta) {
            participante.setPreguntaCorrecta(participante.getPreguntaCorrecta() + 1);
        }

        participantesDAO.actualizar(participante);
    }

    /**
     * CALCULAR PRECISIÓN DE UN PARTICIPANTE
     *
     * @param participanteId ID del participante
     * @return Porcentaje de precisión (0-100)
     */
    public double calcularPrecision(Integer participanteId) {

        Participantes participante = participantesDAO.buscarPorId(participanteId);
        if (participante == null) {
            return 0.0;
        }

        int total = participante.getPreguntaRespuesta();
        int correctas = participante.getPreguntaCorrecta();

        if (total == 0) {
            return 0.0;
        }

        return ((double) correctas / total) * 100.0;
    }

    /**
     * OBTENER RANKING ACTUAL DE UNA SALA
     * Útil para mostrar en tiempo real durante el juego
     */
    public List<Participantes> obtenerRankingActual(Integer salaId) {
        return participantesDAO.obtenerRanking(salaId);
    }
}