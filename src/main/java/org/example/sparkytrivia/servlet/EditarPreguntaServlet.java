package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.OpcionesRespuesta;
import org.example.sparkytrivia.model.Preguntas;
import org.example.sparkytrivia.service.PreguntaService;

import java.io.IOException;
import java.util.*;

/**
 * Servlet para actualizar una pregunta existente
 * Endpoint: PUT /api/preguntas/{id}
 */
@WebServlet(name = "EditarPreguntaServlet", urlPatterns = {"/api/preguntas/editar/*"})
public class EditarPreguntaServlet extends HttpServlet {

    private PreguntaService preguntaService = new PreguntaService();
    private Gson gson = new Gson();

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
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

            // Extraer ID de la pregunta de la URL
            String pathInfo = request.getPathInfo(); // /5
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

            // Leer datos JSON
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            String contenido = (String) datos.get("contenido");
            String tipo = (String) datos.get("tipo");
            Integer puntos = datos.get("puntos") != null ?
                    ((Double) datos.get("puntos")).intValue() : null;
            Integer limiteTiempo = datos.get("limiteTiempo") != null ?
                    ((Double) datos.get("limiteTiempo")).intValue() : null;
            String dificultad = (String) datos.get("dificultad");
            String imagenPregunta = (String) datos.get("imagenPregunta");
            String explicacion = (String) datos.get("explicacion");

            // Opciones (opcional)
            List<PreguntaService.OpcionRespuestaDTO> opciones = null;
            if (datos.containsKey("opciones")) {
                List<Map<String, Object>> opcionesRaw =
                        (List<Map<String, Object>>) datos.get("opciones");

                opciones = new ArrayList<>();
                for (Map<String, Object> opcionRaw : opcionesRaw) {
                    String textoOpcion = (String) opcionRaw.get("textoOpcion");
                    Boolean isCorrecto = (Boolean) opcionRaw.get("isCorrecto");
                    opciones.add(new PreguntaService.OpcionRespuestaDTO(textoOpcion, isCorrecto));
                }
            }

            // Actualizar pregunta
            Preguntas pregunta = preguntaService.actualizarPregunta(
                    preguntaId,
                    usuarioId,
                    contenido,
                    tipo,
                    puntos,
                    limiteTiempo,
                    dificultad,
                    imagenPregunta,
                    explicacion,
                    opciones
            );

            // Construir respuesta
            Map<String, Object> preguntaMap = new HashMap<>();
            preguntaMap.put("preguntaId", pregunta.getPreguntaId());
            preguntaMap.put("contenido", pregunta.getContenido());
            preguntaMap.put("tipo", pregunta.getTipo());
            preguntaMap.put("puntos", pregunta.getPuntos());
            preguntaMap.put("limiteTiempo", pregunta.getLimiteTiempo());
            preguntaMap.put("dificultad", pregunta.getDificultad());

            result.put("success", true);
            result.put("message", "Pregunta actualizada exitosamente");
            result.put("pregunta", preguntaMap);

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