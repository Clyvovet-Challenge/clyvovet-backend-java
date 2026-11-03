package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
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

    /**
     * {@code SELECT ... FOR UPDATE}: a linha fica presa ate o fim da transacao.
     *
     * Serve as decisoes do tipo "le, confere e grava" -- confirmar um pagamento,
     * marcar um horario. Sem a trava, dois pedidos simultaneos leem o mesmo
     * estado, os dois passam na conferencia e os dois gravam. Com ela, o segundo
     * espera o primeiro terminar e confere contra o que ele gravou.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<T> findComTravaById(UUID id);

    default T travarPorId(UUID id, Recurso recurso) {
        return findComTravaById(id).orElseThrow(RecursoNaoEncontradoException.naoEncontrado(recurso, id));
    }

    default void garantirQueExiste(UUID id, Recurso recurso) {
        if (!existsById(id)) {
            throw new RecursoNaoEncontradoException(recurso, id);
        }
    }
}
