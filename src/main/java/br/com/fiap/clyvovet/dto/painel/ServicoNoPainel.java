package br.com.fiap.clyvovet.dto.painel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um servico do catalogo, com o que ele deu de trabalho e o que ele trouxe.
 *
 * <p>A receita e o que foi efetivamente PAGO, e nao {@code quantidade x preco}: o
 * preco do catalogo e editavel, e uma tabela nova reescreveria o faturamento do
 * mes passado.</p>
 */
public record ServicoNoPainel(UUID servicoId, String nome, long realizados, BigDecimal receita) {
}
