package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.websocket.GameManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SERVLET PARA INICIAR EL HILO DEL JUEGO (GameManager)
 *
 * Endpoint: POST /api/salas/iniciar-juego
 *
 * Este servlet arranca el GameRoomThread que controla
 * el flujo del juego (enviar preguntas, temporizadores, etc.)
 */
@WebServlet(name = "IniciarJuegoServlet", urlPatterns = {"/api/salas/iniciar-juego"})
public class IniciarJuegoServlet extends HttpServlet {

    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // Validar sesión
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Leer código de sala
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);
            String codigoSala = (String) datos.get("codigoSala");

            if (codigoSala == null || codigoSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Código de sala requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Iniciar el juego en el GameManager
            boolean iniciado = GameManager.getInstance().iniciarTrivia(codigoSala.toUpperCase());

            if (iniciado) {
                result.put("success", true);
                result.put("message", "Juego iniciado correctamente");
                response.setStatus(HttpServletResponse.SC_OK);

                System.out.println("✅ Juego iniciado por servlet para sala: " + codigoSala);

            } else {
                result.put("success", false);
                result.put("message", "No se pudo iniciar el juego. La sala podría estar ya en curso.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al iniciar juego: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            System.err.println(" Error iniciando juego: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}