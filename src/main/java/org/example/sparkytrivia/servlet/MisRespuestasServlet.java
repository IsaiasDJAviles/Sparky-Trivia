package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.dao.ParticipantesDAO;
import org.example.sparkytrivia.dao.RespuestasJugadorDAO;
import org.example.sparkytrivia.dao.SalaDAO;
import org.example.sparkytrivia.dao.OpcionesRespuestaDAO;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.RespuestasJugador;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.model.OpcionesRespuesta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVLET PARA OBTENER MIS RESPUESTAS EN UNA SALA - VERSION MEJORADA
 *
 * Endpoint: GET /api/salas/mis-respuestas?codigo=XY34AB
 *
 * MEJORAS:
 * - Mejor logging para depuracion
 * - Manejo de errores mejorado
 * - Busqueda de respuesta correcta mas robusta
 */
@WebServlet(name = "MisRespuestasServlet", urlPatterns = {"/api/salas/mis-respuestas"})
public class MisRespuestasServlet extends HttpServlet {

    private SalaDAO salaDAO = new SalaDAO();
    private ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private RespuestasJugadorDAO respuestasDAO = new RespuestasJugadorDAO();
    private OpcionesRespuestaDAO opcionesDAO = new OpcionesRespuestaDAO();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesion");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");
            String codigoSala = request.getParameter("codigo");

            System.out.println("[MisRespuestas] Usuario ID: " + usuarioId);
            System.out.println("[MisRespuestas] Codigo sala: " + codigoSala);

            if (codigoSala == null || codigoSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Codigo de sala requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Sala sala = salaDAO.buscarPorCodigo(codigoSala.toUpperCase());

            if (sala == null) {
                System.out.println("[MisRespuestas] Sala no encontrada: " + codigoSala);
                result.put("success", false);
                result.put("message", "Sala no encontrada");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            System.out.println("[MisRespuestas] Sala encontrada: " + sala.getSalaId());

            // Buscar mi participacion en esta sala
            Participantes miParticipacion = participantesDAO.buscarParticipante(
                    sala.getSalaId(),
                    usuarioId
            );

            if (miParticipacion == null) {
                System.out.println("[MisRespuestas] Usuario no participo en esta sala");
                result.put("success", false);
                result.put("message", "No participaste en esta sala");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            System.out.println("[MisRespuestas] Participante ID: " + miParticipacion.getParticipanteId());
            System.out.println("[MisRespuestas] Nickname: " + miParticipacion.getNicknameJuego());

            // Obtener todas mis respuestas
            List<RespuestasJugador> respuestas = respuestasDAO.listarPorParticipante(
                    miParticipacion.getParticipanteId()
            );

            System.out.println("[MisRespuestas] Respuestas encontradas: " +
                    (respuestas != null ? respuestas.size() : 0));

            if (respuestas == null || respuestas.isEmpty()) {
                result.put("success", true);
                result.put("respuestas", new ArrayList<>());
                result.put("message", "No se encontraron respuestas registradas para este participante");
                result.put("debug", Map.of(
                        "participanteId", miParticipacion.getParticipanteId(),
                        "salaId", sala.getSalaId(),
                        "usuarioId", usuarioId
                ));
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Convertir a JSON con toda la informacion
            List<Map<String, Object>> respuestasJson = new ArrayList<>();

            for (RespuestasJugador r : respuestas) {
                try {
                    Map<String, Object> respuestaMap = new HashMap<>();

                    // Pregunta
                    String contenidoPregunta = "Pregunta no disponible";
                    if (r.getPregunta() != null) {
                        contenidoPregunta = r.getPregunta().getContenido();
                    }
                    respuestaMap.put("pregunta", contenidoPregunta);

                    // Tu respuesta
                    String tuRespuesta = "No respondida";
                    if (r.getOpcionSeleccionada() != null) {
                        tuRespuesta = r.getOpcionSeleccionada().getTextoOpcion();
                    }
                    respuestaMap.put("tuRespuesta", tuRespuesta);

                    // Es correcta
                    respuestaMap.put("esCorrecta", r.getEsCorrecta() != null ? r.getEsCorrecta() : false);

                    // Puntos ganados
                    respuestaMap.put("puntosGanados", r.getPuntosGanados() != null ? r.getPuntosGanados() : 0);

                    // Tiempo tomado
                    respuestaMap.put("tiempoTomado", r.getTiempoTomado() != null ? r.getTiempoTomado() : 0);

                    // Buscar la respuesta correcta
                    String respuestaCorrecta = "Desconocida";
                    if (r.getPregunta() != null) {
                        // Buscar opciones de la pregunta
                        List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(
                                r.getPregunta().getPreguntaId()
                        );

                        for (OpcionesRespuesta op : opciones) {
                            if (op.getIsCorrecto() != null && op.getIsCorrecto()) {
                                respuestaCorrecta = op.getTextoOpcion();
                                break;
                            }
                        }
                    }
                    respuestaMap.put("respuestaCorrecta", respuestaCorrecta);

                    respuestasJson.add(respuestaMap);

                    System.out.println("[MisRespuestas] Respuesta procesada: " + contenidoPregunta +
                            " -> " + tuRespuesta + " (" + (r.getEsCorrecta() ? "CORRECTA" : "INCORRECTA") + ")");

                } catch (Exception e) {
                    System.err.println("[MisRespuestas] Error procesando respuesta: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            result.put("success", true);
            result.put("respuestas", respuestasJson);
            result.put("total", respuestasJson.size());
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            System.err.println("[MisRespuestas] Error general: " + e.getMessage());
            e.printStackTrace();

            result.put("success", false);
            result.put("message", "Error interno: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
    }
}