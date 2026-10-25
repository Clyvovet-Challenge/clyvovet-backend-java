package br.com.fiap.clyvovet.dto.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O tutor marcando um atendimento.
 *
 * Sem campo de status: ele e sempre AGENDADO. Aceita-lo do corpo permitiria um
 * atendimento que nasce REALIZADO sem nunca ter acontecido.
 *
 * O @Setter e do formulario das telas: o binding do Spring MVC preenche o
 * objeto campo a campo. Uma classe *Form so para isso repetiria cada campo e
 * cada validacao.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AgendamentoRequest {

    @NotNull
    private UUID animalId;

    @NotNull
    private UUID servicoId;

    @NotNull
    private UUID veterinarioId;

    /**
     * A antecedencia minima e verificada no service, com a hora junto. O
     * formulario manda a data como texto (yyyy-MM-dd); o JSON ja vinha assim.
     */
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
