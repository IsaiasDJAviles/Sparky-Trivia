package org.example.sparkytrivia.servlet;


import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.Usuario;
import org.example.sparkytrivia.service.UsuarioService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name= "LoginServlet", urlPatterns = {"/api/auth/login"})
public class LoginServlet extends HttpServlet {

    private UsuarioService usuarioService = new UsuarioService();//servicio para validad credenciales
    private Gson gson = new Gson();


    //manejamos las peticiones POST
    @Override
    protected  void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //configura las respuestas como JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        //Map para construir la respuesta
        Map<String, Object> result = new HashMap<>();

        try {
            //leer JSON
            Map<String, String> datos = gson.fromJson(request.getReader(), Map.class);
            //autenticar user
            Usuario usuario = usuarioService.autenticar(datos.get("email"), datos.get("password"));
            //creamos sesion Http para mandtener el usuario logueado
            HttpSession session = request.getSession();
            //guardamos los datos del usuario en la sesion
            session.setAttribute("usuarioId", usuario.getUsuarioId());
            session.setAttribute("nickname", usuario.getNickName());
            session.setAttribute("email", usuario.getEmail());
            session.setAttribute("rol", usuario.getRol());
            //la sesion expira despues de 30 min inactivo
            session.setMaxInactiveInterval(1800);

            // Construir respuesta exitosa
            result.put("success", true);
            result.put("message", "Login exitoso");
            result.put("usuario", Map.of(
                    "usuarioId", usuario.getUsuarioId(),
                    "email", usuario.getEmail(),
                    "nickName", usuario.getNickName(),
                    "firstName", usuario.getFirstName(),
                    "lastName", usuario.getLastName(),
                    "rol", usuario.getRol()
            ));

            // Código HTTP 200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);


        } catch (Exception e) {
            // Si las credenciales son incorrectas
            result.put("success", false);
            result.put("message", e.getMessage());

            // Código HTTP 401 (No autorizado)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        // Enviar respuesta JSON al cliente
        response.getWriter().write(gson.toJson(result));
    }

}
