package br.com.fiap.clyvovet.repository.projecao;

/** Contagem por um texto do proprio dado — a raca do animal, hoje. Pode ser nulo. */
public record ContagemPorRotulo(String rotulo, Long quantidade) {
}
