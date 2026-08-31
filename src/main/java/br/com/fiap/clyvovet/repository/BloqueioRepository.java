package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Bloqueio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BloqueioRepository extends RepositorioBase<Bloqueio> {

    /** Bloqueios que alcancam a data — o intervalo cobre, nao precisa comecar nela. */
    @Query("""
            SELECT b FROM Bloqueio b
            WHERE b.veterinario.id = :veterinarioId
              AND b.dataInicio <= :data
              AND b.dataFim >= :data
            """)
    List<Bloqueio> queAlcancam(
            @Param("veterinarioId") UUID veterinarioId,
            @Param("data") LocalDate data);

    default Bloqueio obterPorId(UUID id) {
        return obterPorId(id, Recurso.BLOQUEIO);
    }
}
