package br.com.fiap.clyvovet.model;

/**
 * Resultado clinico de um atendimento.
 *
 * Sem ele nao existe a pergunta "onde a clinica tem mais sucesso e mais
 * fracasso" — que e o que a visao de produto pede do painel. INDEFINIDO nao e
 * o mesmo que nulo: nulo significa "atendimento ainda nao concluido", e
 * INDEFINIDO significa "concluido, e o veterinario nao soube classificar".
 */
public enum Desfecho {
    MELHORA,
    ESTAVEL,
    PIORA,
    OBITO,
    INDEFINIDO
}
