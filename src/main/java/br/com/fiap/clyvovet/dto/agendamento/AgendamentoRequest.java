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
 * Nao existe campo de status nem de clinica: o status e sempre AGENDADO (regra
 * A11) e a clinica sai do servico escolhido. Aceitar qualquer um dos dois do
 * corpo abriria a porta para um agendamento que nasce REALIZADO — ou seja,
 * para um atendimento que consta como feito sem nunca ter acontecido.
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

    /**
     * @Future e nao @FutureOrPresent: a antecedencia minima da regra A10 e
     * verificada no service, com a hora junto. Aqui a data apenas nao pode ser
     * de ontem.
     */
    @NotNull
    @Future(message = "Não é possível agendar para uma data passada")
    private LocalDate data;

    @NotNull
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String hora;

    /**
     * Consentimento de acesso ao historico clinico (regra C9 e C10 da spec 08).
     *
     * Boolean e nao boolean, e sem default: o campo ausente chega null e e
     * tratado como recusa. Um primitivo assumiria false silenciosamente, o que
     * daria no mesmo por acaso — mas aqui a ausencia de default e o registro de
     * que a escolha e deliberada. Consentimento pre-marcado nao e
     * consentimento, e isso e regra da API, nao escolha do frontend.
     */
    private Boolean consentimentoHistorico;

    public boolean consentiu() {
        return Boolean.TRUE.equals(consentimentoHistorico);
    }
}
