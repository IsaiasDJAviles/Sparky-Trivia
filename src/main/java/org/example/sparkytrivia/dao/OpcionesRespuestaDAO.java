package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.OpcionesRespuesta;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

/**
 * DAO para la tabla OpcionesRespuesta
 * Maneja operaciones CRUD de las opciones de respuesta de cada pregunta
 */
public class OpcionesRespuestaDAO {

    /**
     * Crear una nueva opción de respuesta
     *
     * @param opcion Objeto OpcionRespuesta a crear
     * @return La opción creada con su ID asignado
     */
    public static OpcionesRespuesta crear(OpcionesRespuesta opcion) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(opcion); // Guardar en BD
            em.getTransaction().commit();
            return opcion;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al crear opción de respuesta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Buscar una opción por su ID
     *
     * @param id ID de la opción
     * @return La opción encontrada o null si no existe
     */
    public OpcionesRespuesta buscarPorId(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(OpcionesRespuesta.class, id);
        } finally {
            em.close();
        }
    }

    /**
     * Listar todas las opciones de una pregunta específica
     * Ordenadas por orderPregunta (1, 2, 3, 4 → a, b, c, d)
     *
     * @param preguntaId ID de la pregunta
     * @return Lista de opciones ordenadas
     */
    public List<OpcionesRespuesta> listarPorPregunta(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT o FROM OpcionesRespuesta o " +
                                    "WHERE o.pregunta.preguntaId = :preguntaId " +
                                    "ORDER BY o.orderPregunta ASC",
                            OpcionesRespuesta.class)
                    .setParameter("preguntaId", preguntaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Obtener la opción correcta de una pregunta
     *
     * @param preguntaId ID de la pregunta
     * @return La opción marcada como correcta, o null si no hay
     */
    public OpcionesRespuesta obtenerOpcionCorrecta(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT o FROM OpcionesRespuesta o " +
                                    "WHERE o.pregunta.preguntaId = :preguntaId AND o.isCorrecto = true",
                            OpcionesRespuesta.class)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // No hay respuesta correcta marcada
        } finally {
            em.close();
        }
    }

    /**
     * Contar cuántas opciones tiene una pregunta
     *
     * @param preguntaId ID de la pregunta
     * @return Número de opciones
     */
    public Long contarPorPregunta(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(o) FROM OpcionesRespuesta o WHERE o.pregunta.preguntaId = :preguntaId",
                            Long.class)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Contar cuántas opciones correctas tiene una pregunta
     * Debe ser exactamente 1 para preguntas de opción múltiple
     *
     * @param preguntaId ID de la pregunta
     * @return Número de opciones marcadas como correctas
     */
    public Long contarOpcionesCorrectas(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(o) FROM OpcionesRespuesta o " +
                                    "WHERE o.pregunta.preguntaId = :preguntaId AND o.isCorrecto = true",
                            Long.class)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Obtener el siguiente número de orden disponible para una pregunta
     *
     * @param preguntaId ID de la pregunta
     * @return Siguiente número de orden (1, 2, 3, 4...)
     */
    public Integer obtenerSiguienteOrden(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Integer maxOrden = em.createQuery(
                            "SELECT MAX(o.orderPregunta) FROM OpcionesRespuesta o " +
                                    "WHERE o.pregunta.preguntaId = :preguntaId",
                            Integer.class)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();

            return (maxOrden == null) ? 1 : maxOrden + 1;
        } finally {
            em.close();
        }
    }

    /**
     * Actualizar una opción existente
     *
     * @param opcion Opción con los nuevos datos
     * @return Opción actualizada
     */
    public OpcionesRespuesta actualizar(OpcionesRespuesta opcion) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            OpcionesRespuesta actualizada = em.merge(opcion);
            em.getTransaction().commit();
            return actualizada;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar opción: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Eliminar una opción por su ID
     *
     * @param id ID de la opción a eliminar
     */
    public void eliminar(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            OpcionesRespuesta opcion = em.find(OpcionesRespuesta.class, id);
            if (opcion != null) {
                em.remove(opcion);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al eliminar opción: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Eliminar todas las opciones de una pregunta
     * Útil al eliminar una pregunta completa
     *
     * @param preguntaId ID de la pregunta
     */
    public static void eliminarPorPregunta(Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM OpcionesRespuesta o WHERE o.pregunta.preguntaId = :preguntaId")
                    .setParameter("preguntaId", preguntaId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al eliminar opciones: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Marcar una opción como correcta y desmarcar las demás de la misma pregunta
     * Garantiza que solo haya UNA respuesta correcta
     *
     * @param opcionId ID de la opción a marcar como correcta
     */
    public void marcarComoCorrecta(Integer opcionId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            // Buscar la opción
            OpcionesRespuesta opcion = em.find(OpcionesRespuesta.class, opcionId);
            if (opcion != null) {
                Integer preguntaId = opcion.getPregunta().getPreguntaId();

                // Desmarcar todas las opciones de esta pregunta
                em.createQuery(
                                "UPDATE OpcionesRespuesta o SET o.isCorrecto = false " +
                                        "WHERE o.pregunta.preguntaId = :preguntaId")
                        .setParameter("preguntaId", preguntaId)
                        .executeUpdate();

                // Marcar solo esta como correcta
                opcion.setIsCorrecto(true);
                em.merge(opcion);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al marcar opción correcta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}