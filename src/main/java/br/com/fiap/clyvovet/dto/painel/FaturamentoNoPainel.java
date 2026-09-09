package br.com.fiap.clyvovet.dto.painel;

import java.math.BigDecimal;

/**
 * O dinheiro dos atendimentos do periodo.
 *
 * <p>Os quatro valores sao as quatro situacoes de {@code StatusPagamento}, e estao
 * todos aqui de proposito: um painel que mostrasse so o recebido esconderia
 * exatamente a parte sobre a qual da para agir.</p>
 *
 * @param recebido    PAGO. E o caixa.
 * @param aReceber    PENDENTE — atendimento feito e ainda nao quitado.
 * @param estornado   REEMBOLSADO. Ja entrou e voltou; nao esta em {@code recebido}.
 * @param cancelado   CANCELADO. Cobranca que nao chegou a valer.
 * @param ticketMedio recebido dividido pelos atendimentos realizados. Zero quando
 *                    nao houve atendimento — e nao um erro de divisao.
 */
public record FaturamentoNoPainel(
        BigDecimal recebido,
        BigDecimal aReceber,
        BigDecimal estornado,
        BigDecimal cancelado,
        BigDecimal ticketMedio,
        long pagamentos) {
}
