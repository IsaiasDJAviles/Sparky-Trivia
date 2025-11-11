package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.Trivia;
import org.example.sparkytrivia.service.TriviaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


//SERVLET PARA ACTUALIZAR UNA TRIVIA EXISTENTE
@WebServlet(name = "ActualizarTriviaServlet", urlPatterns = {"/api/trivias/actualizar/*"})
public class ActualizarTriviaServlet extends HttpServlet {

    // Servicio para lógica de negocio
    private TriviaService triviaService = new TriviaService();

    // Gson para JSON
    private Gson gson = new Gson();

    //manejamos la peticion PUT para actualizar Trivias
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Map para construir respuesta
        Map<String, Object> result = new HashMap<>();

        try {
            //verificamos que el usuario esté logueado
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return;
            }

            //obtenemo el ID del usuario desde la sesión
            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // extraer el ID de la trivia de la URL
            String pathInfo = request.getPathInfo(); //

            if (pathInfo == null || pathInfo.equals("/")) {
                result.put("success", false);
                result.put("message", "ID de trivia no especificado");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Convertir ID a Integer
            String idStr = pathInfo.substring(1);
            Integer triviaId = Integer.parseInt(idStr);

            //leer datos JSON del request
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            //extraer campos del JSON
            String titulo = (String) datos.get("titulo");
            String descripcion = (String) datos.get("descripcion");
            String categoria = (String) datos.get("categoria");
            String dificultad = (String) datos.get("dificultad");
            String fotoPortada = (String) datos.get("fotoPortada");
            String status = (String) datos.get("status");
            Boolean esPublico = (Boolean) datos.get("esPublico");

            // Convertir tiempoEstimado
            Integer tiempoEstimado = null;
            if (datos.get("tiempoEstimado") != null) {
                tiempoEstimado = ((Double) datos.get("tiempoEstimado")).intValue();
            }

            // llamamos al servicio para actualizar
            // El servicio validará que el usuario sea el host
            Trivia trivia = triviaService.actualizarTrivia(
                    triviaId,
                    usuarioId,
                    titulo,
                    descripcion,
                    categoria,
                    dificultad,
                    tiempoEstimado,
                    fotoPortada,
                    esPublico,
                    status
            );

            //Construir respuesta exitosa
            result.put("success", true);
            result.put("message", "Trivia actualizada exitosamente");
            result.put("trivia", Map.of(
                    "triviaId", trivia.getTriviaId(),
                    "titulo", trivia.getTitulo(),
                    "descripcion", trivia.getDescripcion() != null ? trivia.getDescripcion() : "",
                    "categoria", trivia.getCategoria(),
                    "dificultad", trivia.getDificultad(),
                    "status", trivia.getStatus(),
                    "esPublico", trivia.getEsPublico()
            ));

            // Código HTTP 200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            // Error de validación o permisos
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
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