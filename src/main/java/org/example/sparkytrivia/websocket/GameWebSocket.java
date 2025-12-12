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

/**
 * WEBSOCKET PARA COMUNICACION EN TIEMPO REAL DEL JUEGO
 *
 * CORRECCION CRITICA: Se agrego la anotacion @OnMessage que faltaba
 * Sin esta anotacion, el servidor nunca recibia los mensajes del cliente
 */
@ServerEndpoint("/game/{codigoSala}")
public class GameWebSocket {

    // Almacena todas las conexiones WebSocket por sala
    private static final Map<String, Map<String, Session>> salasSessions = new ConcurrentHashMap<>();

    // Mapa: sessionId -> usuarioId (para identificar quien es cada conexion)
    private static final Map<String, Integer> sessionUsuarios = new ConcurrentHashMap<>();

    private static final Gson gson = new Gson();
    private static final SalaDAO salaDAO = new SalaDAO();
    private static final ParticipantesDAO participantesDAO = new ParticipantesDAO();

    @OnOpen
    public void onOpen(Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println("[WS] Nueva conexion - Sala: " + codigoSala + " | SessionID: " + session.getId());

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

            // Agregar sesion al mapa de la sala
            salasSessions.computeIfAbsent(codigoSala, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);

            // Enviar confirmacion de conexion
            JsonObject confirmacion = new JsonObject();
            confirmacion.addProperty("tipo", "CONECTADO");
            confirmacion.addProperty("mensaje", "Conectado a sala " + codigoSala);
            confirmacion.addProperty("salaId", sala.getSalaId());
            session.getBasicRemote().sendText(gson.toJson(confirmacion));

            System.out.println("[WS] Cliente conectado exitosamente a sala: " + codigoSala);

        } catch (Exception e) {
            System.err.println("[WS] Error en onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * CORRECCION CRITICA: Esta anotacion @OnMessage faltaba
     * Sin ella, el metodo nunca se ejecutaba cuando llegaban mensajes
     */
    @OnMessage
    public void onMessage(String mensaje, Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println("[WS] Mensaje recibido en sala " + codigoSala + ": " + mensaje);

        try {
            // Parsear mensaje JSON
            JsonObject data = gson.fromJson(mensaje, JsonObject.class);
            String tipo = data.get("tipo").getAsString();

            System.out.println("[WS] Tipo de mensaje: " + tipo);

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
                    // Responder PONG para mantener conexion viva
                    JsonObject pong = new JsonObject();
                    pong.addProperty("tipo", "PONG");
                    session.getBasicRemote().sendText(gson.toJson(pong));
                    break;

                default:
                    System.out.println("[WS] Tipo de mensaje desconocido: " + tipo);
            }

        } catch (Exception e) {
            System.err.println("[WS] Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("codigoSala") String codigoSala) {
        System.out.println("[WS] Cliente desconectado - Sala: " + codigoSala + " | SessionID: " + session.getId());

        try {
            // Remover sesion del mapa
            Map<String, Session> sessionsEnSala = salasSessions.get(codigoSala);
            if (sessionsEnSala != null) {
                sessionsEnSala.remove(session.getId());

                // Si no quedan sesiones, remover la sala del mapa
                if (sessionsEnSala.isEmpty()) {
                    salasSessions.remove(codigoSala);
                }
            }

            // Remover del mapa de usuarios
            sessionUsuarios.remove(session.getId());

            // Notificar a otros que alguien salio
            JsonObject notificacion = new JsonObject();
            notificacion.addProperty("tipo", "JUGADOR_SALIO");
            notificacion.addProperty("sessionId", session.getId());
            broadcast(codigoSala, gson.toJson(notificacion), session.getId());

        } catch (Exception e) {
            System.err.println("[WS] Error en onClose: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("codigoSala") String codigoSala) {
        System.err.println("[WS] Error WebSocket - Sala: " + codigoSala + " | Error: " + throwable.getMessage());
        throwable.printStackTrace();
    }

    /**
     * Manejar cuando un usuario se une a la sala
     */
    private void manejarUnirse(Session session, String codigoSala, JsonObject data) throws IOException {
        int usuarioId = data.get("usuarioId").getAsInt();
        String nickname = data.get("nickname").getAsString();

        System.out.println("[WS] Usuario uniendose - ID: " + usuarioId + " | Nick: " + nickname);

        // Guardar relacion session -> usuario
        sessionUsuarios.put(session.getId(), usuarioId);

        // Notificar a todos que alguien se unio
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "JUGADOR_UNIDO");
        notificacion.addProperty("usuarioId", usuarioId);
        notificacion.addProperty("nickname", nickname);

        broadcast(codigoSala, gson.toJson(notificacion), null);
    }

    /**
     * Manejar respuesta de un jugador a una pregunta
     * ESTE ES EL METODO CRITICO que envia la respuesta al GameRoomThread
     */
    private void manejarRespuesta(Session session, String codigoSala, JsonObject data) {
        try {
            int participanteId = data.get("participanteId").getAsInt();
            int preguntaId = data.get("preguntaId").getAsInt();
            int opcionId = data.get("opcionId").getAsInt();
            int tiempoTomado = data.get("tiempoTomado").getAsInt();

            System.out.println("[WS] ========== RESPUESTA RECIBIDA ==========");
            System.out.println("[WS] Sala: " + codigoSala);
            System.out.println("[WS] Participante ID: " + participanteId);
            System.out.println("[WS] Pregunta ID: " + preguntaId);
            System.out.println("[WS] Opcion ID: " + opcionId);
            System.out.println("[WS] Tiempo tomado: " + tiempoTomado + "s");
            System.out.println("[WS] ==========================================");

            // Enviar al GameRoomThread para procesar
            GameManager.getInstance().procesarRespuesta(codigoSala, data);

        } catch (Exception e) {
            System.err.println("[WS] Error procesando respuesta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Manejar mensaje de chat
     */
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

    /**
     * Enviar mensaje a todos los clientes de una sala
     */
    public static void broadcast(String codigoSala, String mensaje, String excluirSessionId) {
        Map<String, Session> sessionsEnSala = salasSessions.get(codigoSala);

        if (sessionsEnSala != null) {
            System.out.println("[WS] Broadcasting a sala " + codigoSala + " (" + sessionsEnSala.size() + " clientes)");

            for (Map.Entry<String, Session> entry : sessionsEnSala.entrySet()) {
                // Excluir sesion si se especifico
                if (excluirSessionId != null && entry.getKey().equals(excluirSessionId)) {
                    continue;
                }

                Session clientSession = entry.getValue();
                if (clientSession.isOpen()) {
                    try {
                        clientSession.getBasicRemote().sendText(mensaje);
                    } catch (IOException e) {
                        System.err.println("[WS] Error enviando mensaje a sesion: " + entry.getKey());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("[WS] No hay sesiones en sala: " + codigoSala);
        }
    }

    /**
     * Obtener numero de conexiones activas en una sala
     */
    public static int getConexionesActivas(String codigoSala) {
        Map<String, Session> sessions = salasSessions.get(codigoSala);
        return sessions != null ? sessions.size() : 0;
    }
}