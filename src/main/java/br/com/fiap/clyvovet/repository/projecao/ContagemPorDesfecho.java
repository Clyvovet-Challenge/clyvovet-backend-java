package br.com.fiap.clyvovet.repository.projecao;

import br.com.fiap.clyvovet.model.Desfecho;

/**
 * Quantos atendimentos terminaram em cada desfecho.
 *
 * <p>O desfecho pode vir <b>nulo</b>, e nao e o mesmo que {@code INDEFINIDO}: nulo e
 * o atendimento concluido sem ninguem registrar o resultado, INDEFINIDO e o
 * veterinario dizendo que nao soube classificar. O nulo continua na conta porque
 * escondê-lo faria a soma dos desfechos nao bater com o total de realizados — e
 * quem olha o painel repararia na diferenca sem ter onde procurar a explicacao.</p>
 */
public record ContagemPorDesfecho(Desfecho desfecho, Long quantidade) {
}
