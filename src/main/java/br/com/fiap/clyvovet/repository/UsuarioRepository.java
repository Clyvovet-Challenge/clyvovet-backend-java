package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends RepositorioBase<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    default Usuario obterPorId(UUID id) {
        return obterPorId(id, Recurso.USUARIO);
    }
}
