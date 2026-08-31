package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.model.AcessoHistorico;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcessoHistoricoRepository extends RepositorioBase<AcessoHistorico> {

    /** A linha do dia, se ja houver — e o que faz o registro incrementar em vez de duplicar. */
    Optional<AcessoHistorico> findByAnimalIdAndUsuarioIdAndDiaAndEmergencial(
            UUID animalId, UUID usuarioId, LocalDate dia, boolean emergencial);

    @Query("""
            SELECT a FROM AcessoHistorico a
            WHERE a.animal.id = :animalId
            ORDER BY a.dia DESC
            """)
    List<AcessoHistorico> doAnimal(@Param("animalId") UUID animalId);
}
