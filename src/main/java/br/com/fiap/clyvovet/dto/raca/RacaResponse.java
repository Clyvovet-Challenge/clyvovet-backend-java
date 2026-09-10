package br.com.fiap.clyvovet.dto.raca;

import br.com.fiap.clyvovet.model.EspecieAnimal;

import java.util.UUID;

/**
 * Uma raca do catalogo, como o app a recebe.
 *
 * `chave` e o campo que o cliente usa para escolher a arte do animal
 * ("golden-retriever" -> golden-retriever.png). `nome` e o que ele mostra.
 * `porteTipico` pre-preenche o formulario de cadastro.
 *
 * `especieRotulo` vem junto de `especie` de proposito: o app precisa gravar o
 * texto que ja usava ("Cachorro") em `Animal.especie`, e derivar isso no
 * cliente a partir do enum seria espalhar a regra por dois lugares.
 */
public record RacaResponse(
        UUID id,
        EspecieAnimal especie,
        String especieRotulo,
        String nome,
        String chave,
        String porteTipico
) {}
