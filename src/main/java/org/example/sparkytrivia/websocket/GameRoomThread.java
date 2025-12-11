package org.example.sparkytrivia.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.sparkytrivia.dao.*;
import org.example.sparkytrivia.model.*;
import org.example.sparkytrivia.service.PuntajeService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoomThread implements Runnable {

    private final Sala sala;
    private final String codigoSala;
    private boolean activo = true;
    private final Gson gson = new Gson();

    private final PreguntasDAO preguntasDAO = new PreguntasDAO();
    private final OpcionesRespuestaDAO opcionesDAO = new OpcionesRespuestaDAO();
    private final ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private final RespuestasJugadorDAO respuestasDAO = new RespuestasJugadorDAO();
    private final SalaDAO salaDAO = new SalaDAO();
    private final PuntajeService puntajeService = new PuntajeService();

    private List<Preguntas> preguntas;
    private final Map<Integer, JsonObject> respuestasPreguntaActual = new ConcurrentHashMap<>();
    private final List<Participantes> participantes = new CopyOnWriteArrayList<>();

    public GameRoomThread(Sala sala) {
        this.sala = sala;
        this.codigoSala = sala.getCodigoSala();
        System.out.println("🎮 GameRoomThread creado para sala: " + codigoSala);
    }

    @Override
    public void run() {
        System.out.println("🚀 Iniciando juego en sala: " + codigoSala);

        try {
            inicializar();

            // BUCLE PRINCIPAL - CADA PREGUNTA
            for (int i = 0; i < preguntas.size() && activo; i++) {
                Preguntas pregunta = preguntas.get(i);

                System.out.println("📝 Pregunta " + (i + 1) + "/" + preguntas.size());

                sala.setPreguntaActual(i + 1);
                salaDAO.actualizar(sala);

                respuestasPreguntaActual.clear();

                // 1. ENVIAR PREGUNTA
                enviarPregunta(pregunta, i + 1);

                // 2. ESPERAR RESPUESTAS (O TIMEOUT)
                esperarRespuestas(pregunta);

                // 3. PROCESAR RESPUESTAS Y CALCULAR PUNTOS
                calcularPuntajesPregunta(pregunta);

                // 4. ACTUALIZAR RANKING EN TIEMPO REAL
                actualizarRanking();

                // 5. PAUSA MUY CORTA (2 seg) antes de siguiente pregunta
                if (i < preguntas.size() - 1) {
                    Thread.sleep(2000); // Solo 2 segundos para ver explicación
                }
            }

            finalizarJuego();

        } catch (InterruptedException e) {
            System.out.println("⚠️ Juego interrumpido en sala: " + codigoSala);
        } catch (Exception e) {
            System.err.println("❌ Error en GameRoomThread: " + e.getMessage());
            e.printStackTrace();
        } finally {
            activo = false;
            System.out.println("🏁 Juego finalizado en sala: " + codigoSala);
        }
    }

    private void inicializar() {
        preguntas = preguntasDAO.listarPorTrivia(sala.getTrivia().getTriviaId());

        if (preguntas == null || preguntas.isEmpty()) {
            throw new RuntimeException("La trivia no tiene preguntas");
        }

        participantes.addAll(participantesDAO.listarActivosPorSala(sala.getSalaId()));

        System.out.println("✅ Inicializado: " + preguntas.size() + " preguntas, " +
                participantes.size() + " jugadores");
    }

    private void enviarPregunta(Preguntas pregunta, int numeroPregunta) {
        try {
            List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());

            JsonObject mensajePregunta = new JsonObject();
            mensajePregunta.addProperty("tipo", "PREGUNTA");
            mensajePregunta.addProperty("numeroPregunta", numeroPregunta);
            mensajePregunta.addProperty("totalPreguntas", preguntas.size());

            JsonObject dataPregunta = new JsonObject();
            dataPregunta.addProperty("preguntaId", pregunta.getPreguntaId());
            dataPregunta.addProperty("contenido", pregunta.getContenido());
            dataPregunta.addProperty("tipo", pregunta.getTipo());
            dataPregunta.addProperty("puntos", pregunta.getPuntos());
            dataPregunta.addProperty("limiteTiempo", pregunta.getLimiteTiempo());

            if (pregunta.getImagenPregunta() != null) {
                dataPregunta.addProperty("imagen", pregunta.getImagenPregunta());
            }

            JsonArray opcionesArray = new JsonArray();
            for (OpcionesRespuesta opcion : opciones) {
                JsonObject opcionJson = new JsonObject();
                opcionJson.addProperty("opcionId", opcion.getOpcionId());
                opcionJson.addProperty("textoOpcion", opcion.getTextoOpcion());
                opcionesArray.add(opcionJson);
            }
            dataPregunta.add("opciones", opcionesArray);

            mensajePregunta.add("pregunta", dataPregunta);

            GameWebSocket.broadcast(codigoSala, gson.toJson(mensajePregunta), null);

        } catch (Exception e) {
            System.err.println("❌ Error enviando pregunta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ⭐ MEJORADO: Termina INMEDIATAMENTE cuando todos responden
     */
    private void esperarRespuestas(Preguntas pregunta) throws InterruptedException {
        int limiteTiempo = pregunta.getLimiteTiempo() * 1000; // ms
        long tiempoInicio = System.currentTimeMillis();

        System.out.println("⏱️ Esperando respuestas (máx " + pregunta.getLimiteTiempo() + "s)...");

        while (System.currentTimeMillis() - tiempoInicio < limiteTiempo && activo) {
            Thread.sleep(100); // Check cada 100ms

            // ⭐ SI TODOS YA RESPONDIERON, TERMINAR INMEDIATAMENTE
            if (respuestasPreguntaActual.size() >= participantes.size()) {
                System.out.println("✅ TODOS RESPONDIERON - Procesando inmediatamente");
                return; // Salir del bucle
            }
        }

        System.out.println("⏱️ Tiempo agotado. Respuestas: " +
                respuestasPreguntaActual.size() + "/" + participantes.size());
    }

    private void calcularPuntajesPregunta(Preguntas pregunta) {
        System.out.println("🧮 Calculando puntajes...");

        List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());
        OpcionesRespuesta opcionCorrecta = opciones.stream()
                .filter(OpcionesRespuesta::getIsCorrecto)
                .findFirst()
                .orElse(null);

        if (opcionCorrecta == null) {
            System.err.println("❌ No hay respuesta correcta para pregunta: " + pregunta.getPreguntaId());
            return;
        }

        // Procesar TODAS las respuestas recibidas
        for (Map.Entry<Integer, JsonObject> entry : respuestasPreguntaActual.entrySet()) {
            Integer participanteId = entry.getKey();
            JsonObject respuestaData = entry.getValue();

            try {
                int opcionSeleccionadaId = respuestaData.get("opcionId").getAsInt();
                int tiempoTomado = respuestaData.get("tiempoTomado").getAsInt();

                boolean esCorrecta = (opcionSeleccionadaId == opcionCorrecta.getOpcionId());

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

                // Actualizar puntaje INMEDIATAMENTE
                puntajeService.actualizarPuntaje(participanteId, puntosGanados);
                puntajeService.registrarRespuesta(participanteId, esCorrecta);

                System.out.println("✅ Participante " + participanteId + ": " +
                        (esCorrecta ? "CORRECTA" : "INCORRECTA") + " → " + puntosGanados + " pts");

            } catch (Exception e) {
                System.err.println("❌ Error procesando respuesta de participante " + participanteId);
                e.printStackTrace();
            }
        }

        // Enviar respuesta correcta DESPUÉS de procesar
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

    /**
     * ⭐ ACTUALIZAR RANKING ORDENADO POR PUNTAJE EN TIEMPO REAL
     */
    private synchronized void actualizarRanking() {
        System.out.println("📊 Actualizando ranking...");

        // Obtener ranking ORDENADO por puntaje
        List<Participantes> ranking = puntajeService.obtenerRankingActual(sala.getSalaId());

        JsonObject mensajeRanking = new JsonObject();
        mensajeRanking.addProperty("tipo", "RANKING");

        JsonArray rankingArray = new JsonArray();
        int posicion = 1;
        for (Participantes p : ranking) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("posicion", posicion);
            jugador.addProperty("nickname", p.getNicknameJuego());
            jugador.addProperty("puntaje", p.getPuntajeFinal()); // ⭐ PUNTAJE TOTAL ACUMULADO
            jugador.addProperty("correctas", p.getPreguntaCorrecta());
            jugador.addProperty("respondidas", p.getPreguntaRespuesta());
            rankingArray.add(jugador);
            posicion++;
        }

        mensajeRanking.add("ranking", rankingArray);

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensajeRanking), null);

        System.out.println("✅ Ranking enviado (" + ranking.size() + " jugadores)");
    }

    private void finalizarJuego() {
        System.out.println("🏁 Finalizando juego...");

        puntajeService.calcularRanking(sala.getSalaId());

        List<Participantes> rankingFinal = puntajeService.obtenerRankingActual(sala.getSalaId());

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

        GameManager.getInstance().finalizarTrivia(codigoSala);
    }

    public void recibirRespuesta(JsonObject respuesta) {
        try {
            int participanteId = respuesta.get("participanteId").getAsInt();
            respuestasPreguntaActual.put(participanteId, respuesta);
            System.out.println("📩 Respuesta recibida de participante: " + participanteId +
                    " (" + respuestasPreguntaActual.size() + "/" + participantes.size() + ")");
        } catch (Exception e) {
            System.err.println("❌ Error recibiendo respuesta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void detener() {
        activo = false;
    }

    public boolean estaActivo() {
        return activo;
    }
}