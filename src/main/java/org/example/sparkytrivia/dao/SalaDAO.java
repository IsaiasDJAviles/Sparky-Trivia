package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.Sala;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

public class SalaDAO {

    // Crear una nueva sala
    public Sala crear(Sala sala) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(sala);
            em.getTransaction().commit();
            return sala;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al crear sala: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Buscar sala por ID
    public Sala buscarPorId(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Sala.class, id);
        } finally {
            em.close();
        }
    }

    // Buscar sala por código (IMPORTANTE para unirse)
    public Sala buscarPorCodigo(String codigoSala) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM Sala s WHERE s.codigoSala = :codigo", Sala.class)
                    .setParameter("codigo", codigoSala)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    // Listar salas activas (esperando o en progreso)
    public List<Sala> listarActivas() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM Sala s WHERE s.status IN ('esperando', 'en_progreso') " +
                                    "ORDER BY s.fechaCreacion DESC", Sala.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Listar salas públicas activas
    public List<Sala> listarPublicasActivas() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM Sala s WHERE s.esPublico = true " +
                                    "AND s.status = 'esperando' " +
                                    "ORDER BY s.fechaCreacion DESC", Sala.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Listar salas creadas por un usuario (host)
    public List<Sala> listarPorUsuario(Integer usuarioId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM Sala s WHERE s.host.usuarioId = :usuarioId " +
                                    "ORDER BY s.fechaCreacion DESC", Sala.class)
                    .setParameter("usuarioId", usuarioId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Actualizar sala
    public Sala actualizar(Sala sala) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Sala updated = em.merge(sala);
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar sala: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Eliminar sala
    public void eliminar(Integer id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Sala sala = em.find(Sala.class, id);
            if (sala != null) {
                em.remove(sala);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al eliminar sala: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Verificar si un código ya existe (para evitar duplicados)
    public boolean existeCodigo(String codigo) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(s) FROM Sala s WHERE s.codigoSala = :codigo", Long.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
}