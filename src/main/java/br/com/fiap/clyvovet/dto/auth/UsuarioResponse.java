package br.com.fiap.clyvovet.dto.auth;

import br.com.fiap.clyvovet.model.Perfil;

import java.util.UUID;

/** Nunca inclui a senha, nem sequer o hash. */
public record UsuarioResponse(
        UUID id,
        String email,
        Perfil perfil,
        boolean ativo,
        UUID tutorId,
        String tutorNome,
        UUID veterinarioId,
        String veterinarioNome
) {}
