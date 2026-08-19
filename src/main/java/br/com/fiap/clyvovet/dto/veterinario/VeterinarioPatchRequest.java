package br.com.fiap.clyvovet.dto.veterinario;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import br.com.fiap.clyvovet.model.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

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
public class VeterinarioPatchRequest {

    @Size(min = 11, max = 11)
    private String cpf;

    @Size(min = 3, max = 100)
    private String nome;

    private LocalDate dataNascimento;

    private Sexo sexo;

    @Email
    @Size(min = 10, max = 100)
    private String email;

    @Size(min = 10, max = 11)
    private String telefone;

    @Valid
    private EnderecoRequest endereco;

    @Size(min = 3, max = 100)
    private String especialidade;

    /** Limite acompanha a coluna, VARCHAR2(30). */
    @Size(min = 4, max = 30)
    private String crmv;

    private UUID clinicaId;
}
