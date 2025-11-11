package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.service.TriviaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//SERVLET PARA ELIMINAR UNA TRIVIA
@WebServlet(name= "EliminarTriviaServlet", urlPatterns = {"/api/trivias/eliminar/*"})
public class EliminarTriviaServlet extends HttpServlet {

    private TriviaService triviaService = new TriviaService();
    private Gson gson = new Gson();

    //maneja preticiones DELETE para eliminar las trivias
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        //map para la respuesta Json
        Map<String, Object> result = new HashMap<>();

        try{
            //verifica que el usuarioa esté logueado
            HttpSession session = request.getSession(false);
            //si no hay sesion o no tiene usuarioId guardado
            if(session == null || session.getAttribute("usuarioId") == null){
                result.put("success", false);
                result.put("message", "Debes iniciar sesion");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write(gson.toJson(result));
                return;
            }
            //btener el ID del usuario desde la sesion
            Integer usuarioId = (Integer) session.getAttribute("usuarioId");
            //extraer el ID de la trivia desde la url
            String pathInfo = request.getPathInfo();
            //valida que se envio un ID
            if(pathInfo == null || pathInfo.equals("/")){
                result.put("success", false);
                result.put("message", "ID de trivia no especificado");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                response.getWriter().write(gson.toJson(result));
                return;
            }
            //convertir el ID de string a integer
            String idStr = pathInfo.substring(1);
            Integer triviaId;

            try{
                //se intenta convertir a num
                triviaId = Integer.parseInt(idStr);
            }catch (NumberFormatException e){
                //si no es num valdio
                result.put("success", false);
                result.put("message", "ID de trivia invalido");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(result));
                return;

            }

            //lamar al servicio para eliminar la trivia
            triviaService.eliminarTrivia(triviaId, usuarioId);
            //ya eliminado exitoso
            result.put("success", true);
            result.put("message", "Trivia eliminada");
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        }catch (Exception e){
            result.put("success", false);
            result.put("message", "Error interno del servidor");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        response.getWriter().write(gson.toJson(result));
    }

}
