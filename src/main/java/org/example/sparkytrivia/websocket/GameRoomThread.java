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

/**
 * HILO DE JUEGO PARA UNA SALA - VERSION CORREGIDA
 *
 * PROBLEMAS CORREGIDOS:
 * 1. Las respuestas no se guardaban porque el tiempo expiraba antes de recibirlas
 * 2. El ranking no se actualizaba porque registrarRespuesta no se llamaba
 * 3. Falta de logs para depuracion
 */
public class GameRoomThread implements Runnable {

    private final Sala sala;
    private final String codigoSala;
    private volatile boolean activo = true;
    private final Gson gson = new Gson();

    // DAOs
    private final PreguntasDAO preguntasDAO = new PreguntasDAO();
    private final OpcionesRespuestaDAO opcionesDAO = new OpcionesRespuestaDAO();
    private final ParticipantesDAO participantesDAO = new ParticipantesDAO();
    private final RespuestasJugadorDAO respuestasDAO = new RespuestasJugadorDAO();
    private final SalaDAO salaDAO = new SalaDAO();
    private final PuntajeService puntajeService = new PuntajeService();

    // Estado del juego
    private List<Preguntas> preguntas;
    private final Map<Integer, JsonObject> respuestasPreguntaActual = new ConcurrentHashMap<>();
    private final List<Participantes> participantes = new CopyOnWriteArrayList<>();

    // Control de pregunta actual
    private volatile int preguntaActualIndex = -1;
    private volatile Integer preguntaActualId = null;

    // CONFIGURACION DE TIEMPOS (en milisegundos)
    private static final int ESPERA_INICIAL = 5000;       // 5 segundos para countdown del frontend
    private static final int BUFFER_LATENCIA = 5000;      // 5 segundos extra para latencia (aumentado)
    private static final int PAUSA_ENTRE_PREGUNTAS = 4000; // 4 segundos entre preguntas

    public GameRoomThread(Sala sala) {
        this.sala = sala;
        this.codigoSala = sala.getCodigoSala();
        log("GameRoomThread creado para sala: " + codigoSala);
    }

    @Override
    public void run() {
        log("=== INICIANDO JUEGO EN SALA: " + codigoSala + " ===");

        try {
            inicializar();

            // Esperar a que el frontend termine su countdown
            log("Esperando " + (ESPERA_INICIAL/1000) + " segundos para sincronizar con clientes...");
            Thread.sleep(ESPERA_INICIAL);

            // BUCLE PRINCIPAL - CADA PREGUNTA
            for (int i = 0; i < preguntas.size() && activo; i++) {
                Preguntas pregunta = preguntas.get(i);

                // Actualizar estado
                preguntaActualIndex = i;
                preguntaActualId = pregunta.getPreguntaId();

                log("========================================");
                log("PREGUNTA " + (i + 1) + "/" + preguntas.size() + ": " + pregunta.getContenido());
                log("Pregunta ID: " + preguntaActualId);
                log("========================================");

                // Actualizar en BD
                sala.setPreguntaActual(i + 1);
                salaDAO.actualizar(sala);

                // Limpiar respuestas de la pregunta anterior
                respuestasPreguntaActual.clear();
                log("Respuestas limpiadas. Map size: " + respuestasPreguntaActual.size());

                // 1. ENVIAR PREGUNTA A TODOS LOS CLIENTES
                enviarPregunta(pregunta, i + 1);

                // 2. ESPERAR RESPUESTAS (tiempo limite + buffer de latencia)
                esperarRespuestas(pregunta);

                // 3. MOSTRAR RESPUESTAS RECIBIDAS
                log("--- RESPUESTAS RECIBIDAS: " + respuestasPreguntaActual.size() + " ---");
                for (Map.Entry<Integer, JsonObject> entry : respuestasPreguntaActual.entrySet()) {
                    log("  Participante " + entry.getKey() + ": " + entry.getValue().toString());
                }

                // 4. PROCESAR RESPUESTAS Y CALCULAR PUNTOS
                calcularPuntajesPregunta(pregunta);

                // 5. ENVIAR RESPUESTA CORRECTA
                enviarRespuestaCorrecta(pregunta);

                // 6. ACTUALIZAR RANKING EN TIEMPO REAL
                actualizarRanking();

                // 7. PAUSA ANTES DE LA SIGUIENTE PREGUNTA
                if (i < preguntas.size() - 1) {
                    log("Pausa de " + (PAUSA_ENTRE_PREGUNTAS/1000) + " segundos antes de siguiente pregunta...");
                    Thread.sleep(PAUSA_ENTRE_PREGUNTAS);
                }
            }

            finalizarJuego();

        } catch (InterruptedException e) {
            log("Juego interrumpido en sala: " + codigoSala);
        } catch (Exception e) {
            logError("Error en GameRoomThread: " + e.getMessage());
            e.printStackTrace();
        } finally {
            activo = false;
            preguntaActualIndex = -1;
            preguntaActualId = null;
            log("=== JUEGO FINALIZADO EN SALA: " + codigoSala + " ===");
        }
    }

    private void inicializar() {
        preguntas = preguntasDAO.listarPorTrivia(sala.getTrivia().getTriviaId());

        if (preguntas == null || preguntas.isEmpty()) {
            throw new RuntimeException("La trivia no tiene preguntas");
        }

        participantes.addAll(participantesDAO.listarActivosPorSala(sala.getSalaId()));

        log("Inicializado:");
        log("  - Preguntas: " + preguntas.size());
        log("  - Participantes: " + participantes.size());
        for (Participantes p : participantes) {
            log("    * " + p.getNicknameJuego() + " (ID: " + p.getParticipanteId() + ")");
        }
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

            String jsonMensaje = gson.toJson(mensajePregunta);
            log("Enviando pregunta " + numeroPregunta + " (limite: " + pregunta.getLimiteTiempo() + "s)");

            GameWebSocket.broadcast(codigoSala, jsonMensaje, null);

        } catch (Exception e) {
            logError("Error enviando pregunta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void esperarRespuestas(Preguntas pregunta) throws InterruptedException {
        // Tiempo total = tiempo limite + buffer para latencia
        int tiempoTotal = (pregunta.getLimiteTiempo() * 1000) + BUFFER_LATENCIA;
        long tiempoInicio = System.currentTimeMillis();

        log("Esperando respuestas (max " + pregunta.getLimiteTiempo() + "s + " +
                (BUFFER_LATENCIA/1000) + "s buffer = " + (tiempoTotal/1000) + "s total)...");

        int checkCount = 0;
        while (System.currentTimeMillis() - tiempoInicio < tiempoTotal && activo) {
            Thread.sleep(200); // Check cada 200ms
            checkCount++;

            // Log cada 5 segundos
            if (checkCount % 25 == 0) {
                long tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000;
                log("  [" + tiempoTranscurrido + "s] Respuestas: " +
                        respuestasPreguntaActual.size() + "/" + participantes.size());
            }

            // Si TODOS respondieron, terminar inmediatamente
            if (respuestasPreguntaActual.size() >= participantes.size()) {
                log("TODOS RESPONDIERON - Procesando inmediatamente");
                return;
            }
        }

        log("Tiempo agotado. Respuestas finales: " +
                respuestasPreguntaActual.size() + "/" + participantes.size());
    }

    private void calcularPuntajesPregunta(Preguntas pregunta) {
        log("--- CALCULANDO PUNTAJES ---");

        List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());
        OpcionesRespuesta opcionCorrecta = opciones.stream()
                .filter(OpcionesRespuesta::getIsCorrecto)
                .findFirst()
                .orElse(null);

        if (opcionCorrecta == null) {
            logError("No hay respuesta correcta para pregunta: " + pregunta.getPreguntaId());
            return;
        }

        log("Respuesta correcta: opcionId=" + opcionCorrecta.getOpcionId() +
                " (" + opcionCorrecta.getTextoOpcion() + ")");

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

                log("Procesando participante " + participanteId + ":");
                log("  - Opcion seleccionada: " + opcionSeleccionadaId);
                log("  - Tiempo tomado: " + tiempoTomado + "s");
                log("  - Es correcta: " + esCorrecta);
                log("  - Puntos ganados: " + puntosGanados);

                // Guardar respuesta en BD
                Participantes participante = participantesDAO.buscarPorId(participanteId);
                if (participante == null) {
                    logError("Participante no encontrado: " + participanteId);
                    continue;
                }

                OpcionesRespuesta opcionSeleccionada = opcionesDAO.buscarPorId(opcionSeleccionadaId);
                if (opcionSeleccionada == null) {
                    logError("Opcion no encontrada: " + opcionSeleccionadaId);
                    continue;
                }

                // CREAR Y GUARDAR RESPUESTA
                RespuestasJugador respuestaJugador = new RespuestasJugador(participante, pregunta, sala);
                respuestaJugador.setOpcionSeleccionada(opcionSeleccionada);
                respuestaJugador.setEsCorrecta(esCorrecta);
                respuestaJugador.setTiempoTomado(tiempoTomado);
                respuestaJugador.setPuntosGanados(puntosGanados);

                RespuestasJugador guardada = respuestasDAO.crear(respuestaJugador);
                log("  - Respuesta guardada en BD con ID: " +
                        (guardada != null ? guardada.getRespuestaId() : "NULL"));

                // ACTUALIZAR PUNTAJE Y CONTADORES DEL PARTICIPANTE
                puntajeService.actualizarPuntaje(participanteId, puntosGanados);
                puntajeService.registrarRespuesta(participanteId, esCorrecta);

                // Verificar que se actualizo
                Participantes actualizado = participantesDAO.buscarPorId(participanteId);
                log("  - Participante actualizado: puntaje=" + actualizado.getPuntajeFinal() +
                        ", correctas=" + actualizado.getPreguntaCorrecta() +
                        ", respondidas=" + actualizado.getPreguntaRespuesta());

            } catch (Exception e) {
                logError("Error procesando respuesta de participante " + participanteId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        log("--- FIN CALCULO PUNTAJES ---");
    }

    private void enviarRespuestaCorrecta(Preguntas pregunta) {
        List<OpcionesRespuesta> opciones = opcionesDAO.listarPorPregunta(pregunta.getPreguntaId());
        OpcionesRespuesta opcionCorrecta = opciones.stream()
                .filter(OpcionesRespuesta::getIsCorrecto)
                .findFirst()
                .orElse(null);

        if (opcionCorrecta == null) return;

        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "RESPUESTA_CORRECTA");
        mensaje.addProperty("preguntaId", pregunta.getPreguntaId());
        mensaje.addProperty("opcionCorrectaId", opcionCorrecta.getOpcionId());

        if (pregunta.getExplicacion() != null) {
            mensaje.addProperty("explicacion", pregunta.getExplicacion());
        }

        log("Enviando respuesta correcta: opcionId=" + opcionCorrecta.getOpcionId());
        GameWebSocket.broadcast(codigoSala, gson.toJson(mensaje), null);
    }

    private synchronized void actualizarRanking() {
        log("--- ACTUALIZANDO RANKING ---");

        // Obtener ranking ORDENADO por puntaje desde la BD
        List<Participantes> ranking = puntajeService.obtenerRankingActual(sala.getSalaId());

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

            log("  #" + posicion + " " + p.getNicknameJuego() + ": " +
                    p.getPuntajeFinal() + " pts, " +
                    p.getPreguntaCorrecta() + "/" + p.getPreguntaRespuesta() + " correctas");

            posicion++;
        }

        mensajeRanking.add("ranking", rankingArray);

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensajeRanking), null);
        log("Ranking enviado a " + ranking.size() + " jugadores");
    }

    private void finalizarJuego() {
        log("=== FINALIZANDO JUEGO ===");

        // Calcular ranking final
        puntajeService.calcularRanking(sala.getSalaId());

        List<Participantes> rankingFinal = puntajeService.obtenerRankingActual(sala.getSalaId());

        JsonObject mensajeFin = new JsonObject();
        mensajeFin.addProperty("tipo", "JUEGO_FINALIZADO");
        mensajeFin.addProperty("mensaje", "Juego terminado!");

        JsonArray rankingArray = new JsonArray();
        for (Participantes p : rankingFinal) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("rangoFinal", p.getRangoFinal());
            jugador.addProperty("nickname", p.getNicknameJuego());
            jugador.addProperty("puntajeFinal", p.getPuntajeFinal());
            jugador.addProperty("correctas", p.getPreguntaCorrecta());
            jugador.addProperty("respondidas", p.getPreguntaRespuesta());
            rankingArray.add(jugador);

            log("  Final #" + p.getRangoFinal() + " " + p.getNicknameJuego() + ": " +
                    p.getPuntajeFinal() + " pts");
        }

        mensajeFin.add("rankingFinal", rankingArray);

        GameWebSocket.broadcast(codigoSala, gson.toJson(mensajeFin), null);

        // Actualizar estado de la sala
        sala.setStatus("finalizado");
        sala.setFinalizacion(java.time.LocalDateTime.now());
        salaDAO.actualizar(sala);

        GameManager.getInstance().finalizarTrivia(codigoSala);
    }

    /**
     * RECIBIR RESPUESTA DE UN PARTICIPANTE
     * Este metodo es llamado desde GameManager cuando llega un mensaje RESPUESTA
     */
    public void recibirRespuesta(JsonObject respuesta) {
        try {
            int participanteId = respuesta.get("participanteId").getAsInt();
            int preguntaIdRecibida = respuesta.get("preguntaId").getAsInt();
            int opcionId = respuesta.get("opcionId").getAsInt();
            int tiempoTomado = respuesta.get("tiempoTomado").getAsInt();

            log(">>> RESPUESTA RECIBIDA <<<");
            log("  Participante ID: " + participanteId);
            log("  Pregunta ID recibida: " + preguntaIdRecibida);
            log("  Pregunta ID actual: " + preguntaActualId);
            log("  Opcion ID: " + opcionId);
            log("  Tiempo tomado: " + tiempoTomado + "s");

            // Validar que la respuesta sea para la pregunta actual
            if (preguntaActualId == null) {
                log("  RECHAZADA: No hay pregunta activa");
                return;
            }

            if (preguntaIdRecibida != preguntaActualId) {
                log("  RECHAZADA: Pregunta incorrecta (esperada: " + preguntaActualId + ")");
                return;
            }

            // Verificar si ya respondio
            if (respuestasPreguntaActual.containsKey(participanteId)) {
                log("  RECHAZADA: Ya respondio esta pregunta");
                return;
            }

            // Guardar respuesta en el mapa
            respuestasPreguntaActual.put(participanteId, respuesta);

            log("  ACEPTADA: Respuesta #" + respuestasPreguntaActual.size() +
                    " de " + participantes.size());

            // Enviar confirmacion al cliente
            enviarConfirmacionRespuesta(participanteId);

        } catch (Exception e) {
            logError("Error recibiendo respuesta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enviarConfirmacionRespuesta(int participanteId) {
        JsonObject confirmacion = new JsonObject();
        confirmacion.addProperty("tipo", "RESPUESTA_CONFIRMADA");
        confirmacion.addProperty("participanteId", participanteId);
        confirmacion.addProperty("mensaje", "Respuesta registrada");

        GameWebSocket.broadcast(codigoSala, gson.toJson(confirmacion), null);
    }

    public void detener() {
        activo = false;
    }

    public boolean estaActivo() {
        return activo;
    }

    // Metodos de logging
    private void log(String mensaje) {
        System.out.println("[GAME:" + codigoSala + "] " + mensaje);
    }

    private void logError(String mensaje) {
        System.err.println("[GAME:" + codigoSala + "] ERROR: " + mensaje);
    }
}