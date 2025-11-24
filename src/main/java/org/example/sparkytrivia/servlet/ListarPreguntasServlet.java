package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sparkytrivia.model.OpcionesRespuesta;
import org.example.sparkytrivia.model.Preguntas;
import org.example.sparkytrivia.service.PreguntaService;

import java.io.IOException;
import java.util.*;

/**
 * Servlet para listar todas las preguntas de una trivia
 * Endpoint: GET /api/trivias/{id}/preguntas
 */
@WebServlet(name = "ListarPreguntasServlet", urlPatterns = {"/api/trivias/preguntas"})
public class ListarPreguntasServlet extends HttpServlet {

    private PreguntaService preguntaService = new PreguntaService();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // Obtener triviaId del parámetro de query string
            String triviaIdParam = request.getParameter("triviaId");

            if (triviaIdParam == null || triviaIdParam.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "ID de trivia no especificado");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer triviaId;
            try {
                triviaId = Integer.parseInt(triviaIdParam);
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("message", "ID de trivia inválido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Listar preguntas de la trivia
            List<Preguntas> preguntas = preguntaService.listarPreguntasPorTrivia(triviaId);

            // Convertir a JSON limpio
            List<Map<String, Object>> preguntasJson = new ArrayList<>();

            for (Preguntas pregunta : preguntas) {
                Map<String, Object> preguntaMap = new HashMap<>();

                preguntaMap.put("preguntaId", pregunta.getPreguntaId());
                preguntaMap.put("orderPregunta", pregunta.getOrderPregunta());
                preguntaMap.put("contenido", pregunta.getContenido());
                preguntaMap.put("tipo", pregunta.getTipo());
                preguntaMap.put("puntos", pregunta.getPuntos());
                preguntaMap.put("limiteTiempo", pregunta.getLimiteTiempo());
                preguntaMap.put("dificultad", pregunta.getDificultad());
                preguntaMap.put("imagenPregunta", pregunta.getImagenPregunta());
                preguntaMap.put("explicacion", pregunta.getExplicacion());

                // Convertir opciones de respuesta
                List<Map<String, Object>> opcionesJson = new ArrayList<>();
                for (OpcionesRespuesta opcion : pregunta.getOpciones()) {
                    Map<String, Object> opcionMap = new HashMap<>();
                    opcionMap.put("opcionId", opcion.getOpcionId());
                    opcionMap.put("orderPregunta", opcion.getOrderPregunta());
                    opcionMap.put("textoOpcion", opcion.getTextoOpcion());
                    opcionMap.put("isCorrecto", opcion.getIsCorrecto());
                    opcionesJson.add(opcionMap);
                }

                preguntaMap.put("opciones", opcionesJson);
                preguntasJson.add(preguntaMap);
            }

            // Respuesta exitosa
            result.put("success", true);
            result.put("preguntas", preguntasJson);
            result.put("total", preguntas.size());

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