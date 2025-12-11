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
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.RespuestasJugador;
import org.example.sparkytrivia.model.Sala;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SERVLET PARA OBTENER MIS RESPUESTAS EN UNA SALA
 *
 * Endpoint: GET /api/salas/mis-respuestas?codigo=XY34AB
 */
@WebServlet(name = "MisRespuestasServlet", urlPatterns = {"/api/salas/mis-respuestas"})
public class MisRespuestasServlet extends HttpServlet {

    private SalaDAO salaDAO = new SalaDAO();
    private ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private RespuestasJugadorDAO respuestasDAO = new RespuestasJugadorDAO();
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
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");
            String codigoSala = request.getParameter("codigo");

            if (codigoSala == null || codigoSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Código de sala requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Sala sala = salaDAO.buscarPorCodigo(codigoSala.toUpperCase());

            if (sala == null) {
                result.put("success", false);
                result.put("message", "Sala no encontrada");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Buscar mi participación en esta sala
            Participantes miParticipacion = participantesDAO.buscarParticipante(
                    sala.getSalaId(),
                    usuarioId
            );

            if (miParticipacion == null) {
                result.put("success", false);
                result.put("message", "No participaste en esta sala");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Obtener todas mis respuestas
            List<RespuestasJugador> respuestas = respuestasDAO.listarPorParticipante(
                    miParticipacion.getParticipanteId()
            );

            // Convertir a JSON con toda la información
            List<Map<String, Object>> respuestasJson = respuestas.stream().map(r -> {
                Map<String, Object> respuesta = new HashMap<>();

                respuesta.put("pregunta", r.getPregunta().getContenido());
                respuesta.put("tuRespuesta", r.getOpcionSeleccionada() != null
                        ? r.getOpcionSeleccionada().getTextoOpcion()
                        : "No respondida");
                respuesta.put("esCorrecta", r.getEsCorrecta());
                respuesta.put("puntosGanados", r.getPuntosGanados());
                respuesta.put("tiempoTomado", r.getTiempoTomado());

                // Buscar la respuesta correcta
                String respuestaCorrecta = r.getPregunta().getOpciones().stream()
                        .filter(op -> op.getIsCorrecto())
                        .map(op -> op.getTextoOpcion())
                        .findFirst()
                        .orElse("Desconocida");

                respuesta.put("respuestaCorrecta", respuestaCorrecta);

                return respuesta;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("respuestas", respuestasJson);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            System.err.println("Error obteniendo respuestas: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}