package br.com.fiap.clyvovet.dto.pagamento;

import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Record, como os demais Response da API. Era a unica resposta mutavel: o
 * mapper a montava com oito setters, e nada impedia que fosse alterada depois
 * de pronta.
 */
public record PagamentoResponse(
        UUID id,
        FormaPagamento formaPagamento,
        BigDecimal valor,
        LocalDate dataPagamento,
        String descricao,
        String observacao,
        UUID eventoClinicoId,
        StatusPagamento statusPagamento
) {
}
