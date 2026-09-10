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
    /**
     * O nome da raca. Continua obrigatorio e continua sendo o que o app ja
     * mandava -- este campo nao mudou de contrato.
     *
     * Quando `racaId` vem junto, o service SOBRESCREVE este texto com o nome do
     * catalogo. Entao um par incoerente (id de Poodle, texto "Golden") nao
     * corrompe nada: o catalogo ganha.
     */
    @NotBlank
    @Size(min = 3, max = 100)
    private String raca;

    /**
     * A raca escolhida no catalogo. OPCIONAL, e de proposito.
     *
     * Este campo e puramente aditivo: quem nao manda continua funcionando
     * exatamente como antes, com a raca em texto livre. Quem manda ganha tres
     * coisas de brinde -- a especie e o porte vem do catalogo (e param de
     * divergir), e o animal passa a ter a chave estavel que escolhe a arte em
     * pixel e casa predisposicao na API .NET sem adivinhar.
     */
    private UUID racaId;
    @NotBlank
    @Size(min = 3, max = 100)
    private String especie;
    /**
     * PEQUENO, MEDIO ou GRANDE -- as tres unicas que o CHECK do banco aceita.
     *
     * SEM O PADRAO ISSO ERA UM 500. O `chk_animal_porte` existe desde a V1 nos
     * dois dialetos, mas nada validava antes de chegar la: porte invalido
     * atravessava a aplicacao inteira e voltava como erro de integridade, que o
     * cliente le como "a API quebrou" em vez de "voce mandou um valor errado".
     *
     * A comparacao e insensivel a caixa de proposito, e isso conserta uma
     * divergencia silenciosa: o formulario do app manda "Pequeno", e a colacao
     * do MySQL (utf8mb4_0900_ai_ci) aceita contra 'PEQUENO' sem reclamar -- mas a
     * do Oracle NAO. O mesmo cadastro que funciona hoje seria recusado la. O
     * mapper normaliza para maiuscula antes de gravar, entao os dois passam.
     */
    @Pattern(regexp = "(?i)PEQUENO|MEDIO|GRANDE",
             message = "porte deve ser PEQUENO, MEDIO ou GRANDE")
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