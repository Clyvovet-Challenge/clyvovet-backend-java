package br.com.fiap.clyvovet.repository.projecao;

import br.com.fiap.clyvovet.model.StatusPagamento;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dinheiro do periodo, quebrado por servico e por situacao do pagamento.
 *
 * <p>Uma consulta so, e nao duas, porque as duas perguntas que o painel faz saem
 * das MESMAS linhas: o caixa do periodo e a soma de tudo por status, e a receita por
 * servico e o recorte PAGO dessas linhas. Separadas, elas poderiam divergir — e a
 * primeira vez que divergissem seria na tela, com o total nao batendo com a soma
 * das partes.</p>
 *
 * <p>O {@code servicoId} vem nulo no atendimento sem servico do catalogo. Ele
 * continua entrando no caixa: o dinheiro entrou, e a V2 tem pagamento assim. E por
 * isso que a consulta usa LEFT JOIN — com o join implicito, esses pagamentos
 * sumiriam do faturamento sem deixar rastro.</p>
 */
public record ValorPorServicoEStatus(
        UUID servicoId,
        StatusPagamento status,
        BigDecimal total,
        Long quantidade) {
}
