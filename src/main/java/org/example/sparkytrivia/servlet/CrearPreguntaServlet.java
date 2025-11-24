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
 * Servlet para crear una nueva pregunta con sus opciones de respuesta
 * Endpoint: POST /api/preguntas/crear
 */
@WebServlet(name = "CrearPreguntaServlet", urlPatterns = {"/api/preguntas/crear"})
public class CrearPreguntaServlet extends HttpServlet {

    private PreguntaService preguntaService = new PreguntaService();
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("✅ CrearPreguntaServlet: doPost llamado");
        System.out.println("📍 URL solicitada: " + request.getRequestURI());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // Verificar sesión activa
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("usuarioId") == null) {
                System.out.println("❌ Sin sesión activa");
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");
            System.out.println("✅ Usuario ID: " + usuarioId);

            // Leer el cuerpo del request
            String body = request.getReader().lines()
                    .collect(java.util.stream.Collectors.joining());
            System.out.println("📥 Body recibido: " + body);

            // Leer datos JSON del request
            Map<String, Object> datos = gson.fromJson(body, Map.class);

            // Extraer triviaId
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

            System.out.println("📋 Trivia ID: " + triviaId);

            // Extraer campos de la pregunta
            String contenido = (String) datos.get("contenido");
            String tipo = (String) datos.get("tipo");
            Integer puntos = datos.get("puntos") != null ?
                    ((Double) datos.get("puntos")).intValue() : null;
            Integer limiteTiempo = datos.get("limiteTiempo") != null ?
                    ((Double) datos.get("limiteTiempo")).intValue() : null;
            String dificultad = (String) datos.get("dificultad");
            String imagenPregunta = (String) datos.get("imagenPregunta");
            String explicacion = (String) datos.get("explicacion");

            System.out.println("📝 Contenido: " + contenido);

            // Extraer opciones de respuesta
            List<Map<String, Object>> opcionesRaw =
                    (List<Map<String, Object>>) datos.get("opciones");

            if (opcionesRaw == null || opcionesRaw.isEmpty()) {
                result.put("success", false);
                result.put("message", "Debe incluir opciones de respuesta");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            System.out.println("🔢 Opciones recibidas: " + opcionesRaw.size());

            // Convertir opciones a DTO
            List<PreguntaService.OpcionRespuestaDTO> opciones = new ArrayList<>();
            for (Map<String, Object> opcionRaw : opcionesRaw) {
                String textoOpcion = (String) opcionRaw.get("textoOpcion");
                Boolean isCorrecto = (Boolean) opcionRaw.get("isCorrecto");

                opciones.add(new PreguntaService.OpcionRespuestaDTO(textoOpcion, isCorrecto));
                System.out.println("  - " + textoOpcion + " (correcta: " + isCorrecto + ")");
            }

            // Llamar al servicio para crear la pregunta
            System.out.println("🚀 Llamando a preguntaService.crearPregunta()...");
            Preguntas pregunta = preguntaService.crearPregunta(
                    triviaId,
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

            System.out.println("✅ Pregunta creada con ID: " + pregunta.getPreguntaId());

            // Construir respuesta con la pregunta creada
            Map<String, Object> preguntaMap = new HashMap<>();
            preguntaMap.put("preguntaId", pregunta.getPreguntaId());
            preguntaMap.put("orderPregunta", pregunta.getOrderPregunta());
            preguntaMap.put("contenido", pregunta.getContenido());
            preguntaMap.put("tipo", pregunta.getTipo());
            preguntaMap.put("puntos", pregunta.getPuntos());
            preguntaMap.put("limiteTiempo", pregunta.getLimiteTiempo());
            preguntaMap.put("dificultad", pregunta.getDificultad());

            // Opciones
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

            result.put("success", true);
            result.put("message", "Pregunta creada exitosamente");
            result.put("pregunta", preguntaMap);

            response.setStatus(HttpServletResponse.SC_CREATED);
            System.out.println("✅ Respuesta enviada exitosamente");

        } catch (RuntimeException e) {
            System.err.println("❌ Error RuntimeException: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("❌ Error Exception: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error interno del servidor");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
    }
}