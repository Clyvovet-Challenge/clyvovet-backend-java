package br.com.fiap.clyvovet.repository.projecao;

import java.util.UUID;

/**
 * Quantos atendimentos foram realizados por servico do catalogo.
 *
 * <p>So aparece atendimento COM servico vinculado. O que nao tem fica de fora
 * porque nao ha o que somar nem a que nome atribuir — e a diferenca entre esta
 * soma e o total de realizados e justamente o quanto a clinica atende sem
 * precificar.</p>
 */
public record ContagemPorServico(UUID servicoId, String nome, Long quantidade) {
}
