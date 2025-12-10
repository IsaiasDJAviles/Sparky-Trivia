package org.example.sparkytrivia.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.example.sparkytrivia.dao.ParticipantesDAO;
import org.example.sparkytrivia.dao.SalaDAO;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.Sala;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ServerEndpoint("/game/{codigoSala}")
public class GameWebSocket {

    // Almacena todas las conexiones WebSocket por sala
    private static final Map<String, Map<String, Session>> salasSessions = new ConcurrentHashMap<>();

    // Mapa: sessionId → usuarioId (para identificar quién es cada conexión)
    private static final Map<String, Integer> sessionUsuarios = new ConcurrentHashMap<>();

    private static final Gson gson = new Gson();
    private static final SalaDAO salaDAO = new SalaDAO();
    private static final ParticipantesDAO participantesDAO = new ParticipantesDAO();


    @OnOpen
    public void onOpen(Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println(" Nueva conexión WebSocket - Sala: " + codigoSala + " | SessionID: " + session.getId());

        try {
            // Verificar que la sala exista
            Sala sala = salaDAO.buscarPorCodigo(codigoSala);
            if (sala == null) {
                JsonObject error = new JsonObject();
                error.addProperty("tipo", "ERROR");
                error.addProperty("mensaje", "Sala no encontrada");
                session.getBasicRemote().sendText(gson.toJson(error));
                session.close();
                return;
            }

            // Agregar sesión al mapa de la sala
            salasSessions.computeIfAbsent(codigoSala, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);

            // Enviar confirmación de conexión
            JsonObject confirmacion = new JsonObject();
            confirmacion.addProperty("tipo", "CONECTADO");
            confirmacion.addProperty("mensaje", "Conectado a sala " + codigoSala);
            confirmacion.addProperty("salaId", sala.getSalaId());
            session.getBasicRemote().sendText(gson.toJson(confirmacion));

            System.out.println(" Cliente conectado exitosamente a sala: " + codigoSala);

        } catch (Exception e) {
            System.err.println(" Error en onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onMessage(String mensaje, Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println(" Mensaje recibido en sala " + codigoSala + ": " + mensaje);

        try {
            // Parsear mensaje JSON
            JsonObject data = gson.fromJson(mensaje, JsonObject.class);
            String tipo = data.get("tipo").getAsString();

            switch (tipo) {
                case "UNIRSE":
                    manejarUnirse(session, codigoSala, data);
                    break;

                case "RESPUESTA":
                    manejarRespuesta(session, codigoSala, data);
                    break;

                case "CHAT":
                    manejarChat(session, codigoSala, data);
                    break;

                case "PING":
                    // Responder PONG para mantener conexión viva
                    JsonObject pong = new JsonObject();
                    pong.addProperty("tipo", "PONG");
                    session.getBasicRemote().sendText(gson.toJson(pong));
                    break;

                default:
                    System.out.println("Tipo de mensaje desconocido: " + tipo);
            }

        } catch (Exception e) {
            System.err.println(" Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println(" Cliente desconectado - Sala: " + codigoSala + " | SessionID: " + session.getId());

        try {
            // Remover sesión del mapa
            Map<String, Session> sessionsEnSala = salasSessions.get(codigoSala);
            if (sessionsEnSala != null) {
                sessionsEnSala.remove(session.getId());

                // Si no quedan sesiones, remover la sala del mapa
                if (sessionsEnSala.isEmpty()) {
                    salasSessions.remove(codigoSala);
                    System.out.println("🗑️ Sala " + codigoSala + " removida (sin conexiones activas)");
                }
            }

            // Obtener usuarioId de la sesión
            Integer usuarioId = sessionUsuarios.remove(session.getId());

            if (usuarioId != null) {
                // Notificar a los demás que este usuario se desconectó
                JsonObject desconexion = new JsonObject();
                desconexion.addProperty("tipo", "JUGADOR_SALIO");
                desconexion.addProperty("usuarioId", usuarioId);
                broadcast(codigoSala, gson.toJson(desconexion), session.getId());
            }

        } catch (Exception e) {
            System.err.println(" Error en onClose: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println(" Error en WebSocket - SessionID: " + session.getId());
        error.printStackTrace();
    }

    private void manejarUnirse(Session session, String codigoSala, JsonObject data) throws IOException {
        int usuarioId = data.get("usuarioId").getAsInt();
        String nickname = data.get("nickname").getAsString();

        // Guardar asociación sessionId → usuarioId
        sessionUsuarios.put(session.getId(), usuarioId);

        // Notificar a todos que un nuevo jugador se unió
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "JUGADOR_UNIDO");
        notificacion.addProperty("usuarioId", usuarioId);
        notificacion.addProperty("nickname", nickname);

        // Obtener sala para saber cuántos usuarios hay
        Sala sala = salaDAO.buscarPorCodigo(codigoSala);
        if (sala != null) {
            notificacion.addProperty("usuariosActuales", sala.getUsuariosActuales());
        }

        broadcast(codigoSala, gson.toJson(notificacion), null);

        System.out.println(" " + nickname + " se unió a la sala " + codigoSala);
    }


    private void manejarRespuesta(Session session, String codigoSala, JsonObject data) {
        // Este método será implementado con GameManager
        // Por ahora solo confirmamos recepción
        int participanteId = data.get("participanteId").getAsInt();
        int preguntaId = data.get("preguntaId").getAsInt();

        System.out.println(" Respuesta recibida - Participante: " + participanteId + " | Pregunta: " + preguntaId);

        // El GameRoomThread procesará la respuesta
        GameManager.getInstance().procesarRespuesta(codigoSala, data);
    }

    private void manejarChat(Session session, String codigoSala, JsonObject data) throws IOException {
        String mensaje = data.get("mensaje").getAsString();
        Integer usuarioId = sessionUsuarios.get(session.getId());

        if (usuarioId != null) {
            JsonObject chat = new JsonObject();
            chat.addProperty("tipo", "CHAT");
            chat.addProperty("usuarioId", usuarioId);
            chat.addProperty("mensaje", mensaje);
            chat.addProperty("timestamp", System.currentTimeMillis());

            broadcast(codigoSala, gson.toJson(chat), null);
        }
    }

    public static void broadcast(String codigoSala, String mensaje, String excluirSessionId) {
        Map<String, Session> sessionsEnSala = salasSessions.get(codigoSala);

        if (sessionsEnSala != null) {
            for (Map.Entry<String, Session> entry : sessionsEnSala.entrySet()) {
                // Excluir sesión si se especificó
                if (excluirSessionId != null && entry.getKey().equals(excluirSessionId)) {
                    continue;
                }

                Session clientSession = entry.getValue();
                if (clientSession.isOpen()) {
                    try {
                        clientSession.getBasicRemote().sendText(mensaje);
                    } catch (IOException e) {
                        System.err.println("Error enviando mensaje a sesión: " + entry.getKey());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static int getConexionesActivas(String codigoSala) {
        Map<String, Session> sessions = salasSessions.get(codigoSala);
        return sessions != null ? sessions.size() : 0;
    }
}