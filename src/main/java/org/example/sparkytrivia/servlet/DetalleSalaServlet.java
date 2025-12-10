package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.service.SalaService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVLET REST PARA OBTENER DETALLES DE UNA SALA
 *
 * Endpoint: GET /api/salas/detalle?codigo=XY34AB
 * Protocolo: HTTP sobre TCP
 *
 * Response (JSON):
 * {
 *   "success": true,
 *   "sala": {
 *     "salaId": 1,
 *     "codigoSala": "XY34AB",
 *     "nombreSala": "Sala de Estudio",
 *     "status": "esperando",
 *     "usuariosActuales": 3,
 *     "maxUsuarios": 50,
 *     "trivia": { ... },
 *     "participantes": [ ... ]
 *   }
 * }
 */
@WebServlet(name = "DetalleSalaServlet", urlPatterns = {"/api/salas/detalle"})
public class DetalleSalaServlet extends HttpServlet {

    private SalaService salaService = new SalaService();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // VALIDAR SESIÓN
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // OBTENER CÓDIGO DE LA SALA DESDE QUERY PARAMS
            String codigoSala = request.getParameter("codigo");

            if (codigoSala == null || codigoSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Código de sala requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // OBTENER SALA
            Sala sala = salaService.obtenerSala(codigoSala.toUpperCase());

            // OBTENER PARTICIPANTES
            List<Participantes> participantes = salaService.listarParticipantes(sala.getSalaId());

            // CONSTRUIR RESPUESTA
            result.put("success", true);

            // Crear el mapa de la sala con HashMap
            Map<String, Object> salaData = new HashMap<>();
            salaData.put("salaId", sala.getSalaId());
            salaData.put("codigoSala", sala.getCodigoSala());
            salaData.put("nombreSala", sala.getNombreSala());
            salaData.put("status", sala.getStatus());
            salaData.put("usuariosActuales", sala.getUsuariosActuales());
            salaData.put("maxUsuarios", sala.getMaxUsuario());
            salaData.put("esPublico", sala.getEsPublico());
            salaData.put("unirseDespues", sala.getUnirseDespues());
            salaData.put("preguntaActual", sala.getPreguntaActual());

            // Datos de la trivia
            Map<String, Object> triviaData = new HashMap<>();
            triviaData.put("triviaId", sala.getTrivia().getTriviaId());
            triviaData.put("titulo", sala.getTrivia().getTitulo());
            triviaData.put("categoria", sala.getTrivia().getCategoria());
            triviaData.put("dificultad", sala.getTrivia().getDificultad());
            triviaData.put("preguntasTotales", sala.getTrivia().getPreguntasTotales());
            salaData.put("trivia", triviaData);

            // Datos del host
            Map<String, Object> hostData = new HashMap<>();
            hostData.put("usuarioId", sala.getHost().getUsuarioId());
            hostData.put("nickname", sala.getHost().getNickName());
            salaData.put("host", hostData);

            // Lista de participantes
            List<Map<String, Object>> participantesData = participantes.stream().map(p -> {
                Map<String, Object> participanteMap = new HashMap<>();
                participanteMap.put("participanteId", p.getParticipanteId());
                participanteMap.put("nickname", p.getNicknameJuego());
                participanteMap.put("esHost", p.getEsHost());
                participanteMap.put("puntajeFinal", p.getPuntajeFinal());
                participanteMap.put("esActivo", p.getEsActivo());
                return participanteMap;
            }).collect(Collectors.toList());
            salaData.put("participantes", participantesData);

            result.put("sala", salaData);

            response.setStatus(HttpServletResponse.SC_OK); // 200

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400

            System.err.println("Error al obtener detalles de sala: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}