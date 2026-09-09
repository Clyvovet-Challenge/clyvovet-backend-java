/**
 * O que as consultas de agregacao devolvem, antes de virar painel.
 *
 * <p>Sao linhas de {@code GROUP BY}, e nao entidades: a contagem por status, o
 * total por desfecho, a receita por servico. Ficam separadas dos DTOs de resposta
 * de proposito — a forma do agrupamento e da consulta, a forma do painel e da tela,
 * e ja aconteceu neste projeto de as duas mudarem por motivos diferentes.</p>
 *
 * <p>O contrario — trazer os eventos do periodo e contar em Java — foi considerado
 * e recusado: o periodo e escolhido por quem chama, e nada impede um pedido de dez
 * anos. Agregar no banco mantem a memoria constante seja qual for a janela.</p>
 */
package br.com.fiap.clyvovet.repository.projecao;
