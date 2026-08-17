package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

/**
 * Base dos repositorios da aplicacao: acrescenta ao JpaRepository as duas
 * buscas que sempre vinham acompanhadas de um "senao, 404".
 *
 * Antes, cada service repetia
 * {@code findById(id).orElseThrow(() -> new EntityNotFoundException("... " + id))}
 * — o mesmo trecho em cerca de vinte lugares, cada um livre para escrever a
 * mensagem do seu jeito. Com a decisao aqui, o service volta a expressar so a
 * intencao: "obter este animal".
 *
 * Os metodos sao default de proposito. O Spring Data deriva consultas a partir
 * do NOME de metodos abstratos; um metodo com corpo ele simplesmente respeita,
 * o que permite estender o repositorio sem infraestrutura extra.
 */
@NoRepositoryBean
public interface RepositorioBase<T> extends JpaRepository<T, UUID> {

    default T obterPorId(UUID id, Recurso recurso) {
        return findById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
    }

    default void garantirQueExiste(UUID id, Recurso recurso) {
        if (!existsById(id)) {
            throw new RecursoNaoEncontradoException(recurso, id);
        }
    }
}
