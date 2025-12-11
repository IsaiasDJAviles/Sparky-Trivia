package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "SessionCheckServlet", urlPatterns = {"/api/auth/session-check"})
public class SessionCheckServlet extends HttpServlet {

    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        // Obtener sesión SIN crear una nueva
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("usuarioId") != null) {
            // Sesión activa y válida
            result.put("valid", true);
            result.put("usuarioId", session.getAttribute("usuarioId"));
            result.put("nickname", session.getAttribute("nickname"));
            result.put("email", session.getAttribute("email"));

            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            // No hay sesión o está expirada
            result.put("valid", false);
            result.put("message", "Sesión no válida o expirada");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        response.getWriter().write(gson.toJson(result));
    }
}