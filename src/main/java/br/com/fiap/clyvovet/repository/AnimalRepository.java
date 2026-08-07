package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.model.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    /**
     * O parametro tutorId nao e um filtro de busca exposto ao cliente: e o
     * recorte de seguranca. Os services passam o id do tutor logado quando o
     * perfil e TUTOR, e null para VETERINARIO e ADMIN, que enxergam tudo.
     * Aplicar o recorte na query, e nao depois dela, mantem a paginacao correta.
     */
    @Query("SELECT a FROM Animal a WHERE " +
            "(:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:especie IS NULL OR LOWER(a.especie) LIKE LOWER(CONCAT('%', :especie, '%'))) AND " +
            "(:tutorId IS NULL OR a.tutor.id = :tutorId)")
    Page<Animal> buscarPorFiltros(
            @Param("nome") String nome,
            @Param("especie") String especie,
            @Param("tutorId") UUID tutorId,
            Pageable pageable);
}