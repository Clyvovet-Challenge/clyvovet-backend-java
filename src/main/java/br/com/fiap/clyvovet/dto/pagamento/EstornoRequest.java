package br.com.fiap.clyvovet.dto.pagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Devolver dinheiro sem registrar por quê deixa a conta sem explicação. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class EstornoRequest {

    @NotBlank
    @Size(min = 10, max = 500, message = "Descreva o motivo em pelo menos 10 caracteres")
    private String motivo;
}
