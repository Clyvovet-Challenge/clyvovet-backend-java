package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Veterinario;

/**
 * As tres entidades que um evento clinico referencia, resolvidas em conjunto.
 *
 * Existe para encurtar a assinatura dos mapeamentos, que chegavam a cinco
 * parametros — quatro deles do mesmo "assunto" e faceis de trocar de ordem na
 * chamada, ja que o compilador nao acusa nada quando os tipos batem.
 */
public record RelacionamentosDoEvento(Veterinario veterinario, Animal animal, Clinica clinica) {
}
