package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnimalRepository extends RepositorioBase<Animal> {

    /**
     * O parametro tutorId nao e um filtro de busca exposto ao cliente: e o
     * recorte de seguranca. Os services passam o id do tutor logado quando o
     * perfil e TUTOR, e null para VETERINARIO e ADMIN, que enxergam tudo.
     * Aplicar o recorte na query, e nao depois dela, mantem a paginacao correta.
     *
     * NAO REMOVER O {@code ESCAPE '\}{@code '}: sem ele o Hibernate emite
     * {@code LIKE ... ESCAPE ''}, e sob a semantica do Oracle (que o H2 imita
     * com MODE=Oracle) string vazia E nulo. O predicado vira
     * {@code ESCAPE NULL}, avalia como desconhecido e NUNCA casa — todos os
     * filtros por texto da API devolviam lista vazia por causa disso.
     */
    @Query("SELECT a FROM Animal a WHERE " +
            "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ESCAPE '\\') AND " +
            "(:especie IS NULL OR LOWER(a.especie) LIKE LOWER(CONCAT('%', :especie, '%')) ESCAPE '\\') AND " +
            "(:tutorId IS NULL OR a.tutor.id = :tutorId)")
    Page<Animal> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("especie") String especie,
            @Param("tutorId") UUID tutorId,
            Pageable pageable);

    default Animal obterPorId(UUID id) {
        return obterPorId(id, Recurso.ANIMAL);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.ANIMAL);
    }
}
