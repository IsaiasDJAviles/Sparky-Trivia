package org.example.sparkytrivia.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.service.SalaService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "UnirseSalaServlet", urlPatterns = {"/api/salas/unirse"})
public class UnirseSalaServlet extends HttpServlet {

    private SalaService salaService = new SalaService();
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Configurar respuesta como JSON
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            // VALIDAR SESIÓN
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("usuarioId") == null) {
                result.put("success", false);
                result.put("message", "Debes iniciar sesión para unirte a una sala");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Integer usuarioId = (Integer) session.getAttribute("usuarioId");

            // LEER DATOS DEL REQUEST
            Map<String, Object> datos = gson.fromJson(request.getReader(), Map.class);

            String codigoSala = (String) datos.get("codigoSala");

            // VALIDAR CÓDIGO
            if (codigoSala == null || codigoSala.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El código de sala es obligatorio");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Convertir a mayúsculas y quitar espacios
            codigoSala = codigoSala.trim().toUpperCase();

            // Validar formato (6 caracteres alfanuméricos)
            if (!codigoSala.matches("^[A-Z0-9]{6}$")) {
                result.put("success", false);
                result.put("message", "Código inválido. Debe tener 6 caracteres");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // UNIRSE A LA SALA
            Participantes participante = salaService.unirseASala(codigoSala, usuarioId);

            // RESPUESTA EXITOSA
            result.put("success", true);
            result.put("message", "Te uniste exitosamente a la sala");
            result.put("participante", Map.of(
                    "participanteId", participante.getParticipanteId(),
                    "salaId", participante.getSala().getSalaId(),
                    "codigoSala", participante.getSala().getCodigoSala(),
                    "nombreSala", participante.getSala().getNombreSala(),
                    "nicknameJuego", participante.getNicknameJuego(),
                    "esHost", participante.getEsHost()
            ));

            response.setStatus(HttpServletResponse.SC_OK); // 200

        } catch (Exception e) {
            // MANEJO DE ERRORES
            result.put("success", false);
            result.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400

            System.err.println("Error al unirse a sala: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(gson.toJson(result));
    }
}