package org.example.sparkytrivia.websocket;

import com.google.gson.JsonObject;
import org.example.sparkytrivia.dao.SalaDAO;
import org.example.sparkytrivia.model.Sala;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    // Instancia única (Singleton)
    private static GameManager instance;

    // Almacena los hilos activos para cada sala en juego
    private final Map<String, GameRoomThread> salasActivas;

    private final SalaDAO salaDAO;

    private GameManager() {
        this.salasActivas = new ConcurrentHashMap<>();
        this.salaDAO = new SalaDAO();
        System.out.println(" GameManager inicializado");
    }


    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public boolean iniciarTrivia(String codigoSala) {
        System.out.println(" Iniciando trivia en sala: " + codigoSala);

        // Verificar que no esté ya iniciada
        if (salasActivas.containsKey(codigoSala)) {
            System.out.println(" La sala " + codigoSala + " ya tiene una partida en curso");
            return false;
        }

        try {
            // Obtener sala de la BD
            Sala sala = salaDAO.buscarPorCodigo(codigoSala);
            if (sala == null) {
                System.err.println(" Sala no encontrada: " + codigoSala);
                return false;
            }

            // Actualizar estado de la sala
            sala.setStatus("en_progreso");
            sala.setInicio(java.time.LocalDateTime.now());
            salaDAO.actualizar(sala);

            // Crear y arrancar hilo de juego
            GameRoomThread gameThread = new GameRoomThread(sala);
            Thread thread = new Thread(gameThread);
            thread.setName("GameRoom-" + codigoSala);
            thread.start();

            // Guardar en mapa
            salasActivas.put(codigoSala, gameThread);

            System.out.println(" Partida iniciada en sala: " + codigoSala);

            // Notificar a todos los clientes que la partida comenzó
            JsonObject inicio = new JsonObject();
            inicio.addProperty("tipo", "PARTIDA_INICIADA");
            inicio.addProperty("mensaje", "¡La partida ha comenzado!");
            GameWebSocket.broadcast(codigoSala, inicio.toString(), null);

            return true;

        } catch (Exception e) {
            System.err.println(" Error al iniciar trivia: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public void finalizarTrivia(String codigoSala) {
        System.out.println(" Finalizando trivia en sala: " + codigoSala);

        GameRoomThread gameThread = salasActivas.remove(codigoSala);

        if (gameThread != null) {
            // Detener el hilo
            gameThread.detener();

            // Actualizar estado de la sala en BD
            try {
                Sala sala = salaDAO.buscarPorCodigo(codigoSala);
                if (sala != null) {
                    sala.setStatus("completada");
                    sala.setFinalizacion(java.time.LocalDateTime.now());
                    salaDAO.actualizar(sala);
                }
            } catch (Exception e) {
                System.err.println(" Error actualizando estado de sala: " + e.getMessage());
            }

            System.out.println(" Trivia finalizada en sala: " + codigoSala);

            // Notificar a los clientes
            JsonObject fin = new JsonObject();
            fin.addProperty("tipo", "PARTIDA_FINALIZADA");
            fin.addProperty("mensaje", "¡Partida finalizada!");
            GameWebSocket.broadcast(codigoSala, fin.toString(), null);

        } else {
            System.out.println("️ No se encontró hilo activo para sala: " + codigoSala);
        }
    }

    public void procesarRespuesta(String codigoSala, JsonObject respuesta) {
        GameRoomThread gameThread = salasActivas.get(codigoSala);

        if (gameThread != null) {
            gameThread.recibirRespuesta(respuesta);
        } else {
            System.out.println(" No hay partida activa en sala: " + codigoSala);
        }
    }

    public boolean estaActiva(String codigoSala) {
        return salasActivas.containsKey(codigoSala);
    }

    public int getSalasActivasCount() {
        return salasActivas.size();
    }


    public void broadcast(String codigoSala, String mensaje) {
        GameWebSocket.broadcast(codigoSala, mensaje, null);
    }

    public String getEstadoSala(String codigoSala) {
        if (salasActivas.containsKey(codigoSala)) {
            return "EN_PROGRESO";
        }

        try {
            Sala sala = salaDAO.buscarPorCodigo(codigoSala);
            if (sala != null) {
                return sala.getStatus().toUpperCase();
            }
        } catch (Exception e) {
            System.err.println(" Error obteniendo estado de sala: " + e.getMessage());
        }

        return "DESCONOCIDO";
    }

    public void limpiarSalasInactivas() {
        System.out.println(" Limpiando salas inactivas...");

        salasActivas.entrySet().removeIf(entry -> {
            GameRoomThread thread = entry.getValue();
            if (!thread.estaActivo()) {
                System.out.println(" Removiendo sala inactiva: " + entry.getKey());
                return true;
            }
            return false;
        });

        System.out.println(" Limpieza completada. Salas activas: " + salasActivas.size());
    }

    public void imprimirEstado() {
        System.out.println(" ESTADO DEL GAME MANAGER");
        System.out.println("Salas activas: " + salasActivas.size());

        for (Map.Entry<String, GameRoomThread> entry : salasActivas.entrySet()) {
            GameRoomThread thread = entry.getValue();
            System.out.println("  - " + entry.getKey() + ": " +
                    (thread.estaActivo() ? " ACTIVO" : " INACTIVO"));
        }
    }
}