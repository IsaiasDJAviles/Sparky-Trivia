package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.service.PreguntaService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servlet para reordenar preguntas de una trivia
 * Endpoint: POST /api/trivias/{id}/preguntas/reordenar
 */
@WebServlet(name = "ReordenarPreguntasServlet", urlPatterns = {"/api/preguntas/reordenar"})
public class ReordenarPreguntasServlet extends HttpServlet {

    private PreguntaService preguntaService = new PreguntaService();
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // Verificar sesión
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // Leer datos JSON
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            // Obtener triviaId
            Integer triviaId = null;
            if (datos.get("triviaId") != null) {
                triviaId = ((Double) datos.get("triviaId")).intValue();
            }

            if (triviaId == null) {
                result.put("success", false);
                result.put("message", "ID de trivia es obligatorio");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Obtener nuevo orden (array de IDs de preguntas)
            List<Double> nuevoOrdenRaw = (List<Double>) datos.get("nuevoOrden");

            if (nuevoOrdenRaw == null || nuevoOrdenRaw.isEmpty()) {
                result.put("success", false);
                result.put("message", "Debe especificar el nuevo orden");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Convertir de Double a Integer
            List<Integer> nuevoOrden = nuevoOrdenRaw.stream()
                    .map(Double::intValue)
                    .collect(Collectors.toList());

            // Reordenar
            preguntaService.reordenarPreguntas(triviaId, usuarioId, nuevoOrden);

            result.put("success", true);
            result.put("message", "Preguntas reordenadas exitosamente");
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error interno del servidor");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}