package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.Participantes;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

public class ParticipantesDAO {

    // Crear un nuevo participante
    public Participantes crear(Participantes participante) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(participante);
            em.getTransaction().commit();
            return participante;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al crear participante: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Buscar participante por ID
    public Participantes buscarPorId(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Participantes.class, id);
        } finally {
            em.close();
        }
    }

    // Buscar participante específico en una sala
    public Participantes buscarParticipante(Integer salaId, Integer usuarioId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "AND p.usuario.usuarioId = :usuarioId", Participantes.class)
                    .setParameter("salaId", salaId)
                    .setParameter("usuarioId", usuarioId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    // Listar todos los participantes de una sala
    public List<Participantes> listarPorSala(Integer salaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "ORDER BY p.unio ASC", Participantes.class)
                    .setParameter("salaId", salaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Listar participantes activos de una sala
    public List<Participantes> listarActivosPorSala(Integer salaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "AND p.esActivo = true " +
                                    "ORDER BY p.puntajeFinal DESC", Participantes.class)
                    .setParameter("salaId", salaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Obtener el ranking de una sala (ordenado por puntaje)
    public List<Participantes> obtenerRanking(Integer salaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "AND p.esActivo = true " +
                                    "ORDER BY p.puntajeFinal DESC, p.unio ASC", Participantes.class)
                    .setParameter("salaId", salaId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Contar participantes activos en una sala
    public Long contarActivosPorSala(Integer salaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(p) FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "AND p.esActivo = true", Long.class)
                    .setParameter("salaId", salaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // Actualizar participante
    public Participantes actualizar(Participantes participante) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Participantes updated = em.merge(participante);
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar participante: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Marcar participante como inactivo (abandonó la sala)
    public void marcarComoInactivo(Integer participanteId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Participantes participante = em.find(Participantes.class, participanteId);
            if (participante != null) {
                participante.setEsActivo(false);
                participante.setAbandono(java.time.LocalDateTime.now());
                em.merge(participante);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al marcar como inactivo: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Verificar si un usuario ya está en una sala
    public boolean usuarioEnSala(Integer salaId, Integer usuarioId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(p) FROM Participantes p WHERE p.sala.salaId = :salaId " +
                                    "AND p.usuario.usuarioId = :usuarioId " +
                                    "AND p.esActivo = true", Long.class)
                    .setParameter("salaId", salaId)
                    .setParameter("usuarioId", usuarioId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
}