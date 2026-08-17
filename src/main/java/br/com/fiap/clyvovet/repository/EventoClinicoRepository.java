package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventoClinicoRepository extends RepositorioBase<EventoClinico> {

    /** Ver a nota sobre tutorId em {@link AnimalRepository}. */
    @Query("SELECT e FROM EventoClinico e WHERE " +
            "(:tipoEvento IS NULL OR e.tipoEvento = :tipoEvento) AND " +
            "(:animalNome IS NULL OR LOWER(e.animal.nome) LIKE LOWER(CONCAT('%', :animalNome, '%')) ESCAPE '\\') AND " +
            "(:tutorId IS NULL OR e.animal.tutor.id = :tutorId)")
    Page<EventoClinico> buscarPorFiltros(
            @Param("tipoEvento") TipoEvento tipoEvento,
            @Param("animalNome") String animalNome,
            @Param("tutorId") UUID tutorId,
            Pageable pageable);

    default EventoClinico obterPorId(UUID id) {
        return obterPorId(id, Recurso.EVENTO_CLINICO);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.EVENTO_CLINICO);
    }
}
