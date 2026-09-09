package br.com.fiap.clyvovet.dto.painel;

import java.math.BigDecimal;

/**
 * Uma barra do painel: o rotulo, quantos e quanto isso representa.
 *
 * <p>O percentual vem calculado da API, e nao deixado para a tela. Sao varios
 * clientes lendo o mesmo painel, e a base do percentual e uma decisao — nesta
 * lista, o total de atendimentos realizados. Deixada para cada tela, cada uma
 * escolheria a sua, e duas telas do mesmo produto mostrariam numeros diferentes
 * para o mesmo dia.</p>
 */
public record ContagemNoPainel(String rotulo, long quantidade, BigDecimal percentual) {
}
