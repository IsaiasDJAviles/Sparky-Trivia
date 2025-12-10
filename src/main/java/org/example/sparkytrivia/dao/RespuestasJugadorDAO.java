package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.RespuestasJugador;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

public class RespuestasJugadorDAO {

    // Crear una nueva respuesta
    public RespuestasJugador crear(RespuestasJugador respuesta) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(respuesta);
            em.getTransaction().commit();
            return respuesta;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al crear respuesta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Buscar respuesta por ID
    public RespuestasJugador buscarPorId(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(RespuestasJugador.class, id);
        } finally {
            em.close();
        }
    }

    // Buscar respuesta específica de un participante a una pregunta
    public RespuestasJugador buscarRespuesta(Integer participanteId, Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM RespuestasJugador r " +
                                    "WHERE r.participante.participanteId = :participanteId " +
                                    "AND r.pregunta.preguntaId = :preguntaId", RespuestasJugador.class)
                    .setParameter("participanteId", participanteId)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    // Listar todas las respuestas de una sala
    public List<RespuestasJugador> listarPorSala(Integer salaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM RespuestasJugador r WHERE r.sala.salaId = :salaId " +
                                    "ORDER BY r.respondioEn ASC", RespuestasJugador.class)
                    .setParameter("salaId", salaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Listar respuestas de un participante en una sala
    public List<RespuestasJugador> listarPorParticipante(Integer participanteId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM RespuestasJugador r " +
                                    "WHERE r.participante.participanteId = :participanteId " +
                                    "ORDER BY r.respondioEn ASC", RespuestasJugador.class)
                    .setParameter("participanteId", participanteId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Contar respuestas correctas de un participante
    public Long contarRespuestasCorrectas(Integer participanteId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(r) FROM RespuestasJugador r " +
                                    "WHERE r.participante.participanteId = :participanteId " +
                                    "AND r.esCorrecta = true", Long.class)
                    .setParameter("participanteId", participanteId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // Contar total de respuestas de un participante
    public Long contarRespuestas(Integer participanteId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(r) FROM RespuestasJugador r " +
                                    "WHERE r.participante.participanteId = :participanteId", Long.class)
                    .setParameter("participanteId", participanteId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // Contar cuántos han respondido una pregunta específica en una sala
    public Long contarRespuestasPorPregunta(Integer salaId, Integer preguntaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(r) FROM RespuestasJugador r " +
                                    "WHERE r.sala.salaId = :salaId " +
                                    "AND r.pregunta.preguntaId = :preguntaId", Long.class)
                    .setParameter("salaId", salaId)
                    .setParameter("preguntaId", preguntaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // Actualizar respuesta (por si se necesita modificar puntos)
    public RespuestasJugador actualizar(RespuestasJugador respuesta) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            RespuestasJugador updated = em.merge(respuesta);
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar respuesta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Calcular tiempo promedio de respuesta de un participante
    public Double calcularTiempoPromedio(Integer participanteId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT AVG(r.tiempoTomado) FROM RespuestasJugador r " +
                                    "WHERE r.participante.participanteId = :participanteId", Double.class)
                    .setParameter("participanteId", participanteId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}