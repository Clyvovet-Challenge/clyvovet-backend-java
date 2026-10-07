package br.com.fiap.clyvovet.dto.agendamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * O motivo e obrigatorio: sem ele nao da para distinguir depois a clinica que
 * remarcou por emergencia da que simplesmente nao apareceu.
 *
 * O @Setter e do formulario das telas, como no {@link AgendamentoRequest}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CancelamentoRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String motivo;
}
