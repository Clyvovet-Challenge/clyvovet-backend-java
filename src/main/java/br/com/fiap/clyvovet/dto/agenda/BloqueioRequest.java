package br.com.fiap.clyvovet.dto.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Ferias, folga ou almoco.
 *
 * As duas horas nulas significam dias inteiros; as duas preenchidas, uma faixa
 * de cada dia do intervalo. Meia hora preenchida nao significa nada — e o
 * service recusa antes de o check do banco precisar recusar.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BloqueioRequest {

    @NotNull
    private UUID veterinarioId;

    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String horaInicio;

    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String horaFim;

    @NotBlank
    @Size(min = 3, max = 200)
    private String motivo;
}
