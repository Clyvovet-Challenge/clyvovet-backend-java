package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends RepositorioBase<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado pelo AdminInicialSeeder: so cria o primeiro ADMIN se nao houver nenhum. */
    boolean existsByPerfil(Perfil perfil);

    default Usuario obterPorId(UUID id) {
        return obterPorId(id, Recurso.USUARIO);
    }
}
