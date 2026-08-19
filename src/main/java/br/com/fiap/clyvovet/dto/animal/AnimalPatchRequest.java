package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
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
public class AnimalPatchRequest {

    @Size(min = 3, max = 100)
    private String nome;

    @Size(min = 3, max = 100)
    private String raca;

    @Size(min = 3, max = 100)
    private String especie;

    @Size(min = 3, max = 100)
    private String porte;

    @Size(min = 3, max = 100)
    private String cor;

    private SexoAnimal sexo;

    private LocalDate dataNascimento;

    // Limite igual ao da coluna, VARCHAR2(1000).
    @Size(max = 1000)
    private String observacao;

    /** Ausente significa "nao troque o dono" -- ver SegurancaService.podeAtribuirTutor. */
    private UUID tutorId;
}
