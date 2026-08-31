package br.com.fiap.clyvovet.dto.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O tutor marcando um atendimento.
 *
 * Sem campo de status: ele e sempre AGENDADO. Aceita-lo do corpo permitiria um
 * atendimento que nasce REALIZADO sem nunca ter acontecido.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AgendamentoRequest {

    @NotNull
    private UUID animalId;

    @NotNull
    private UUID servicoId;

    @NotNull
    private UUID veterinarioId;

    /** A antecedencia minima e verificada no service, com a hora junto. */
    @NotNull
    @Future(message = "Não é possível agendar para uma data passada")
    private LocalDate data;

    @NotNull
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String hora;

    /**
     * Consentimento de acesso ao historico (fluxo C).
     *
     * Boolean e nao boolean: o campo ausente chega null e conta como recusa.
     * Consentimento pre-marcado nao e consentimento.
     */
    private Boolean consentimentoHistorico;

    public boolean consentiu() {
        return Boolean.TRUE.equals(consentimentoHistorico);
    }
}
