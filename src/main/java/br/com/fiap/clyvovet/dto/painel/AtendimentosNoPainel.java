package br.com.fiap.clyvovet.dto.painel;

import java.math.BigDecimal;

/**
 * O movimento da clinica no periodo: o que foi marcado, o que aconteceu e o que
 * nao aconteceu.
 *
 * @param marcados             ainda AGENDADO. Numa janela passada isto nao e agenda
 *                             futura — e atendimento que ninguem concluiu nem marcou
 *                             como falta, ou seja, prontuario em aberto.
 * @param realizados           concluidos pelo veterinario.
 * @param faltas               o tutor nao apareceu.
 * @param cancelados           desmarcados antes de acontecer.
 * @param taxaDeComparecimento realizados sobre (realizados + faltas), em pontos
 *                             percentuais. O cancelado fica FORA da base: quem
 *                             desmarcou com antecedencia devolveu o horario, e
 *                             contar isso como falha do tutor mistura duas coisas
 *                             que a clinica trata de formas diferentes.
 * @param taxaDeFalta          o complemento da anterior, sobre a mesma base.
 */
public record AtendimentosNoPainel(
        long marcados,
        long realizados,
        long faltas,
        long cancelados,
        long total,
        BigDecimal taxaDeComparecimento,
        BigDecimal taxaDeFalta) {
}
