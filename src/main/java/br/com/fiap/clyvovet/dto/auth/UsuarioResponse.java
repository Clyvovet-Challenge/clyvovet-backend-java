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
        String veterinarioNome,
        /*
         * A clinica deste usuario -- a que ele administra, ou a que ele atende.
         *
         * Sem este campo o ADMIN_CLINICA fazia login e nao tinha como descobrir por
         * QUAL clinica ele responde: o perfil existia, o vinculo estava no banco, e
         * o unico jeito de o app chegar ao proprio id de clinica era adivinhar.
         * Todas as rotas do painel comecam por ele.
         */
        UUID clinicaId,
        String clinicaNome
) {}
