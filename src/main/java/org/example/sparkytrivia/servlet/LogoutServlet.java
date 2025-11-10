package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/api/auth/logout"})
public class LogoutServlet extends HttpServlet {
    //objeto Gson para convertir datos Java en formato JSON
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");// Indica que la respuesta será de tipo JSON

        // Invalidar sesión si existe
        // Obtiene la sesión actual sin crear una nueva (false evita crear una si no existe)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();// Si existe una sesión activa, la invalida (la cierra), eliminando sus datos
        }
        // Crea un mapa para almacenar los datos que se enviarán como respuesta
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Logout exitoso");

        // Convierte el mapa a JSON y lo envía al cliente como respuesta
        response.getWriter().write(gson.toJson(result));
    }
}