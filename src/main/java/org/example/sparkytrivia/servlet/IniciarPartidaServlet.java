package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.service.SalaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "IniciarPartidaServlet", urlPatterns = {"/api/salas/iniciar"})
public class IniciarPartidaServlet extends HttpServlet {

    private SalaService salaService = new SalaService();
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // VALIDAR SESIÓN
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // LEER DATOS
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            Integer salaId = datos.get("salaId") != null
                    ? ((Double) datos.get("salaId")).intValue()
                    : null;

            if (salaId == null) {
                result.put("success", false);
                result.put("message", "ID de sala requerido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // INICIAR PARTIDA
            salaService.iniciarPartida(salaId, usuarioId);

            result.put("success", true);
            result.put("message", "Partida iniciada exitosamente");
            response.setStatus(HttpServletResponse.SC_OK); // 200

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400

            System.err.println("Error al iniciar partida: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}