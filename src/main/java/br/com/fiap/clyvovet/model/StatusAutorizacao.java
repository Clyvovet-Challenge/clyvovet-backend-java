package br.com.fiap.clyvovet.model;

/**
 * Estado de uma autorizacao de acesso ao historico.
 *
 * Nao existe PENDENTE. O consentimento e concedido no proprio ato do
 * agendamento — nao ha pedido do veterinario nem fila de aprovacao, entao nao
 * ha estado intermediario a representar. A autorizacao nasce VIGENTE ou nao
 * nasce.
 */
public enum StatusAutorizacao {
    VIGENTE,
    /** O tutor retirou o acesso. Nao apaga o que ja foi escrito. */
    REVOGADA,
    /** Passaram-se 2 anos sem atendimento na clinica. */
    EXPIRADA
}
