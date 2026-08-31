package br.com.fiap.clyvovet.model;

/**
 * Ciclo de vida de um atendimento.
 *
 * A coluna status_evento existe no banco desde a V5, mas ficou sem enum
 * correspondente ate agora — o que significa que ela era inalcancavel pela
 * API: nenhuma requisicao conseguia le-la nem grava-la.
 *
 * AGENDADO e o estado em que o evento nasce quando o TUTOR marca a consulta;
 * REALIZADO, quando o veterinario registra um atendimento que ja aconteceu.
 * Ver as regras R1 a R5 da spec 08.
 */
public enum StatusEvento {
    AGENDADO,
    REALIZADO,
    FALTOU,
    CANCELADO
}
