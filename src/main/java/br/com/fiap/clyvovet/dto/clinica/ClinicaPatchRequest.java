package br.com.fiap.clyvovet.dto.clinica;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Corpo do PATCH: so os campos que mudam.
 *
 * Mantem as restricoes de FORMATO e abre mao das de PRESENCA. O raciocinio
 * completo -- por que nao reaproveitar o Request nem usar grupos de validacao,
 * e por que um campo nao pode ser APAGADO via PATCH -- esta em
 * {@link br.com.fiap.clyvovet.dto.tutor.TutorPatchRequest}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ClinicaPatchRequest {

    @Size(min = 3, max = 100)
    private String nome;

    @Size(min = 14, max = 14)
    private String cnpj;

    @Size(min = 10, max = 11)
    private String telefone;

    @Email
    @Size(min = 10, max = 100)
    private String email;

    /** Substituido por inteiro quando enviado: endereco pela metade nao serve. */
    @Valid
    private EnderecoRequest endereco;
}
