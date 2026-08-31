package br.com.fiap.clyvovet.dto.agendamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * O motivo e obrigatorio (regra A14).
 *
 * Cancelamento sem motivo registrado torna impossivel distinguir, depois, a
 * clinica que remarcou por emergencia daquela que simplesmente nao apareceu —
 * e e essa distincao que sustenta qualquer politica de no-show justa.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CancelamentoRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String motivo;
}
