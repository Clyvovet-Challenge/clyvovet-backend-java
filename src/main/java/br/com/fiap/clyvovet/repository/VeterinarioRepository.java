package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Veterinario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VeterinarioRepository extends RepositorioBase<Veterinario> {

    @Query("SELECT v FROM Veterinario v WHERE " +
            "(:nome IS NULL OR LOWER(v.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:especialidade IS NULL OR LOWER(v.especialidade) LIKE LOWER(CONCAT('%', :especialidade, '%')) ESCAPE '\\')")
    Page<Veterinario> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("especialidade") String especialidade,
            Pageable pageable);

    default Veterinario obterPorId(UUID id) {
        return obterPorId(id, Recurso.VETERINARIO);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.VETERINARIO);
    }
}
