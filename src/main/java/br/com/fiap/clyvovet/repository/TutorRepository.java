package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TutorRepository extends RepositorioBase<Tutor> {

    // Busca por nome (parcial) e/ou cidade — parametros opcionais
    @Query("SELECT t FROM Tutor t WHERE " +
            "(:nome IS NULL OR LOWER(t.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:cidade IS NULL OR LOWER(t.endereco.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')) ESCAPE '\\')")
    Page<Tutor> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("cidade") String cidade,
            Pageable pageable);

    default Tutor obterPorId(UUID id) {
        return obterPorId(id, Recurso.TUTOR);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.TUTOR);
    }
}
