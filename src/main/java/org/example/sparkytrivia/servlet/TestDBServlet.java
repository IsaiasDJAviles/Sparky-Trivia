package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sparkytrivia.dao.UsuarioDAO;
import org.example.sparkytrivia.model.Usuario;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "TestDBServlet", urlPatterns = {"/api/test/db"})
public class TestDBServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // Intentar obtener todos los usuarios
            List<Usuario> usuarios = usuarioDAO.listarTodos();

            result.put("success", true);
            result.put("message", "âœ… ConexiÃ³n a PostgreSQL exitosa!");
            result.put("userCount", usuarios.size());
            result.put("users", usuarios);

            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "âŒ Error al conectar con la base de datos");
            result.put("error", e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(result));
    }
}