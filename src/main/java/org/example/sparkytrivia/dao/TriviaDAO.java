package org.example.sparkytrivia.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.sparkytrivia.model.Trivia;
import org.example.sparkytrivia.model.Usuario;
import org.example.sparkytrivia.util.JPAUtil;

import java.util.List;

//OPERACIONES CRUD
public class TriviaDAO {

    //Creamis nueva trivia en la BD
    public Trivia crear(Trivia trivia){
        EntityManager em = JPAUtil.getEntityManager(); //se obtiene conexion con la BD con JPA
        try{
            em.getTransaction().begin();
            em.persist(trivia);//se guarda la trivia en la bd
            em.getTransaction().commit();//se confirman cambios
            return trivia;//devuelve trivia
        }catch (Exception e){
            //Si algo sale mal, se deshacen los cmabios
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }//lanza la excepcion
            throw  new RuntimeException("Error al crear la trivia: "+e.getMessage(), e);
        }finally {
            em.close();//cierra conexion
        }
    }

    //BUSCAR TRIVIA POR ID
    public Trivia buscarPorId(Integer id){
        EntityManager em = JPAUtil.getEntityManager();//obtenemos comexion con la bd
        try{
            //devuelve la trivia o null si no exite
            return em.find(Trivia.class, id);
        }finally {
            em.close();//cierra conexion
        }
    }

    //LSITAR TODAS LAS TRIVIAS
    public List<Trivia> listarTodas(){
        EntityManager em = JPAUtil.getEntityManager();//obtenemso conexion
        try{
            return em.createQuery("SELECT t FROM Trivia t", Trivia.class).getResultList();
        }finally {
            em.close();
        }
    }

    //LISTAR TRIVIAS CREADAS POR UN USUARIO ESPECIFICO
    public List<Trivia> listarPorUsuario(Integer usuarioId){
        EntityManager em= JPAUtil.getEntityManager();

        try{
            return em.createQuery(
                    "SELECT t FROM Trivia t WHERE t.host.usuarioId = :usuarioId", Trivia.class)
                    .setParameter("usuarioId", usuarioId) // Reemplaza :usuarioId con el valor
                    .getResultList();
        }finally {
            em.close();
        }
    }


    //LISTAR SOLO TRIVIAS PUBLICAS
    public List<Trivia> listarPublicas(){
        EntityManager em= JPAUtil.getEntityManager();
        try{
            return em.createQuery("SELECT t FROM Trivia t WHERE t.esPublico = true", Trivia.class).getResultList();
        }finally {
            em.close();
        }
    }

    //LISTAR TRIVIAS POR CATEGORIA
    public List<Trivia> listarPorCategoria(String categoria){
        EntityManager em= JPAUtil.getEntityManager();
        try{
            return em.createQuery("SELECT t FROM Trivia t WHERE t.categoria = :categoria", Trivia.class).setParameter("categoria", categoria).getResultList();
        }finally {
            em.close();
        }
    }

    //ACTUALIZAR TRIVIA EXISTENTE
    public Trivia actualizar(Trivia trivia){
        EntityManager em = JPAUtil.getEntityManager();
        try{
            em.getTransaction().begin();
            Trivia update= em.merge(trivia);
            em.getTransaction().commit();
            return update;
        } catch (Exception e) {
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al actualizar trivia: "+e.getMessage(), e);
        }finally {
            em.close();
        }
    }

    //ELIMINAR UNA TRIVIA POR ID
    public void eliminar(Integer id){
        EntityManager em = JPAUtil.getEntityManager();
        try{
            em.getTransaction().begin();
            Trivia trivia = em.find(Trivia.class, id);
            if (trivia != null){
                em.remove(trivia);
            }
            em.getTransaction().commit();
        }catch (Exception e){
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al eliminar trivia: "+e.getMessage(), e);
        }finally {
            em.close();
        }
    }


}
