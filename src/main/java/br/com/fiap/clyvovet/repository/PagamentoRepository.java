package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PagamentoRepository extends RepositorioBase<Pagamento> {

    /** Ver a nota sobre tutorId em {@link AnimalRepository}. */
    @Query("SELECT p FROM Pagamento p WHERE " +
            "(:statusPagamento IS NULL OR p.statusPagamento = :statusPagamento) AND " +
            "(:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento) AND " +
            "(:tutorId IS NULL OR p.eventoClinico.animal.tutor.id = :tutorId)")
    Page<Pagamento> buscarPorFiltros(
            @Param("statusPagamento") StatusPagamento statusPagamento,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tutorId") UUID tutorId,
            Pageable pageable);

    default Pagamento obterPorId(UUID id) {
        return obterPorId(id, Recurso.PAGAMENTO);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.PAGAMENTO);
    }
}
