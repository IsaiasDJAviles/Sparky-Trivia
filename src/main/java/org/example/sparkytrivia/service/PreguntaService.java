package org.example.sparkytrivia.service;

import org.example.sparkytrivia.dao.OpcionesRespuestaDAO;
import org.example.sparkytrivia.dao.PreguntasDAO;
import org.example.sparkytrivia.dao.TriviaDAO;
import org.example.sparkytrivia.model.OpcionesRespuesta;
import org.example.sparkytrivia.model.Preguntas;
import org.example.sparkytrivia.model.Trivia;

import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de Preguntas y OpcionesRespuesta
 * Contiene todas las validaciones y reglas de negocio
 */
public class PreguntaService {

    // DAOs necesarios
    private PreguntasDAO preguntasDAO = new PreguntasDAO();
    private OpcionesRespuestaDAO opcionesDAO = new OpcionesRespuestaDAO();
    private TriviaDAO triviaDAO = new TriviaDAO();

    // ========== CREAR PREGUNTA ==========

    /**
     * Crear una nueva pregunta con sus opciones de respuesta
     *
     * @param triviaId ID de la trivia a la que pertenece
     * @param usuarioId ID del usuario que está creando (debe ser el host)
     * @param contenido Texto de la pregunta
     * @param tipo Tipo de pregunta (opcion_multiple, verdadero_falso, abierta)
     * @param puntos Puntos que vale la pregunta
     * @param limiteTiempo Tiempo límite en segundos
     * @param dificultad Dificultad (facil, medio, dificil)
     * @param imagenPregunta URL de imagen opcional
     * @param explicacion Explicación opcional
     * @param opciones Lista de opciones de respuesta (mínimo 2)
     * @return La pregunta creada con sus opciones
     */
    public Preguntas crearPregunta(
            Integer triviaId,
            Integer usuarioId,
            String contenido,
            String tipo,
            Integer puntos,
            Integer limiteTiempo,
            String dificultad,
            String imagenPregunta,
            String explicacion,
            List<OpcionRespuestaDTO> opciones) {

        // ===== VALIDACIONES =====

        // 1. Validar que el contenido no esté vacío
        if (contenido == null || contenido.trim().isEmpty()) {
            throw new RuntimeException("El contenido de la pregunta es obligatorio");
        }

        // 2. Verificar que la trivia existe
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrada");
        }

        // 3. Validar que el usuario es el host de la trivia
        if (!trivia.getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Solo el host puede agregar preguntas a esta trivia");
        }

        // 4. Validar opciones de respuesta
        if (opciones == null || opciones.size() < 2) {
            throw new RuntimeException("Debe haber al menos 2 opciones de respuesta");
        }

        if (opciones.size() > 6) {
            throw new RuntimeException("Máximo 6 opciones de respuesta permitidas");
        }

        // 5. Validar que haya exactamente UNA respuesta correcta
        long opcionesCorrectas = opciones.stream()
                .filter(OpcionRespuestaDTO::getIsCorrecto)
                .count();

        if (opcionesCorrectas != 1) {
            throw new RuntimeException("Debe haber exactamente 1 respuesta correcta");
        }

        // 6. Validar que ninguna opción esté vacía
        boolean hayOpcionVacia = opciones.stream()
                .anyMatch(o -> o.getTextoOpcion() == null || o.getTextoOpcion().trim().isEmpty());

        if (hayOpcionVacia) {
            throw new RuntimeException("Todas las opciones deben tener texto");
        }

        // ===== CREAR PREGUNTA =====

        // Obtener el siguiente número de orden
        Integer siguienteOrden = preguntasDAO.obtenerSiguienteOrden(triviaId);

        // Crear objeto Pregunta
        Preguntas pregunta = new Preguntas(trivia, siguienteOrden, contenido);

        // Establecer campos opcionales
        if (tipo != null) {
            pregunta.setTipo(tipo);
        }
        if (puntos != null) {
            pregunta.setPuntos(puntos);
        }
        if (limiteTiempo != null) {
            pregunta.setLimiteTiempo(limiteTiempo);
        }
        if (dificultad != null) {
            pregunta.setDificultad(dificultad);
        }
        if (imagenPregunta != null) {
            pregunta.setImagenPregunta(imagenPregunta);
        }
        if (explicacion != null) {
            pregunta.setExplicacion(explicacion);
        }

        // Guardar pregunta en BD
        pregunta = preguntasDAO.crear(pregunta);

        // ===== CREAR OPCIONES DE RESPUESTA =====

        for (int i = 0; i < opciones.size(); i++) {
            OpcionRespuestaDTO dto = opciones.get(i);

            // Crear opción
            OpcionesRespuesta opcion = new OpcionesRespuesta(
                    pregunta,
                    i + 1, // orderPregunta: 1, 2, 3, 4...
                    dto.getTextoOpcion(),
                    dto.getIsCorrecto()
            );

            // Guardar opción
            opcionesDAO.crear(opcion);
        }

        // ===== ACTUALIZAR CONTADOR DE PREGUNTAS EN TRIVIA =====

        trivia.setPreguntasTotales(trivia.getPreguntasTotales() + 1);
        triviaDAO.actualizar(trivia);

        // Retornar la pregunta creada (con opciones cargadas)
        return preguntasDAO.buscarPorId(pregunta.getPreguntaId());
    }

    // ========== OBTENER PREGUNTA ==========

    /**
     * Obtener una pregunta por su ID con todas sus opciones
     *
     * @param preguntaId ID de la pregunta
     * @return La pregunta con sus opciones
     */
    public Preguntas obtenerPregunta(Integer preguntaId) {
        Preguntas pregunta = preguntasDAO.buscarPorId(preguntaId);
        if (pregunta == null) {
            throw new RuntimeException("Pregunta no encontrada");
        }
        return pregunta;
    }

    // ========== LISTAR PREGUNTAS DE UNA TRIVIA ==========

    /**
     * Listar todas las preguntas de una trivia
     *
     * @param triviaId ID de la trivia
     * @return Lista de preguntas ordenadas
     */
    public List<Preguntas> listarPreguntasPorTrivia(Integer triviaId) {
        // Verificar que la trivia existe
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrada");
        }

        return preguntasDAO.listarPorTrivia(triviaId);
    }

    // ========== ACTUALIZAR PREGUNTA ==========

    /**
     * Actualizar una pregunta existente
     *
     * @param preguntaId ID de la pregunta a actualizar
     * @param usuarioId ID del usuario (debe ser el host)
     * @param contenido Nuevo contenido
     * @param tipo Nuevo tipo
     * @param puntos Nuevos puntos
     * @param limiteTiempo Nuevo tiempo límite
     * @param dificultad Nueva dificultad
     * @param imagenPregunta Nueva imagen
     * @param explicacion Nueva explicación
     * @param opciones Nuevas opciones (opcional)
     * @return Pregunta actualizada
     */
    public Preguntas actualizarPregunta(Integer preguntaId, Integer usuarioId,
                                        String contenido, String tipo, Integer puntos,
                                        Integer limiteTiempo, String dificultad,
                                        String imagenPregunta, String explicacion,
                                        List<OpcionRespuestaDTO> opciones) {

        // Buscar la pregunta existente
        Preguntas pregunta = preguntasDAO.buscarPorId(preguntaId);
        if (pregunta == null) {
            throw new RuntimeException("Pregunta no encontrada");
        }

        // Verificar que el usuario sea el host de la trivia
        if (!pregunta.getTrivia().getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para editar esta pregunta");
        }

        // Actualizar campos de la pregunta (solo si fueron enviados)
        if (contenido != null && !contenido.trim().isEmpty()) {
            pregunta.setContenido(contenido);
        }
        if (tipo != null) {
            pregunta.setTipo(tipo);
        }
        if (puntos != null) {
            pregunta.setPuntos(puntos);
        }
        if (limiteTiempo != null) {
            pregunta.setLimiteTiempo(limiteTiempo);
        }
        if (dificultad != null) {
            pregunta.setDificultad(dificultad);
        }
        if (imagenPregunta != null) {
            pregunta.setImagenPregunta(imagenPregunta);
        }
        if (explicacion != null) {
            pregunta.setExplicacion(explicacion);
        }

        // Si se enviaron opciones nuevas, reemplazar las actuales
        if (opciones != null && !opciones.isEmpty()) {
            // Validar opciones
            if (opciones.size() < 2 || opciones.size() > 6) {
                throw new RuntimeException("Debe haber entre 2 y 6 opciones");
            }

            // Verificar que haya exactamente 1 respuesta correcta
            long correctas = opciones.stream().filter(o -> o.isCorrecto).count();
            if (correctas != 1) {
                throw new RuntimeException("Debe haber exactamente 1 respuesta correcta");
            }

            // SOLUCIÓN: Eliminar opciones antiguas PRIMERO, antes de actualizar
            OpcionesRespuestaDAO.eliminarPorPregunta(preguntaId);

            // Limpiar la colección de opciones en la entidad
            pregunta.getOpciones().clear();

            // Actualizar la pregunta SIN las opciones (para hacer flush de la eliminación)
            pregunta = preguntasDAO.actualizar(pregunta);

            // AHORA crear las opciones nuevas
            int orden = 1;
            for (OpcionRespuestaDTO dto : opciones) {
                if (dto.textoOpcion == null || dto.textoOpcion.trim().isEmpty()) {
                    throw new RuntimeException("Las opciones no pueden estar vacías");
                }

                OpcionesRespuesta opcion = new OpcionesRespuesta();
                opcion.setPregunta(pregunta);  // Asociar con la pregunta actualizada
                opcion.setOrderPregunta(orden++);
                opcion.setTextoOpcion(dto.textoOpcion.trim());
                opcion.setIsCorrecto(dto.isCorrecto);

                // Crear cada opción individualmente
                opcion = OpcionesRespuestaDAO.crear(opcion);

                // Agregar a la colección de la pregunta
                pregunta.getOpciones().add(opcion);
            }
        }

        // Guardar cambios finales
        return preguntasDAO.actualizar(pregunta);
    }

    // ========== ELIMINAR PREGUNTA ==========

    /**
     * Eliminar una pregunta
     *
     * @param preguntaId ID de la pregunta
     * @param usuarioId ID del usuario (debe ser el host)
     */
    public void eliminarPregunta(Integer preguntaId, Integer usuarioId) {
        // Buscar pregunta
        Preguntas pregunta = preguntasDAO.buscarPorId(preguntaId);
        if (pregunta == null) {
            throw new RuntimeException("Pregunta no encontrada");
        }

        // Validar permisos
        if (!pregunta.getTrivia().getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Solo el host puede eliminar esta pregunta");
        }

        // Obtener la trivia para actualizar contador
        Trivia trivia = pregunta.getTrivia();

        // Eliminar pregunta (CASCADE eliminará sus opciones)
        preguntasDAO.eliminar(preguntaId);

        // Actualizar contador de preguntas en trivia
        trivia.setPreguntasTotales(trivia.getPreguntasTotales() - 1);
        triviaDAO.actualizar(trivia);
    }

    // ========== REORDENAR PREGUNTAS ==========

    /**
     * Cambiar el orden de las preguntas de una trivia
     *
     * @param triviaId ID de la trivia
     * @param usuarioId ID del usuario (debe ser el host)
     * @param nuevoOrden Lista de IDs de preguntas en el nuevo orden
     */
    public void reordenarPreguntas(Integer triviaId, Integer usuarioId, List<Integer> nuevoOrden) {
        // Verificar que la trivia existe
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrada");
        }

        // Validar permisos
        if (!trivia.getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Solo el host puede reordenar las preguntas");
        }

        // Validar que todas las preguntas pertenecen a esta trivia
        for (Integer preguntaId : nuevoOrden) {
            if (!preguntasDAO.perteneceATrivia(preguntaId, triviaId)) {
                throw new RuntimeException("Una o más preguntas no pertenecen a esta trivia");
            }
        }

        // Reordenar
        preguntasDAO.reordenar(triviaId, nuevoOrden);
    }

    // ========== VALIDAR PREGUNTA PARA JUEGO ==========

    /**
     * Validar que una pregunta está lista para ser jugada
     *
     * @param preguntaId ID de la pregunta
     * @return true si es válida para jugar
     */
    public boolean validarPreguntaParaJuego(Integer preguntaId) {
        Preguntas pregunta = preguntasDAO.buscarPorId(preguntaId);
        if (pregunta == null) {
            return false;
        }

        // Debe tener contenido
        if (pregunta.getContenido() == null || pregunta.getContenido().trim().isEmpty()) {
            return false;
        }

        // Debe tener al menos 2 opciones
        Long cantidadOpciones = opcionesDAO.contarPorPregunta(preguntaId);
        if (cantidadOpciones < 2) {
            return false;
        }

        // Debe tener exactamente 1 respuesta correcta
        Long opcionesCorrectas = opcionesDAO.contarOpcionesCorrectas(preguntaId);
        if (opcionesCorrectas != 1) {
            return false;
        }

        return true;
    }

    // ========== VALIDAR TRIVIA COMPLETA ==========

    /**
     * Validar que una trivia está lista para jugar
     * Todas sus preguntas deben ser válidas
     *
     * @param triviaId ID de la trivia
     * @return true si está lista para jugar
     */
    public boolean validarTriviaParaJuego(Integer triviaId) {
        // Debe tener al menos 1 pregunta
        Long cantidadPreguntas = preguntasDAO.contarPorTrivia(triviaId);
        if (cantidadPreguntas == 0) {
            return false;
        }

        // Todas las preguntas deben ser válidas
        List<Preguntas> preguntas = preguntasDAO.listarPorTrivia(triviaId);
        for (Preguntas pregunta : preguntas) {
            if (!validarPreguntaParaJuego(pregunta.getPreguntaId())) {
                return false;
            }
        }

        return true;
    }

    // ========== DTO INTERNO ==========

    /**
     * DTO para recibir datos de opciones de respuesta desde el frontend
     */
    public static class OpcionRespuestaDTO {
        private String textoOpcion;
        private Boolean isCorrecto;

        public OpcionRespuestaDTO() {
        }

        public OpcionRespuestaDTO(String textoOpcion, Boolean isCorrecto) {
            this.textoOpcion = textoOpcion;
            this.isCorrecto = isCorrecto;
        }

        public String getTextoOpcion() {
            return textoOpcion;
        }

        public void setTextoOpcion(String textoOpcion) {
            this.textoOpcion = textoOpcion;
        }

        public Boolean getIsCorrecto() {
            return isCorrecto;
        }

        public void setIsCorrecto(Boolean isCorrecto) {
            this.isCorrecto = isCorrecto;
        }
    }
}