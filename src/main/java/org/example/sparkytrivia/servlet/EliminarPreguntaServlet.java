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
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet para eliminar una pregunta
 * Endpoint: DELETE /api/preguntas/{id}
 */
@WebServlet(name = "EliminarPreguntaServlet", urlPatterns = {"/api/preguntas/eliminar/*"})
public class EliminarPreguntaServlet extends HttpServlet {

    private PreguntaService preguntaService = new PreguntaService();
    private Gson gson = new Gson();

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
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

            // Extraer ID de la pregunta
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                result.put("success", false);
                result.put("message", "ID de pregunta no especificado");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer preguntaId;
            try {
                preguntaId = Integer.parseInt(pathInfo.substring(1));
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("message", "ID de pregunta inválido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Eliminar pregunta
            preguntaService.eliminarPregunta(preguntaId, usuarioId);

            result.put("success", true);
            result.put("message", "Pregunta eliminada exitosamente");
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error interno del servidor");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
    }
}