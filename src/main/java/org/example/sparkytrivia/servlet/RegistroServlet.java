package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sparkytrivia.model.Usuario;
import org.example.sparkytrivia.service.UsuarioService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * OBSERVACIONES
 * @WebServlet(name = "RegistroServlet", urlPatterns = {"/api/auth/registro"})
 *  Es una anotación que le dice a Tomcat: "Este servlet responde en la ruta /api/auth/registro
 *
 * Que es GSON?
 *  librería de Google para convertir entre JSON y objetos Java
 *  Sin Gson: Tendrías que construir el JSON a mano con Strings
 *
 * Para que los JSON?
 *   Es un formato de texto para representar datos estructurados de forma legible tanto
 *   para humanos como para máquinas.
 */

@WebServlet(name = "RegistroServlet", urlPatterns = {"/api/auth/registro"})
public class RegistroServlet extends HttpServlet {
    private UsuarioService usuarioService = new UsuarioService();
    private Gson gson = new Gson();

    // Maneja peticiones POST para registrar usuarios(CREAR/ ENVIAR DATOS)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //configurar que la respuesta sea JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        //map para construire la respuesta JSON
        Map<String, Object> result = new HashMap<>();
        try {
            //leemos JSON del request y convertirlo a Map
            Map<String, String> datos = gson.fromJson(request.getReader(), Map.class);
            //validamos que todos los campos requeridos existan
            if (datos.get("email") == null || datos.get("password") == null ||
                    datos.get("firstName") == null || datos.get("lastName") == null ||
                    datos.get("nickName") == null) {

                //si falta algún campo, devolver error 400
                result.put("success", false);
                result.put("message", "Todos los campos son obligatorios");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return; // Terminar ejecución aquí
            }
            //llamamos al servicio para registrar usuario
            Usuario usuario = usuarioService.registrar(
                    datos.get("email"),
                    datos.get("password"),
                    datos.get("firstName"),
                    datos.get("lastName"),
                    datos.get("nickName")
            );
            //se creó el suario exitosamente, preparar respuesta
            result.put("success", true);
            result.put("message", "Usuario registrado exitosamente");
            result.put("usuario", Map.of(
                    "usuarioId", usuario.getUsuarioId(),
                    "email", usuario.getEmail(),
                    "nickName", usuario.getNickName(),
                    "firstName", usuario.getFirstName(),
                    "lastName", usuario.getLastName()
            ));
            response.setStatus(HttpServletResponse.SC_CREATED);

        }catch (Exception e){
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        //Enviar respuesta JSON al cliente
        response.getWriter().write(gson.toJson(result));
    }

}

