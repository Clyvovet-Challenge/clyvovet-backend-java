package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.model.Veterinario;

/**
 * As entidades que um evento clinico referencia, resolvidas em conjunto.
 *
 * Existe para encurtar a assinatura dos mapeamentos, que chegavam a cinco
 * parametros do mesmo "assunto" e faceis de trocar de ordem na chamada, ja que
 * o compilador nao acusa nada quando os tipos batem.
 *
 * O servico e opcional: eventos anteriores ao catalogo nao tem, e o registro
 * direto pelo veterinario tambem pode nao ter. Sem ele, o atendimento nao tem
 * preco e fica fora da inadimplencia.
 */
public record RelacionamentosDoEvento(
        Veterinario veterinario, Animal animal, Clinica clinica, Servico servico) {
}
