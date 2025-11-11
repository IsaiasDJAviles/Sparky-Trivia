package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sparkytrivia.model.Trivia;
import org.example.sparkytrivia.service.TriviaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//SERVLET PARA OBTENER DETALLE DE UN TRIVIA ESPECIFICA
@WebServlet(name = "DetalleTriviaServlet", urlPatterns = {"/api/trivias/*"})
public class DetalleTriviaServlet extends HttpServlet {

    // Servicio para lógica de negocio
    private TriviaService triviaService = new TriviaService();

    // Gson para JSON
    private Gson gson = new Gson();

    // se manejan las peticiones get para obtenr detallles de una trivia
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Map para construir respuesta
        Map<String, Object> result = new HashMap<>();

        try {
            // PASO 1: Extraer el ID de la URL
            // URL ejemplo: /api/trivias/1
            // getPathInfo() devuelve: "/1"
            String pathInfo = request.getPathInfo();

            // Validar que se envió un ID
            if (pathInfo == null || pathInfo.equals("/")) {
                result.put("success", false);
                result.put("message", "ID de trivia no especificado");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            //convertir el ID de String a Integer
            String idStr = pathInfo.substring(1);
            Integer triviaId;

            try {
                triviaId = Integer.parseInt(idStr); // Convertir a número
            } catch (NumberFormatException e) {
                // Si no es un número válido
                result.put("success", false);
                result.put("message", "ID de trivia inválido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            //buscar la trivia por ID
            Trivia trivia = triviaService.obtenerTrivia(triviaId);

            //construir respuesta con toda la información
            Map<String, Object> triviaMap = new HashMap<>();

            triviaMap.put("triviaId", trivia.getTriviaId());
            triviaMap.put("titulo", trivia.getTitulo());
            triviaMap.put("descripcion", trivia.getDescripcion());
            triviaMap.put("categoria", trivia.getCategoria());
            triviaMap.put("dificultad", trivia.getDificultad());
            triviaMap.put("preguntasTotales", trivia.getPreguntasTotales());
            triviaMap.put("tiempoEstimado", trivia.getTiempoEstimado());
            triviaMap.put("fotoPortada", trivia.getFotoPortada());
            triviaMap.put("status", trivia.getStatus());
            triviaMap.put("esPublico", trivia.getEsPublico());
            triviaMap.put("vecesJugada", trivia.getVecesJugada());

            // Información del host
            triviaMap.put("host", Map.of(
                    "usuarioId", trivia.getHost().getUsuarioId(),
                    "nickName", trivia.getHost().getNickName(),
                    "firstName", trivia.getHost().getFirstName(),
                    "lastName", trivia.getHost().getLastName()
            ));

            // Fechas
            triviaMap.put("fechaCreacion", trivia.getFechaCreacion().toString());
            triviaMap.put("fechaActualizacion", trivia.getFechaActualizacion().toString());

            //construir respuesta exitosa
            result.put("success", true);
            result.put("trivia", triviaMap);

            // Código HTTP 200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            // Si la trivia no existe o hubo error
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
        } catch (Exception e) {
            // Error inesperado
            result.put("success", false);
            result.put("message", "Error interno del servidor");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
        }

        // Enviar respuesta JSON al cliente
        response.getWriter().write(gson.toJson(result));
    }
}