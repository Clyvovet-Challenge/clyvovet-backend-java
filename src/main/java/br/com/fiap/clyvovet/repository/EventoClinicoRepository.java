package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventoClinicoRepository extends JpaRepository<EventoClinico, UUID> {

    /** Ver a nota sobre tutorId em {@link AnimalRepository}. */
    @Query("SELECT e FROM EventoClinico e WHERE " +
            "(:tipoEvento IS NULL OR e.tipoEvento = :tipoEvento) AND " +
            "(:animalNome IS NULL OR LOWER(e.animal.nome) LIKE LOWER(CONCAT('%', :animalNome, '%'))) AND " +
            "(:tutorId IS NULL OR e.animal.tutor.id = :tutorId)")
    Page<EventoClinico> buscarPorFiltros(
            @Param("tipoEvento") TipoEvento tipoEvento,
            @Param("animalNome") String animalNome,
            @Param("tutorId") UUID tutorId,
            Pageable pageable);
}