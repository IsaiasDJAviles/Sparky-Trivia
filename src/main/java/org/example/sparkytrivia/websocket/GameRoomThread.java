package org.example.sparkytrivia.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.sparkytrivia.dao.*;
import org.example.sparkytrivia.model.*;
import org.example.sparkytrivia.service.PuntajeService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.Map;

public class GameRoomThread implements Runnable {

    private final Sala sala;
    private final String codigoSala;
    private boolean activo = true;
    private final Gson gson = new Gson();

    // DAOs
    private final PreguntasDAO preguntasDAO = new PreguntasDAO();
    private final OpcionesRespuestaDAO opcionesDAO = new OpcionesRespuestaDAO();
    private final ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private final RespuestasJugadorDAO respuestasDAO = new RespuestasJugadorDAO();
    private final SalaDAO salaDAO = new SalaDAO();

    // Servicios
    private final PuntajeService puntajeService = new PuntajeService();

    // Preguntas de la trivia
    private List<Preguntas> preguntas;

    // Mapa para almacenar respuestas: participanteId → JsonObject
    private final Map<Integer, JsonObject> respuestasPreguntaActual = new ConcurrentHashMap<>();

    // Lista de participantes (thread-safe)
    private final List<Participantes> participantes = new CopyOnWriteArrayList<>();

    public GameRoomThread(Sala sala) {
        this.sala = sala;
        this.codigoSala = sala.getCodigoSala();
        System.out.println(" GameRoomThread creado para sala: " + codigoSala);
    }

    @Override
    public void run() {
        System.out.println(" Iniciando juego en sala: " + codigoSala);

        try {
            // FASE 1: INICIALIZACIÓN
            inicializar();

            // FASE 2: BUCLE PRINCIPAL DE PREGUNTAS
            for (int i = 0; i < preguntas.size() && activo; i++) {
                Preguntas pregunta = preguntas.get(i);

                System.out.println(" Enviando pregunta " + (i + 1) + "/" + preguntas.size());

                // Actualizar pregunta actual en BD
                sala.setPreguntaActual(i + 1);
                salaDAO.actualizar(sala);

                // Limpiar respuestas de la pregunta anterior
                respuestasPreguntaActual.clear();

                // Enviar pregunta a todos los clientes
                enviarPregunta(pregunta, i + 1);

                // Esperar que respondan o se acabe el tiempo
                esperarRespuestas(pregunta);

                // Calcular puntajes de esta pregunta
                calcularPuntajesPregunta(pregunta);

                // Actualizar y enviar ranking
                actualizarRanking();

                // Pausa antes de la siguiente pregunta
                if (i < preguntas.size() - 1) {
                    Thread.sleep(3000); // 3 segundos de pausa
                }
            }

            // FASE 3: FINALIZAR JUEGO
            finalizarJuego();

        } catch (InterruptedException e) {
            System.out.println(" Juego interrumpido en sala: " + codigoSala);
        } catch (Exception e) {
            System.err.println("Error en GameRoomThread: " + e.getMessage());
            e.printStackTrace();
        } finally {
            activo = false;
            System.out.println(" Juego finalizado en sala: " + codigoSala);
        }
    }


    private void inicializar() {
        System.out.println(" Inicializando juego...");

        // Cargar preguntas de la trivia (ordenadas)
        preguntas = preguntasDAO.listarPorTrivia(sala.getTrivia().getTriviaId());

        if (preguntas == null || preguntas.isEmpty()) {
            throw new RuntimeException("La trivia no tiene preguntas");
        }

        // Cargar participantes activos
        participantes.addAll(participantesDAO.listarActivosPorSala(sala.getSalaId()));

        System.out.println(" Juego inicializado: " + preguntas.size() + " preguntas, " +
                participantes.size() + " jugadores");
    }


    private void enviarPregunta(Preguntas pregunta, int numeroPregunta) {
        try {
            // Obtener opciones de respuesta
            List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());

            // Construir JSON de la pregunta
            JsonObject mensajePregunta = new JsonObject();
            mensajePregunta.addProperty("tipo", "PREGUNTA");
            mensajePregunta.addProperty("numeroPregunta", numeroPregunta);
            mensajePregunta.addProperty("totalPreguntas", preguntas.size());

            // Datos de la pregunta
            JsonObject dataPregunta = new JsonObject();
            dataPregunta.addProperty("preguntaId", pregunta.getPreguntaId());
            dataPregunta.addProperty("contenido", pregunta.getContenido());
            dataPregunta.addProperty("tipo", pregunta.getTipo());
            dataPregunta.addProperty("puntos", pregunta.getPuntos());
            dataPregunta.addProperty("limiteTiempo", pregunta.getLimiteTiempo());

            if (pregunta.getImagenPregunta() != null) {
                dataPregunta.addProperty("imagen", pregunta.getImagenPregunta());
            }

            // Opciones (sin revelar cuál es correcta)
            JsonArray opcionesArray = new JsonArray();
            for (OpcionesRespuesta opcion : opciones) {
                JsonObject opcionJson = new JsonObject();
                opcionJson.addProperty("opcionId", opcion.getOpcionId());
                opcionJson.addProperty("textoOpcion", opcion.getTextoOpcion());
                opcionesArray.add(opcionJson);
            }
            dataPregunta.add("opciones", opcionesArray);

            mensajePregunta.add("pregunta", dataPregunta);

            // Broadcast a todos
            GameWebSocket.broadcast(codigoSala, gson.toJson(mensajePregunta), null);

        } catch (Exception e) {
            System.err.println(" Error enviando pregunta: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void esperarRespuestas(Preguntas pregunta) throws InterruptedException {
        int limiteTiempo = pregunta.getLimiteTiempo() * 1000; // Convertir a milisegundos
        long tiempoInicio = System.currentTimeMillis();

        // Esperar hasta que se acabe el tiempo
        // (Los jugadores envían respuestas durante este tiempo)
        while (System.currentTimeMillis() - tiempoInicio < limiteTiempo && activo) {
            Thread.sleep(100); // Check cada 100ms

            // Opcional: Si todos ya respondieron, terminar antes
            if (respuestasPreguntaActual.size() >= participantes.size()) {
                System.out.println(" Todos respondieron antes del tiempo límite");
                break;
            }
        }

        System.out.println(" Tiempo agotado. Respuestas recibidas: " +
                respuestasPreguntaActual.size() + "/" + participantes.size());
    }

    private void calcularPuntajesPregunta(Preguntas pregunta) {
        System.out.println(" Calculando puntajes...");

        // Obtener la opción correcta
        List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());
        OpcionesRespuesta opcionCorrecta = opciones.stream()
                .filter(OpcionesRespuesta::getIsCorrecto)
                .findFirst()
                .orElse(null);

        if (opcionCorrecta == null) {
            System.err.println(" No se encontró respuesta correcta para pregunta: " + pregunta.getPreguntaId());
            return;
        }

        // Procesar cada respuesta recibida
        for (Map.Entry<Integer, JsonObject> entry : respuestasPreguntaActual.entrySet()) {
            Integer participanteId = entry.getKey();
            JsonObject respuestaData = entry.getValue();

            try {
                int opcionSeleccionadaId = respuestaData.get("opcionId").getAsInt();
                int tiempoTomado = respuestaData.get("tiempoTomado").getAsInt();

                // Verificar si es correcta
                boolean esCorrecta = (opcionSeleccionadaId == opcionCorrecta.getOpcionId());

                // Calcular puntos
                int puntosGanados = 0;
                if (esCorrecta) {
                    puntosGanados = puntajeService.calcularPuntaje(
                            tiempoTomado,
                            pregunta.getPuntos(),
                            pregunta.getLimiteTiempo()
                    );
                }

                // Guardar respuesta en BD
                Participantes participante = participantesDAO.buscarPorId(participanteId);
                OpcionesRespuesta opcionSeleccionada = opcionesDAO.buscarPorId(opcionSeleccionadaId);

                RespuestasJugador respuestaJugador = new RespuestasJugador(participante, pregunta, sala);
                respuestaJugador.setOpcionSeleccionada(opcionSeleccionada);
                respuestaJugador.setEsCorrecta(esCorrecta);
                respuestaJugador.setTiempoTomado(tiempoTomado);
                respuestaJugador.setPuntosGanados(puntosGanados);

                respuestasDAO.crear(respuestaJugador);

                // Actualizar puntaje del participante
                puntajeService.actualizarPuntaje(participanteId, puntosGanados);
                puntajeService.registrarRespuesta(participanteId, esCorrecta);

                System.out.println(" Respuesta procesada - Participante: " + participanteId +
                        " | Correcta: " + esCorrecta + " | Puntos: " + puntosGanados);

            } catch (Exception e) {
                System.err.println(" Error procesando respuesta de participante " + participanteId);
                e.printStackTrace();
            }
        }

        // Enviar respuesta correcta a todos
        enviarRespuestaCorrecta(pregunta, opcionCorrecta);
    }


    private void enviarRespuestaCorrecta(Preguntas pregunta, OpcionesRespuesta opcionCorrecta) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "RESPUESTA_CORRECTA");
        mensaje.addProperty("preguntaId", pregunta.getPreguntaId());
        mensaje.addProperty("opcionCorrectaId", opcionCorrecta.getOpcionId());

        if (pregunta.getExplicacion() != null) {
            mensaje.addProperty("explicacion", pregunta.getExplicacion());
        }

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensaje), null);
    }


    private synchronized void actualizarRanking() {
        System.out.println(" Actualizando ranking...");

        // Obtener ranking actualizado
        List<Participantes> ranking = puntajeService.obtenerRankingActual(sala.getSalaId());

        // Construir JSON del ranking
        JsonObject mensajeRanking = new JsonObject();
        mensajeRanking.addProperty("tipo", "RANKING");

        JsonArray rankingArray = new JsonArray();
        int posicion = 1;
        for (Participantes p : ranking) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("posicion", posicion);
            jugador.addProperty("nickname", p.getNicknameJuego());
            jugador.addProperty("puntaje", p.getPuntajeFinal());
            jugador.addProperty("correctas", p.getPreguntaCorrecta());
            jugador.addProperty("respondidas", p.getPreguntaRespuesta());
            rankingArray.add(jugador);
            posicion++;
        }

        mensajeRanking.add("ranking", rankingArray);

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensajeRanking), null);
    }


    private void finalizarJuego() {
        System.out.println(" Finalizando juego...");

        // Calcular ranking final
        puntajeService.calcularRanking(sala.getSalaId());

        // Obtener ranking final
        List<Participantes> rankingFinal = puntajeService.obtenerRankingActual(sala.getSalaId());

        // Enviar resultados finales
        JsonObject mensajeFin = new JsonObject();
        mensajeFin.addProperty("tipo", "JUEGO_FINALIZADO");
        mensajeFin.addProperty("mensaje", "¡Juego terminado!");

        JsonArray rankingArray = new JsonArray();
        for (Participantes p : rankingFinal) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("rangoFinal", p.getRangoFinal());
            jugador.addProperty("nickname", p.getNicknameJuego());
            jugador.addProperty("puntajeFinal", p.getPuntajeFinal());
            jugador.addProperty("correctas", p.getPreguntaCorrecta());
            jugador.addProperty("respondidas", p.getPreguntaRespuesta());

            double precision = puntajeService.calcularPrecision(p.getParticipanteId());
            jugador.addProperty("precision", precision);

            rankingArray.add(jugador);
        }

        mensajeFin.add("rankingFinal", rankingArray);

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensajeFin), null);

        // Notificar al GameManager que finalizó
        GameManager.getInstance().finalizarTrivia(codigoSala);
    }

    public void recibirRespuesta(JsonObject respuesta) {
        try {
            int participanteId = respuesta.get("participanteId").getAsInt();

            // Guardar respuesta (thread-safe)
            respuestasPreguntaActual.put(participanteId, respuesta);

            System.out.println(" Respuesta recibida de participante: " + participanteId);

        } catch (Exception e) {
            System.err.println(" Error recibiendo respuesta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void detener() {
        activo = false;
        System.out.println(" Deteniendo hilo de sala: " + codigoSala);
    }

    public boolean estaActivo() {
        return activo;
    }
}