package br.com.fiap.clyvovet.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Auto-cadastro publico.
 *
 * Nao existe campo "perfil" de proposito: o perfil e sempre forcado para TUTOR
 * no service. Aceitar o perfil vindo do corpo permitiria que qualquer um se
 * cadastrasse como ADMIN — escalacao de privilegio por mass assignment.
 * Criar veterinario ou admin e operacao restrita, em POST /auth/usuarios.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RegistroRequest {

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    /** Tutor ja cadastrado a vincular. Opcional. */
    private UUID tutorId;
}
