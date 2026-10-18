package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.model.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Os campos do formulario "Cadastrar pet".
 *
 * <p>Existe porque o {@link AnimalRequest} nao tem setters -- o Jackson o preenche
 * por reflection na API --, e o binding de formulario precisa deles. As regras de
 * tamanho sao as mesmas do request da API, de proposito.</p>
 *
 * <p>Nao ha campo de dono, de especie nem de nome da raca. O dono e o tutor logado,
 * decidido no controller: um {@code tutorId} enviado a mao no POST nao tem onde ser
 * lido. A especie e o nome da raca vem do catalogo, pelo {@code racaId}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class PetForm {

    @NotBlank
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotNull(message = "Escolha a raça")
    private UUID racaId;

    @NotBlank(message = "Escolha o porte")
    @Pattern(regexp = "(?i)PEQUENO|MEDIO|GRANDE", message = "Porte deve ser pequeno, médio ou grande")
    private String porte;

    @NotBlank
    @Size(min = 3, max = 100, message = "A cor deve ter entre 3 e 100 caracteres")
    private String cor;

    @NotNull(message = "Escolha o sexo")
    private SexoAnimal sexo;

    @NotNull(message = "Informe a data de nascimento")
    @PastOrPresent(message = "Data de nascimento não pode ser futura")
    private LocalDate dataNascimento;

    @Size(max = 1000, message = "A observação aceita até 1000 caracteres")
    private String observacao;

    /**
     * Monta o pedido da API para o tutor logado. O nome da raca e a especie vao
     * vazios porque o service os sobrescreve com os do catalogo.
     */
    AnimalRequest comoPedido(UUID tutorId) {
        return new AnimalRequest(nome, null, racaId, null, porte, cor, sexo,
                dataNascimento, observacao, tutorId, null, null);
    }
}
