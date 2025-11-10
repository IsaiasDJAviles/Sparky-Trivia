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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@WebServlet(name = "ListarTriviasServlet", urlPatterns = {"/api/trivias"})
public class ListarTriviasServlet extends HttpServlet {

    // Servicio para lógica de negocio
    private TriviaService triviaService = new TriviaService();

    // Gson para JSON
    private Gson gson = new Gson();

    //aqui manejasmos las peticiones GET para el listado de trivias
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //configuramso las respuesta como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Map para construir respuesta
        Map<String, Object> result = new HashMap<>();

        try {
            // verificar si quiere listar "mis trivias" o "trivias públicas"
            String mias = request.getParameter("mias");

            List<Trivia> trivias;

            if ("true".equals(mias)) {
                // quiere ver SUS trivias
                // Verificar que esté logueado
                HttpSession session = request.getSession(false);

                if (session == null || session.getAttribute("usuarioId") == null) {
                    //no está logueado
                    result.put("success", false);
                    result.put("message", "Debes iniciar sesión");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                    response.getWriter().write(gson.toJson(result));
                    return;
                }

                //obtener ID del usuario desde la sesión
                Integer usuarioId = (Integer) session.getAttribute("usuarioId");

                //listar trivias del usuario
                trivias = triviaService.listarMisTrivias(usuarioId);

            } else {
                //listar trivias públicas (cualquiera puede verlas)
                trivias = triviaService.listarTriviasPublicas();
            }

            // convertir lista de Trivia a lista de Maps (para JSON limpio)
            List<Map<String, Object>> triviasJson = new ArrayList<>();

            for (Trivia trivia : trivias) {
                // Crear un Map por cada trivia
                Map<String, Object> triviaMap = new HashMap<>();

                // Agregar campos básicos
                triviaMap.put("triviaId", trivia.getTriviaId());
                triviaMap.put("titulo", trivia.getTitulo());
                triviaMap.put("descripcion", trivia.getDescripcion());
                triviaMap.put("categoria", trivia.getCategoria());
                triviaMap.put("dificultad", trivia.getDificultad());
                triviaMap.put("preguntasTotales", trivia.getPreguntasTotales());
                triviaMap.put("tiempoEstimado", trivia.getTiempoEstimado());
                triviaMap.put("status", trivia.getStatus());
                triviaMap.put("esPublico", trivia.getEsPublico());
                triviaMap.put("vecesJugada", trivia.getVecesJugada());

                // Agregar info del host (creador)
                triviaMap.put("host", Map.of(
                        "usuarioId", trivia.getHost().getUsuarioId(),
                        "nickName", trivia.getHost().getNickName()
                ));

                // Agregar fechas
                triviaMap.put("fechaCreacion", trivia.getFechaCreacion().toString());

                // Agregar al listado
                triviasJson.add(triviaMap);
            }

            // construir respuesta exitosa
            result.put("success", true);
            result.put("trivias", triviasJson);
            result.put("total", trivias.size()); // Cuántas trivias se encontraron

            // Código HTTP 200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            // Si algo falló
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
        }

        // Enviar respuesta JSON al cliente
        response.getWriter().write(gson.toJson(result));
    }
}