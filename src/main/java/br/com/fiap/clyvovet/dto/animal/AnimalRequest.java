package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AnimalRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
    @NotBlank
    @Size(min = 3, max = 100)
    private String raca;
    @NotBlank
    @Size(min = 3, max = 100)
    private String especie;
    @NotBlank
    @Size(min = 3, max = 100)
    private String porte;
    @NotBlank
    @Size(min = 3, max = 100)
    private String cor;
    @NotNull
    private SexoAnimal sexo;
    @NotNull
    /**
     * Nenhum dos sete DTOs com data de nascimento validava isto, e o resultado era
     * aceitar um cadastro nascido em 2999 — verificado contra a pilha local, um
     * animal com dataNascimento 2999-01-01 entrava com 201.
     *
     * O projeto já usava @PastOrPresent nos DTOs de pagamento; aqui a anotação
     * estava só faltando. Ela ignora nulo, então continua valendo como campo
     * opcional no PATCH.
     */
    @PastOrPresent(message = "Data de nascimento não pode ser futura")
    private LocalDate dataNascimento;
    // Limite igual ao da coluna, VARCHAR2(1000): sem ele um texto maior passa
    // pela validacao e so falha no INSERT, virando erro de servidor.
    @Size(max = 1000)
    private String observacao;
    @NotNull
    private UUID tutorId;

    /**
     * Numero do microchip, padrao ISO 11784/11785.
     *
     * Opcional: animal sem chip e comum, e o indice unico do banco ignora
     * nulos, entao varios convivem. Dois com o MESMO chip, nao -- seriam dois
     * animais com a mesma identidade no balcao, e o resumo de seguranca do
     * errado.
     */
    @Pattern(regexp = "\\d{15}", message = "Microchip deve ter 15 dígitos")
    private String microchip;

    /** Compoe o resumo de seguranca. Nulo = nao informado, que nao e o mesmo que nao. */
    private Boolean castrado;
}