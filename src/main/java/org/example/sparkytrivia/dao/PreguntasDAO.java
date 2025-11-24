package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.Preguntas;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

/**
 * DAO para la tabla Preguntas
 * Maneja operaciones CRUD y consultas relacionadas con preguntas de trivias
 */
public class PreguntasDAO {

    /**
     * Crear una nueva pregunta en la BD
     *
     * @param pregunta Objeto Pregunta a crear
     * @return La pregunta creada con su ID asignado
     */
    public Preguntas crear(Preguntas pregunta) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(pregunta); // Guardar la pregunta
            em.getTransaction().commit(); // Confirmar cambios
            return pregunta;
        } catch (Exception e) {
            // Si algo falla, deshacer cambios
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al crear pregunta: " + e.getMessage(), e);
        } finally {
            em.close(); // Cerrar conexión
        }
    }

    /**
     * Buscar una pregunta por su ID
     *
     * @param id ID de la pregunta
     * @return La pregunta encontrada o null si no existe
     */
    public Preguntas buscarPorId(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Buscar pregunta y cargar sus opciones de respuesta
            return em.createQuery(
                            "SELECT p FROM Preguntas p LEFT JOIN FETCH p.opciones WHERE p.preguntaId = :id",
                            Preguntas.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // No se encontró
        } finally {
            em.close();
        }
    }

    /**
     * Listar todas las preguntas de una trivia específica
     * Ordenadas por orderPregunta (1, 2, 3...)
     *
     * @param triviaId ID de la trivia
     * @return Lista de preguntas ordenadas
     */
    public List<Preguntas> listarPorTrivia(Integer triviaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Consulta JPQL que carga preguntas y sus opciones de respuesta
            return em.createQuery(
                            "SELECT DISTINCT p FROM Preguntas p " +
                                    "LEFT JOIN FETCH p.opciones " +
                                    "WHERE p.trivia.triviaId = :triviaId " +
                                    "ORDER BY p.orderPregunta ASC",
                            Preguntas.class)
                    .setParameter("triviaId", triviaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Contar cuántas preguntas tiene una trivia
     *
     * @param triviaId ID de la trivia
     * @return Número de preguntas
     */
    public Long contarPorTrivia(Integer triviaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(p) FROM Preguntas p WHERE p.trivia.triviaId = :triviaId",
                            Long.class)
                    .setParameter("triviaId", triviaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Obtener el siguiente número de orden disponible para una trivia
     * (Para asignar automáticamente orderPregunta)
     *
     * @param triviaId ID de la trivia
     * @return Siguiente número de orden (1, 2, 3...)
     */
    public Integer obtenerSiguienteOrden(Integer triviaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Obtener el máximo orderPregunta actual
            Integer maxOrden = em.createQuery(
                            "SELECT MAX(p.orderPregunta) FROM Preguntas p WHERE p.trivia.triviaId = :triviaId",
                            Integer.class)
                    .setParameter("triviaId", triviaId)
                    .getSingleResult();

            // Si no hay preguntas, empezar en 1, sino sumar 1
            return (maxOrden == null) ? 1 : maxOrden + 1;
        } finally {
            em.close();
        }
    }

    /**
     * Actualizar una pregunta existente
     *
     * @param pregunta Pregunta con los nuevos datos
     * @return Pregunta actualizada
     */
    public Preguntas actualizar(Preguntas pregunta) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Preguntas actualizada = em.merge(pregunta); // Actualizar en BD
            em.getTransaction().commit();
            return actualizada;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar pregunta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Eliminar una pregunta por su ID
     * CASCADE eliminará automáticamente sus opciones de respuesta
     *
     * @param id ID de la pregunta a eliminar
     */
    public void eliminar(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Preguntas pregunta = em.find(Preguntas.class, id);
            if (pregunta != null) {
                em.remove(pregunta); // Eliminar
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al eliminar pregunta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Reordenar preguntas de una trivia
     * Actualiza el orderPregunta de múltiples preguntas
     *
     * @param triviaId ID de la trivia
     * @param nuevoOrden Lista de IDs de preguntas en el nuevo orden deseado
     */
    public void reordenar(Integer triviaId, List<Integer> nuevoOrden) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            // Recorrer la lista y actualizar el orden de cada pregunta
            for (int i = 0; i < nuevoOrden.size(); i++) {
                Integer preguntaId = nuevoOrden.get(i);
                Integer nuevoOrderPregunta = i + 1; // 1, 2, 3, 4...

                // Actualizar directamente en la BD
                em.createQuery(
                                "UPDATE Preguntas p SET p.orderPregunta = :orden " +
                                        "WHERE p.preguntaId = :id AND p.trivia.triviaId = :triviaId")
                        .setParameter("orden", nuevoOrderPregunta)
                        .setParameter("id", preguntaId)
                        .setParameter("triviaId", triviaId)
                        .executeUpdate();
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al reordenar preguntas: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Verificar si una pregunta pertenece a una trivia específica
     * Útil para validaciones de seguridad
     *
     * @param preguntaId ID de la pregunta
     * @param triviaId ID de la trivia
     * @return true si la pregunta pertenece a esa trivia
     */
    public boolean perteneceATrivia(Integer preguntaId, Integer triviaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(p) FROM Preguntas p " +
                                    "WHERE p.preguntaId = :preguntaId AND p.trivia.triviaId = :triviaId",
                            Long.class)
                    .setParameter("preguntaId", preguntaId)
                    .setParameter("triviaId", triviaId)
                    .getSingleResult();

            return count > 0;
        } finally {
            em.close();
        }
    }
}