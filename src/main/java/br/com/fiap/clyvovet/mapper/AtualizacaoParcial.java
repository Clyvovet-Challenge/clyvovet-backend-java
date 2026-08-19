package br.com.fiap.clyvovet.mapper;

import java.util.function.Consumer;

/**
 * Apoio dos mappers na aplicacao de um PATCH sobre a entidade.
 *
 * Existe para que cada mapper escreva a intencao numa linha por campo:
 *
 * <pre>{@code
 * aplicarSePresente(patch.getNome(), tutor::setNome);
 * aplicarSePresente(patch.getEmail(), tutor::setEmail);
 * }</pre>
 *
 * em vez de um {@code if (x != null)} repetido dezenas de vezes -- que e onde
 * costuma passar despercebido o campo que ninguem lembrou de copiar.
 */
final class AtualizacaoParcial {

    private AtualizacaoParcial() {
    }

    /** Repassa o valor ao destino apenas quando ele veio na requisicao. */
    static <T> void aplicarSePresente(T valor, Consumer<T> destino) {
        if (valor != null) {
            destino.accept(valor);
        }
    }
}
