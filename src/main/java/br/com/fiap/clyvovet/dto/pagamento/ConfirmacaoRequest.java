package br.com.fiap.clyvovet.dto.pagamento;

import br.com.fiap.clyvovet.model.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** A data entra aqui, e não no cadastro: pendente não tem data de pagamento. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ConfirmacaoRequest {

    @NotNull
    @PastOrPresent(message = "Data de pagamento não pode ser futura")
    private LocalDate dataPagamento;

    /** Opcional: sem ele, mantém a forma registrada na criação. */
    private FormaPagamento formaPagamento;
}
