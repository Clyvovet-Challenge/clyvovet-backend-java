package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.dto.auth.RegistroRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Os campos do formulario "Criar conta".
 *
 * <p>Existe porque o {@link RegistroRequest} nao tem setters -- o Jackson o
 * preenche por reflection na API --, e o binding de formulario do Spring precisa
 * deles. As regras de tamanho sao as mesmas do request da API, de proposito: a
 * tela nao pode aceitar o que a API recusaria, nem o contrario.</p>
 *
 * <p>Nao ha campo de perfil. O que a pessoa escolhe aqui e nome, e-mail e senha;
 * o perfil e sempre TUTOR, decidido no service. Um {@code perfil=ADMIN} enviado
 * a mao no POST nao tem onde ser lido.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class CadastroForm {

    @NotBlank
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank
    @Email(message = "Informe um e-mail válido")
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotBlank(message = "Repita a senha")
    private String confirmacaoDaSenha;

    boolean senhasConferem() {
        return senha != null && senha.equals(confirmacaoDaSenha);
    }

    RegistroRequest comoRegistro() {
        return new RegistroRequest(email, senha, nome);
    }
}
