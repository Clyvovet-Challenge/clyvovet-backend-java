package br.com.fiap.clyvovet.mapper;

import java.util.function.Function;

/**
 * Leitura de campo de uma entidade associada que pode ser nula.
 *
 * As respostas expoem o id e o nome do relacionamento (o tutor do animal, a
 * clinica do veterinario), e o relacionamento e opcional no banco. Sem este
 * apoio, cada campo desses vira uma linha de ternario repetida no mapper —
 * era o caso de seis linhas so no EventoClinicoMapper.
 */
final class Referencias {

    private Referencias() {
    }

    static <O, V> V de(O origem, Function<O, V> extrator) {
        return origem == null ? null : extrator.apply(origem);
    }
}
