package br.com.fiap.clyvovet.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Auto-cadastro publico.
 *
 * Nao existe campo "perfil" de proposito: o perfil e sempre forcado para TUTOR
 * no service. Aceitar o perfil vindo do corpo permitiria que qualquer um se
 * cadastrasse como ADMIN — escalacao de privilegio por mass assignment.
 * Criar veterinario ou admin e operacao restrita, em POST /auth/usuarios.
 *
 * TAMBEM NAO EXISTE MAIS O CAMPO tutorId, e a ausencia dele e a correcao de
 * uma falha real (X13 da spec 08). Ele era aceito nesta rota, que e PUBLICA e
 * NAO AUTENTICADA: quem descobrisse o UUID de um tutor ja cadastrado se
 * registrava apontando para ele e passava a enxergar os animais, o historico
 * clinico e os pagamentos daquela pessoa — alem do CPF e do endereco dela.
 * Nenhuma verificacao de identidade acontecia no caminho.
 *
 * No lugar dele entrou o {@code nome}: o registro agora CRIA o tutor do
 * usuario, em vez de apontar para um que ja existe. Ver UsuarioService.
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

    /**
     * Nome de quem esta se cadastrando. Vira o nome do Tutor criado junto.
     *
     * E o unico dado de tutor exigido aqui porque e o unico NOT NULL da tabela.
     * CPF, telefone e endereco ficam para depois, num PATCH /tutores/{id} — a
     * alternativa seria um formulario de cadastro longo antes de a pessoa ver
     * qualquer valor no produto.
     */
    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
}
