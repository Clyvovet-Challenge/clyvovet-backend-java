package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Clinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ClinicaRepository extends RepositorioBase<Clinica> {

    @Query("SELECT c FROM Clinica c WHERE " +
            "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:cidade IS NULL OR LOWER(c.endereco.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')) ESCAPE '\\')")
    Page<Clinica> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("cidade") String cidade,
            Pageable pageable);

    default Clinica obterPorId(UUID id) {
        return obterPorId(id, Recurso.CLINICA);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.CLINICA);
    }
}
