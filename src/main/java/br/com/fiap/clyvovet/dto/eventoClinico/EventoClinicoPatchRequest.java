package br.com.fiap.clyvovet.dto.eventoClinico;

import br.com.fiap.clyvovet.model.TipoEvento;
import jakarta.validation.constraints.Pattern;
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
public class EventoClinicoPatchRequest {

    private LocalDate data;

    /** A coluna e VARCHAR2(5): so cabe HH:mm. */
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String hora;

    @Size(max = 1000)
    private String descricao;

    private UUID veterinarioId;

    private UUID animalId;

    private UUID clinicaId;

    private TipoEvento tipoEvento;
}
