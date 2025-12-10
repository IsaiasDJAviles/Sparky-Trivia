package org.example.sparkytrivia.service;

import org.example.sparkytrivia.dao.ParticipantesDAO;
import org.example.sparkytrivia.dao.SalaDAO;
import org.example.sparkytrivia.dao.TriviaDAO;
import org.example.sparkytrivia.dao.UsuarioDAO;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.model.Trivia;
import org.example.sparkytrivia.model.Usuario;

import java.util.List;
import java.util.Random;

public class SalaService {

    private SalaDAO salaDAO = new SalaDAO();
    private TriviaDAO triviaDAO = new TriviaDAO();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    private ParticipantesDAO participantesDAO = new ParticipantesDAO();

    /**
     * CREAR UNA NUEVA SALA
     * - Genera código único de 6 caracteres
     * - Valida que la trivia exista y tenga preguntas
     * - Agrega al host como primer participante
     */
    public Sala crearSala(String nombreSala, Integer triviaId, Integer hostId,
                          Integer maxUsuarios, Boolean esPublico, Boolean unirseDespues) {

        // Validar que la trivia exista
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrada");
        }

        // Validar que la trivia tenga al menos 1 pregunta
        if (trivia.getPreguntasTotales() == null || trivia.getPreguntasTotales() < 1) {
            throw new RuntimeException("La trivia debe tener al menos 1 pregunta para crear una sala");
        }

        // Validar que el usuario exista
        Usuario host = usuarioDAO.buscarPorId(hostId);
        if (host == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        // Generar código único de sala
        String codigoSala = generarCodigoUnico();

        // Crear objeto Sala
        Sala sala = new Sala(codigoSala, nombreSala, trivia, host);

        // Configurar opciones
        if (maxUsuarios != null) {
            sala.setMaxUsuario(maxUsuarios);
        }
        if (esPublico != null) {
            sala.setEsPublico(esPublico);
        }
        if (unirseDespues != null) {
            sala.setUnirseDespues(unirseDespues);
        }

        // Guardar sala en BD
        sala = salaDAO.crear(sala);

        // Agregar al host como primer participante
        Participantes hostParticipante = new Participantes(sala, host, true);
        participantesDAO.crear(hostParticipante);

        // Actualizar contador de usuarios actuales
        sala.setUsuariosActuales(1);
        salaDAO.actualizar(sala);

        return sala;
    }

    /**
     * UNIRSE A UNA SALA EXISTENTE
     * - Valida que la sala exista y esté disponible
     * - Verifica que no esté llena
     * - Verifica que el usuario no esté ya en la sala
     */
    public Participantes unirseASala(String codigoSala, Integer usuarioId) {

        // Buscar la sala por código
        Sala sala = salaDAO.buscarPorCodigo(codigoSala);
        if (sala == null) {
            throw new RuntimeException("Sala no encontrada. Verifica el código");
        }

        // Validar que el usuario exista
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        // Verificar que la sala esté en estado "esperando"
        if (!"esperando".equals(sala.getStatus())) {
            if ("en_progreso".equals(sala.getStatus()) && !sala.getUnirseDespues()) {
                throw new RuntimeException("La partida ya comenzó y no permite nuevos jugadores");
            }
            if ("completada".equals(sala.getStatus()) || "cancelada".equals(sala.getStatus())) {
                throw new RuntimeException("Esta sala ya finalizó");
            }
        }

        // Verificar que la sala no esté llena
        if (sala.getUsuariosActuales() >= sala.getMaxUsuario()) {
            throw new RuntimeException("La sala está llena");
        }

        // Verificar que el usuario no esté ya en la sala
        if (participantesDAO.usuarioEnSala(sala.getSalaId(), usuarioId)) {
            throw new RuntimeException("Ya estás en esta sala");
        }

        // Crear participante
        Participantes participante = new Participantes(sala, usuario, false);
        participante = participantesDAO.crear(participante);

        // Actualizar contador de usuarios actuales
        sala.setUsuariosActuales(sala.getUsuariosActuales() + 1);
        salaDAO.actualizar(sala);

        return participante;
    }

    /**
     * ABANDONAR UNA SALA
     */
    public void abandonarSala(Integer salaId, Integer usuarioId) {

        Sala sala = salaDAO.buscarPorId(salaId);
        if (sala == null) {
            throw new RuntimeException("Sala no encontrada");
        }

        Participantes participante = participantesDAO.buscarParticipante(salaId, usuarioId);
        if (participante == null) {
            throw new RuntimeException("No estás en esta sala");
        }

        // Marcar como inactivo
        participantesDAO.marcarComoInactivo(participante.getParticipanteId());

        // Actualizar contador
        sala.setUsuariosActuales(sala.getUsuariosActuales() - 1);
        salaDAO.actualizar(sala);

        // Si el host abandona, cancelar la sala
        if (participante.getEsHost()) {
            sala.setStatus("cancelada");
            salaDAO.actualizar(sala);
        }
    }

    /**
     * OBTENER DETALLES DE UNA SALA
     */
    public Sala obtenerSala(String codigoSala) {
        Sala sala = salaDAO.buscarPorCodigo(codigoSala);
        if (sala == null) {
            throw new RuntimeException("Sala no encontrada");
        }
        return sala;
    }

    /**
     * LISTAR PARTICIPANTES DE UNA SALA
     */
    public List<Participantes> listarParticipantes(Integer salaId) {
        return participantesDAO.listarActivosPorSala(salaId);
    }

    /**
     * LISTAR SALAS PÚBLICAS ACTIVAS
     */
    public List<Sala> listarSalasPublicas() {
        return salaDAO.listarPublicasActivas();
    }

    /**
     * INICIAR PARTIDA (solo el host puede hacerlo)
     */
    public void iniciarPartida(Integer salaId, Integer usuarioId) {

        Sala sala = salaDAO.buscarPorId(salaId);
        if (sala == null) {
            throw new RuntimeException("Sala no encontrada");
        }

        // Verificar que sea el host
        if (!sala.getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Solo el host puede iniciar la partida");
        }

        // Verificar que haya al menos 1 jugador (además del host)
        if (sala.getUsuariosActuales() < 1) {
            throw new RuntimeException("Se necesita al menos 1 jugador para iniciar");
        }

        // Cambiar estado a "en_progreso"
        sala.setStatus("en_progreso");
        sala.setInicio(java.time.LocalDateTime.now());
        salaDAO.actualizar(sala);
    }

    /**
     * GENERAR CÓDIGO ÚNICO DE 6 CARACTERES
     * Formato: XXXYYY (letras mayúsculas y números)
     */
    private String generarCodigoUnico() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String codigo;

        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
            }
            codigo = sb.toString();
        } while (salaDAO.existeCodigo(codigo)); // Asegurar que sea único

        return codigo;
    }

    /**
     * ACTUALIZAR SALA (para cambios de configuración)
     */
    public Sala actualizarSala(Sala sala) {
        return salaDAO.actualizar(sala);
    }
}