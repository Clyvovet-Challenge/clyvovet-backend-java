package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends RepositorioBase<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado pelo AdminInicialSeeder: so cria o primeiro ADMIN se nao houver nenhum. */
    boolean existsByPerfil(Perfil perfil);

    /**
     * Soma uma falha no proprio banco. Ler, somar em Java e gravar perdia
     * incrementos quando as tentativas chegavam juntas: cinco senhas erradas em
     * paralelo contavam como uma, e o bloqueio nao vinha.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Usuario u SET u.tentativasFalhas = u.tentativasFalhas + 1 WHERE u.id = :id")
    int somarFalha(@Param("id") UUID id);

    default Usuario obterPorId(UUID id) {
        return obterPorId(id, Recurso.USUARIO);
    }
}
