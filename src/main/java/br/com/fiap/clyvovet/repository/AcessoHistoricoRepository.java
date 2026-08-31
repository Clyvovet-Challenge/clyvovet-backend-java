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

    /**
     * Quantos ANIMAIS DISTINTOS este usuario alcancou no dia.
     *
     * Conta LINHAS, e nao a soma de vezes, e e essa escolha que faz o teto medir
     * a coisa certa. Cada linha e um par (usuario, animal, dia): o veterinario
     * que reabre o mesmo prontuario quarenta vezes durante uma cirurgia conta 1;
     * o que consulta duzentos animais diferentes numa tarde conta 200 — e nao
     * esta atendendo nenhum deles.
     */
    long countByUsuarioIdAndDiaAndEmergencial(UUID usuarioId, LocalDate dia, boolean emergencial);

    /** Quebras de vidro do usuario desde uma data. Sustenta o teto da regra C22. */
    long countByUsuarioIdAndEmergencialTrueAndDiaGreaterThanEqual(UUID usuarioId, LocalDate desde);

    /**
     * Quem passou do teto, para a revisao do administrador da plataforma.
     *
     * Nao ha entidade nova de alerta: o dado ja esta nesta tabela, e uma tabela
     * de avisos paralela criaria uma segunda versao da verdade para manter em
     * sincronia com esta. O que faltava era a pergunta, nao o registro.
     */
    @Query("""
            SELECT a.usuario.id, a.usuario.email, a.dia, COUNT(a)
            FROM AcessoHistorico a
            WHERE a.dia >= :desde
              AND a.emergencial = :emergencial
            GROUP BY a.usuario.id, a.usuario.email, a.dia
            HAVING COUNT(a) > :teto
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> acimaDoTeto(
            @Param("desde") LocalDate desde,
            @Param("emergencial") boolean emergencial,
            @Param("teto") long teto);
}
