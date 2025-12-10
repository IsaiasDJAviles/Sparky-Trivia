package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.service.SalaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SERVLET REST PARA CREAR SALAS
 *
 * Endpoint: POST /api/salas/crear
 * Protocolo: HTTP sobre TCP
 *
 * Request Body (JSON):
 * {
 *   "nombreSala": "Sala de Estudio",
 *   "triviaId": 1,
 *   "maxUsuarios": 50,
 *   "esPublico": true,
 *   "unirseDespues": false
 * }
 *
 * Response (JSON):
 * {
 *   "success": true,
 *   "message": "Sala creada exitosamente",
 *   "sala": {
 *     "salaId": 1,
 *     "codigoSala": "XY34AB",
 *     "nombreSala": "Sala de Estudio",
 *     "status": "esperando",
 *     "usuariosActuales": 1,
 *     "maxUsuarios": 50
 *   }
 * }
 */
@WebServlet(name = "CrearSalaServlet", urlPatterns = {"/api/salas/crear"})
public class CrearSalaServlet extends HttpServlet {

    private SalaService salaService = new SalaService();
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON con charset UTF-8
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // VALIDAR SESIÓN (el usuario debe estar logueado)
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión para crear una sala");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // LEER DATOS DEL REQUEST (JSON)
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            // Extraer campos
            String nombreSala = (String) datos.get("nombreSala");

            // Convertir Double a Integer (Gson lee números como Double)
            Integer triviaId = datos.get("triviaId") != null
                    ? ((Double) datos.get("triviaId")).intValue()
                    : null;

            Integer maxUsuarios = datos.get("maxUsuarios") != null
                    ? ((Double) datos.get("maxUsuarios")).intValue()
                    : 50;

            Boolean esPublico = (Boolean) datos.get("esPublico");
            if (esPublico == null) esPublico = true;

            Boolean unirseDespues = (Boolean) datos.get("unirseDespues");
            if (unirseDespues == null) unirseDespues = false;

            // VALIDACIONES
            if (nombreSala == null || nombreSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El nombre de la sala es obligatorio");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            if (triviaId == null) {
                result.put("success", false);
                result.put("message", "Debes seleccionar una trivia");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // LLAMAR AL SERVICIO PARA CREAR LA SALA
            Sala sala = salaService.crearSala(
                    nombreSala,
                    triviaId,
                    usuarioId,
                    maxUsuarios,
                    esPublico,
                    unirseDespues
            );

            // CONSTRUIR RESPUESTA EXITOSA
            result.put("success", true);
            result.put("message", "Sala creada exitosamente");
            result.put("sala", Map.of(
                    "salaId", sala.getSalaId(),
                    "codigoSala", sala.getCodigoSala(),
                    "nombreSala", sala.getNombreSala(),
                    "status", sala.getStatus(),
                    "usuariosActuales", sala.getUsuariosActuales(),
                    "maxUsuarios", sala.getMaxUsuario(),
                    "esPublico", sala.getEsPublico(),
                    "triviaId", sala.getTrivia().getTriviaId(),
                    "tituloTrivia", sala.getTrivia().getTitulo()
            ));

            response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created

        } catch (Exception e) {
            // MANEJO DE ERRORES
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400

            // Log del error en consola del servidor
            System.err.println("Error al crear sala: " + e.getMessage());
            e.printStackTrace();
        }

        // ENVIAR RESPUESTA JSON
        response.getWriter().write(gson.toJson(result));
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Para soporte de CORS (si lo necesitas)
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}