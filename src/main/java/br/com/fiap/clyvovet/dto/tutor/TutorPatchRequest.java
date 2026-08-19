package br.com.fiap.clyvovet.dto.tutor;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import br.com.fiap.clyvovet.model.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Corpo do PATCH /tutores/{id}: so os campos que mudam.
 *
 * <h2>Por que nao reaproveitar o TutorRequest</h2>
 * As duas operacoes validam coisas diferentes. No POST e no PUT o corpo
 * descreve o recurso inteiro, entao {@code nome} e obrigatorio. No PATCH o
 * corpo descreve so o que muda, e a ausencia de {@code nome} significa "nao
 * mexa nele" -- um {@code @NotBlank} ali rejeitaria toda requisicao que nao
 * reenviasse o objeto completo, que e justamente o que o PATCH evita.
 *
 * Por isso este DTO mantem as restricoes de FORMATO ({@code @Size},
 * {@code @Email}) e abre mao das de PRESENCA ({@code @NotNull},
 * {@code @NotBlank}). Um campo enviado continua validado; um campo omitido nao
 * entra na conta.
 *
 * A alternativa seria reaproveitar o TutorRequest com grupos de validacao. Foi
 * descartada porque exigiria anotar campo a campo dos DTOs ja existentes, e um
 * grupo esquecido enfraqueceria em silencio a validacao do POST -- o erro mais
 * caro dos dois.
 *
 * <h2>Limite conhecido, valido para todos os PATCH desta API</h2>
 * Campo ausente e campo enviado como {@code null} chegam iguais aqui, entao nao
 * ha como APAGAR um campo opcional via PATCH: use PUT para isso. Distinguir os
 * dois exigiria {@code Optional} em cada atributo ou JSON Merge Patch
 * (RFC 7386), complexidade que nenhum caso de uso deste projeto pede.
 *
 * O endereco e substituido por inteiro quando enviado, e por isso continua com
 * {@code @Valid}: um endereco pela metade nao e util a ninguem.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TutorPatchRequest {

    @Size(min = 3, max = 100)
    private String nome;

    @Size(min = 11, max = 11)
    private String cpf;

    @Email
    private String email;

    @Size(min = 10, max = 11)
    private String telefone;

    private Sexo sexo;

    private LocalDate dataNascimento;

    @Valid
    private EnderecoRequest endereco;
}
