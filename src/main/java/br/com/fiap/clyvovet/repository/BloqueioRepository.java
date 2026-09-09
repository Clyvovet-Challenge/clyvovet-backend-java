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

    /**
     * Os bloqueios de um profissional que ainda valem para alguma coisa.
     *
     * <p>Existe porque a API sabia CRIAR e APAGAR bloqueio, e nao sabia LISTAR: quem
     * marcasse as ferias fechava a tela e nunca mais via aquilo. Nem para conferir,
     * nem para desfazer — apagar exige o id, e o id so aparecia na resposta do POST,
     * uma unica vez.</p>
     *
     * <p>O corte e por {@code dataFim}, e nao por {@code dataInicio}: ferias que
     * comecaram semana passada e terminam amanha continuam bloqueando a agenda, e
     * some-las da tela seria esconder a causa dos horarios que nao aparecem.</p>
     */
    @Query("""
            SELECT b FROM Bloqueio b
            WHERE b.veterinario.id = :veterinarioId
              AND b.dataFim >= :desde
            ORDER BY b.dataInicio
            """)
    List<Bloqueio> vigentesDe(
            @Param("veterinarioId") UUID veterinarioId,
            @Param("desde") LocalDate desde);

    default Bloqueio obterPorId(UUID id) {
        return obterPorId(id, Recurso.BLOQUEIO);
    }
}
