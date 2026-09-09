package br.com.fiap.clyvovet.repository.projecao;

import br.com.fiap.clyvovet.model.StatusEvento;

/** Quantos atendimentos em cada estado do ciclo de vida, num periodo. */
public record ContagemPorStatus(StatusEvento status, Long quantidade) {
}
