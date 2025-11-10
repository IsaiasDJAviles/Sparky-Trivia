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

//SERVLET PARA CREAR NUEVAS TRIVIAS

@WebServlet(name = "CrearTriviaServlet", urlPatterns = {"/api/trivias/crear"})
public class CrearTriviaServlet extends HttpServlet {

    // Servicio para lógica de negocio
    private TriviaService triviaService = new TriviaService();

    // Gson para JSON
    private Gson gson = new Gson();

    //manejamos las peticiones POST para crear la trivia
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Map para construir respuesta
        Map<String, Object> result = new HashMap<>();

        try {
            //Verificar que el usuario esté logueado (tiene sesión activa)
            HttpSession session = request.getSession(false); // false = no crear nueva

            if (session == null || session.getAttribute("usuarioId") == null) {
                // Si no hay sesión, el usuario NO está logueado
                result.put("success", false);
                result.put("message", "Debes iniciar sesión para crear trivias");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return; // Terminar aquí
            }

            // Obtener el ID del usuario desde la sesión
            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // Leer datos JSON del request
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            //Extraer campos del JSON
            String titulo = (String) datos.get("titulo");
            String descripcion = (String) datos.get("descripcion");
            String categoria = (String) datos.get("categoria");
            String dificultad = (String) datos.get("dificultad");
            String fotoPortada = (String) datos.get("fotoPortada");

            // Convertir tiempoEstimado de Double a Integer (Gson lee números como Double)
            Integer tiempoEstimado = null;
            if (datos.get("tiempoEstimado") != null) {
                tiempoEstimado = ((Double) datos.get("tiempoEstimado")).intValue();
            }

            // Obtener esPublico (Boolean)
            Boolean esPublico = (Boolean) datos.get("esPublico");

            //Validar que el título sea obligatorio
            if (titulo == null || titulo.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El título es obligatorio");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            //Llamar al servicio para crear la trivia
            Trivia trivia = triviaService.crearTrivia(
                    titulo,
                    descripcion,
                    usuarioId,        // El usuario logueado será el host
                    categoria,
                    dificultad,
                    tiempoEstimado,
                    fotoPortada,
                    esPublico
            );

            //Construir respuesta exitosa
            result.put("success", true);
            result.put("message", "Trivia creada exitosamente");
            result.put("trivia", Map.of(
                    "triviaId", trivia.getTriviaId(),
                    "titulo", trivia.getTitulo(),
                    "descripcion", trivia.getDescripcion() != null ? trivia.getDescripcion() : "",
                    "categoria", trivia.getCategoria(),
                    "dificultad", trivia.getDificultad(),
                    "status", trivia.getStatus(),
                    "esPublico", trivia.getEsPublico()
            ));

            // Código HTTP 201 (Created - Recurso creado)
            response.setStatus(HttpServletResponse.SC_CREATED);

        } catch (Exception e) {
            // Si algo falló (ej: usuario no existe, error de BD)
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
        }

        // Enviar respuesta JSON al cliente
        response.getWriter().write(gson.toJson(result));
    }
}