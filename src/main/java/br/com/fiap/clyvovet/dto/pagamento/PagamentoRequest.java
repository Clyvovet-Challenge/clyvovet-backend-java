package br.com.fiap.clyvovet.dto.pagamento;

import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Mesmo padrao dos demais Request: so leitura. O @Data anterior gerava
// setters -- deixando o DTO mutavel depois da validacao -- e um toString
// com valor e forma de pagamento, que acabaria em log.
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PagamentoRequest {

    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    @Digits(integer = 9, fraction = 2, message = "Valor inválido: máximo 9 dígitos inteiros e 2 decimais")
    private BigDecimal valor;

    /**
     * Opcional (regra P1). Era @NotNull, o que obrigava um pagamento PENDENTE a
     * declarar uma data de pagamento que nao aconteceu -- o proprio seed da V2
     * grava pendentes com data nula. A data entra na confirmacao.
     */
    @PastOrPresent(message = "Data de pagamento não pode ser futura")
    private LocalDate dataPagamento;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;

    @NotNull(message = "ID do evento clínico é obrigatório")
    private UUID eventoClinicoId;

    /**
     * Um pagamento nasce PENDENTE ou PAGO. As demais transicoes sao acoes
     * proprias -- CANCELADO e REEMBOLSADO nao se declaram no cadastro.
     */
    @NotNull(message = "Status de pagamento é obrigatório")
    private StatusPagamento statusPagamento;
}