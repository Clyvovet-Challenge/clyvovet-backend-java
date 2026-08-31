package br.com.fiap.clyvovet.dto.agendamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * O motivo e obrigatorio: sem ele nao da para distinguir depois a clinica que
 * remarcou por emergencia da que simplesmente nao apareceu.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CancelamentoRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String motivo;
}
