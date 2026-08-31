package br.com.fiap.clyvovet.exception;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Recurso inexistente — mapeada para 404 pelo GlobalExceptionHandler.
 *
 * E uma excecao de dominio, e nao a EntityNotFoundException do JPA: quem
 * decide que "buscar por um id inexistente e um erro" e a regra da aplicacao,
 * nao a camada de persistencia. Manter a excecao do JPA subindo pelos services
 * amarraria o dominio a uma escolha de infraestrutura.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final transient Recurso recurso;

    public RecursoNaoEncontradoException(Recurso recurso, UUID id) {
        super(recurso.mensagemDeAusencia(id));
        this.recurso = recurso;
    }

    /**
     * Versao com mensagem propria, para quando a busca nao foi por id.
     *
     * A mensagem padrao termina em "com ID: <uuid>", o que seria enganoso ao
     * procurar por microchip: o numero informado nao e um id, e ecoa-lo como se
     * fosse mandaria quem le atras da chave errada.
     */
    public RecursoNaoEncontradoException(Recurso recurso, String mensagem) {
        super(mensagem);
        this.recurso = recurso;
    }

    /**
     * Versao para {@code Optional.orElseThrow}, que espera um fornecedor:
     * {@code findById(id).orElseThrow(naoEncontrado(Recurso.ANIMAL, id))}.
     */
    public static Supplier<RecursoNaoEncontradoException> naoEncontrado(Recurso recurso, UUID id) {
        return () -> new RecursoNaoEncontradoException(recurso, id);
    }

    public Recurso getRecurso() {
        return recurso;
    }
}
