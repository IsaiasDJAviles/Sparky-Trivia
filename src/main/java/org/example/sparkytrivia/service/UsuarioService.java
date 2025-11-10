package org.example.sparkytrivia.service;

import org.example.sparkytrivia.dao.UsuarioDAO;
import org.example.sparkytrivia.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;

/**
* OBSERVACIONES:
* throw new RuntimeException:
*   Es para detener la ejecución y decirle al servlet "algo salió mal"
*
*
* */


public class UsuarioService {

    private UsuarioDAO usuarioDAO= new UsuarioDAO();
    //REGISTRO DE NUEVO USUARIO AL SISTEMA
    public Usuario registrar(String email, String password, String firstName, String lastName, String nickName) {
        //validar que el email no exista
        if(usuarioDAO.buscarPorEmail(email) != null){
            throw new RuntimeException(("El email ya existe"));
        }
        //validar que el nickname no exista
        if(usuarioDAO.buscarPorNickname(nickName) != null){
            throw new RuntimeException(("El nickname ya existe"));
        }
        //Hasheamos el pass por temas de seguridad
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        //creamso un usuario
        Usuario usuario = new Usuario(email, passwordHash, firstName, lastName, nickName);
        return usuarioDAO.crear(usuario);//Se guarda en la base de datos
    }

    //AUTENTICACION DE USUARIO CON EL LOGIN
    public Usuario autenticar(String email, String password) {
        Usuario usuario = usuarioDAO.buscarPorEmail(email);//se busca user por email
        //se verifica que exista
        if(usuario == null){
            throw new RuntimeException(("Credenciales invalidas"));
        }
        //Verificar password con BCrypt, esto se puso en las dependencias
        if(!BCrypt.checkpw(password, usuario.getPasswordHash())){
            throw new RuntimeException("Credenciales invalidas");
        }
        usuario.setFechaLogin(LocalDateTime.now());
        usuarioDAO.actualizar(usuario);
        return usuario;
    }

}
