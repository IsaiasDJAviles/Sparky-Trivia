package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.dao.ParticipantesDAO;
import org.example.sparkytrivia.dao.SalaDAO;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.service.PuntajeService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SERVLET PARA OBTENER RESULTADOS FINALES DE UNA SALA
 *
 * Endpoint: GET /api/salas/resultados?codigo=XY34AB
 */
@WebServlet(name = "ResultadosSalaServlet", urlPatterns = {"/api/salas/resultados"})
public class ResultadosSalaServlet extends HttpServlet {

    private SalaDAO salaDAO = new SalaDAO();
    private ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private PuntajeService puntajeService = new PuntajeService();
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

            // Obtener ranking final
            List<Participantes> ranking = puntajeService.obtenerRankingActual(sala.getSalaId());

            // Convertir a JSON
            List<Map<String, Object>> rankingJson = ranking.stream().map(p -> {
                Map<String, Object> participante = new HashMap<>();
                participante.put("rangoFinal", p.getRangoFinal());
                participante.put("nickname", p.getNicknameJuego());
                participante.put("puntajeFinal", p.getPuntajeFinal());
                participante.put("correctas", p.getPreguntaCorrecta());
                participante.put("respondidas", p.getPreguntaRespuesta());

                double precision = puntajeService.calcularPrecision(p.getParticipanteId());
                participante.put("precision", precision);

                return participante;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("ranking", rankingJson);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            System.err.println("Error obteniendo resultados: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}