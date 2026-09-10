package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Clinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ClinicaRepository extends RepositorioBase<Clinica> {

    /**
     * Busca de clinica, com dois modos que convivem.
     *
     * <p>{@code nome} e {@code cidade} sao os filtros de sempre, e continuam
     * separados: a colecao do Postman e os testes de filtro dependem deles.</p>
     *
     * <p>{@code busca} e o modo do APP, e cruza quatro campos com OR -- nome,
     * bairro, cidade e estado. O tutor nao pensa em campos: ele digita
     * "VetCare", "Pinheiros" ou "Sao Paulo" na mesma caixa e espera que o
     * sistema descubra qual dos tres ele quis dizer. Tres caixas separadas
     * transferem para ele um problema que a consulta resolve com um OR.</p>
     *
     * <p>O ESCAPE de cada LIKE continua obrigatorio -- ver a nota no
     * AnimalRepository: sem ele o predicado avalia como desconhecido sob a
     * semantica do Oracle e a busca NUNCA casa.</p>
     */
    @Query("SELECT c FROM Clinica c WHERE " +
            "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:cidade IS NULL OR LOWER(c.endereco.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')) ESCAPE '\\') AND " +
            "(:busca IS NULL " +
            " OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '\\' " +
            " OR LOWER(c.endereco.bairro) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '\\' " +
            " OR LOWER(c.endereco.cidade) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '\\' " +
            " OR LOWER(c.endereco.estado) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '\\')")
    Page<Clinica> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("cidade") String cidade,
            @Param("busca") String busca,
            Pageable pageable);

    default Clinica obterPorId(UUID id) {
        return obterPorId(id, Recurso.CLINICA);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.CLINICA);
    }
}
