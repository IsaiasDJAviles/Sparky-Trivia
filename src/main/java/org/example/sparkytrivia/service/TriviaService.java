package org.example.sparkytrivia.service;


import org.example.sparkytrivia.dao.TriviaDAO;
import org.example.sparkytrivia.dao.UsuarioDAO;
import org.example.sparkytrivia.model.Trivia;
import org.example.sparkytrivia.model.Usuario;

import java.util.List;

public class TriviaService {

    private TriviaDAO triviaDAO = new TriviaDAO();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    //CREAR UNA NUEVA TIVIA
    public Trivia crearTrivia(String titulo, String descripcion, Integer hostId, String categoria, String dificultad, Integer tiempoEstimado, String fotoPortada, Boolean esPublico) {

        //verificiar que el titulo no este vacio
        if(titulo == null || titulo.trim().isEmpty()) {
            throw new RuntimeException("titulo no puede ser nulo");
        }

        //verificar que el usuario exista
        Usuario host = usuarioDAO.buscarPorId(hostId);
        if(host == null) {
            throw new RuntimeException("Usuaurio no encontrado");
        }


        //creamos el obj trivia con campos obligatorios 
        Trivia trivia = new Trivia(titulo, descripcion, host);
        //ahora campos opcionales
        if(categoria != null){
            trivia.setCategoria(categoria);
        }
        if(dificultad != null){
            trivia.setDificultad(dificultad);
        }
        if(tiempoEstimado != null){
            trivia.setTiempoEstimado(tiempoEstimado);
        }
        if(fotoPortada != null){
            trivia.setFotoPortada(fotoPortada);
        }
        if(esPublico != null){
            trivia.setEsPublico(esPublico);
        }
        return triviaDAO.crear(trivia);
    }

    //ACTUALIZAR TRIVIA EXISTENTE
    public Trivia actualizarTrivia(Integer triviaId, Integer usuarioId, String titulo, String descripcion, String categoria, String dificultad,
                                   Integer tiempoEstimado, String fotoPortada, Boolean esPublico, String status) {
        //se busca la trivia que vamos act
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrado");
        }
        //solo el host puede editar la trivia
        if(!trivia.getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para editar esta trivia");
        }

        //se actualizan solo los campos que fueron enviados
        if (titulo != null && !titulo.trim().isEmpty()) {
            trivia.setTitulo(titulo);
        }
        if (descripcion != null) {
            trivia.setDescripcion(descripcion);
        }
        if (categoria != null) {
            trivia.setCategoria(categoria);
        }
        if (dificultad != null) {
            trivia.setDificultad(dificultad);
        }
        if (tiempoEstimado != null) {
            trivia.setTiempoEstimado(tiempoEstimado);
        }
        if (fotoPortada != null) {
            trivia.setFotoPortada(fotoPortada);
        }
        if (esPublico != null) {
            trivia.setEsPublico(esPublico);
        }
        if (status != null) {
            // Cambiar de "borrador" a "activo" para publicarla
            trivia.setStatus(status);
        }
        return triviaDAO.actualizar(trivia);//guardamos los cambios en la bd
    }


    //OBTENEMOS TRIVIA POR SU ID
    public Trivia obtenerTrivia(Integer triviaId) {
        Trivia trivia = triviaDAO.buscarPorId(triviaId);//buscamos la trivia
        //ver que si exista
        if(trivia == null) {
            throw new RuntimeException("Trivia no encontrado");
        }
        return trivia;
    }

    //LISTAR TODAS LAS TRIVIAS QUE CREÓ UN USER
    public List<Trivia> listarMisTrivias(Integer usuarioId){
        return triviaDAO.listarPorUsuario(usuarioId);
    }

    //LISTAR TRIVIAS PUBLICAS
    public List<Trivia> listarTriviasPublicas(){
        return triviaDAO.listarPublicas();
    }


    //ELIMINAR UNA TRIVIA
    public void eliminarTrivia(Integer triviaId, Integer usuarioId) {
        //buscar la trivia y ver si está
        Trivia trivia = triviaDAO.buscarPorId(triviaId);
        if (trivia == null) {
            throw new RuntimeException("Trivia no encontrada");
        }

        //solo el host puede eliminar su trivia
        if (!trivia.getHost().getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta trivia");
        }

        //eliminar de la BD, si hay preguntas asociadas, se eliminarán por CASCADE
        triviaDAO.eliminar(triviaId);
    }



}
