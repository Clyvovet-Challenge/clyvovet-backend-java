package br.com.fiap.clyvovet.dto.auth;

import br.com.fiap.clyvovet.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Criacao de usuario com perfil arbitrario. Restrito a ADMIN.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UsuarioRequest {

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotNull
    private Perfil perfil;

    private UUID tutorId;

    private UUID veterinarioId;
}
