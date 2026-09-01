package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventoClinicoRepository extends RepositorioBase<EventoClinico> {

    /** Ver a nota sobre tutorId em {@link AnimalRepository}. */
    @Query("SELECT e FROM EventoClinico e WHERE " +
            "(:tipoEvento IS NULL OR e.tipoEvento = :tipoEvento) AND " +
            "(:animalNome IS NULL OR LOWER(e.animal.nome) LIKE LOWER(CONCAT('%', :animalNome, '%')) ESCAPE '\\') AND " +
            "(:tutorId IS NULL OR e.animal.tutor.id = :tutorId) AND " +
            "(:clinicaId IS NULL OR e.clinica.id = :clinicaId)")
    Page<EventoClinico> buscarPorFiltros(
            @Param("tipoEvento") TipoEvento tipoEvento,
            @Param("animalNome") String animalNome,
            @Param("tutorId") UUID tutorId,
            @Param("clinicaId") UUID clinicaId,
            Pageable pageable);

    /**
     * Eventos que ainda ocupam a agenda do veterinario naquele dia.
     *
     * CANCELADO e FALTOU ficam de fora de proposito: o horario de quem cancelou
     * volta a estar livre. Se entrassem, um unico cancelamento bloquearia o
     * slot para sempre.
     */
    @Query("""
            SELECT e FROM EventoClinico e
            WHERE e.veterinario.id = :veterinarioId
              AND e.data = :data
              AND e.statusEvento IN (br.com.fiap.clyvovet.model.StatusEvento.AGENDADO,
                                     br.com.fiap.clyvovet.model.StatusEvento.REALIZADO)
            """)
    List<EventoClinico> ocupandoAAgenda(
            @Param("veterinarioId") UUID veterinarioId,
            @Param("data") LocalDate data);

    /**
     * Retornos previstos que venceram sem o retorno acontecer — a regra R17.
     *
     * O NOT EXISTS e o coracao da consulta: nao basta a data ter passado, e
     * preciso que nao exista um RETORNO realizado apontando para este evento.
     * Sem essa parte, o pet que voltou continuaria na lista de atrasados.
     */
    @Query("""
            SELECT e FROM EventoClinico e
            WHERE e.dataRetornoPrevisto IS NOT NULL
              AND e.dataRetornoPrevisto < :hoje
              AND e.statusEvento = br.com.fiap.clyvovet.model.StatusEvento.REALIZADO
              AND (:veterinarioId IS NULL OR e.veterinario.id = :veterinarioId)
              AND (:clinicaId IS NULL OR e.clinica.id = :clinicaId)
              AND NOT EXISTS (
                    SELECT r FROM EventoClinico r
                    WHERE r.eventoOrigem = e
                      AND r.statusEvento = br.com.fiap.clyvovet.model.StatusEvento.REALIZADO)
            ORDER BY e.dataRetornoPrevisto
            """)
    List<EventoClinico> retornosVencidos(
            @Param("hoje") LocalDate hoje,
            @Param("veterinarioId") UUID veterinarioId,
            @Param("clinicaId") UUID clinicaId);

    /** Agendamentos cuja data ja passou e que ninguem concluiu — viram falta (R18). */
    @Query("""
            SELECT e FROM EventoClinico e
            WHERE e.statusEvento = br.com.fiap.clyvovet.model.StatusEvento.AGENDADO
              AND e.data < :hoje
            """)
    List<EventoClinico> agendadosVencidos(@Param("hoje") LocalDate hoje);

    /** Ha retorno em aberto ligado a este evento? Sustenta R14. */
    @Query("""
            SELECT COUNT(r) > 0 FROM EventoClinico r
            WHERE r.eventoOrigem.id = :origemId
              AND r.statusEvento = br.com.fiap.clyvovet.model.StatusEvento.AGENDADO
            """)
    boolean temRetornoEmAberto(@Param("origemId") UUID origemId);

    /** Historico do animal em ordem de data. Alimenta a serie de peso e a linha do tempo. */
    List<EventoClinico> findByAnimalIdOrderByDataAsc(UUID animalId);

    /** Agenda do tutor: o que ele marcou, do mais recente para tras. */
    @Query("""
            SELECT e FROM EventoClinico e
            WHERE e.animal.tutor.id = :tutorId
            ORDER BY e.data DESC
            """)
    Page<EventoClinico> doTutor(@Param("tutorId") UUID tutorId, Pageable pageable);

    /** Atendimentos realizados ate a data. Base da varredura de inadimplencia. */
    /**
     * Base da lista de inadimplencia, recortada pela clinica de quem pergunta.
     *
     * Nulo e "sem recorte", so para o ADMIN. Sem o filtro, a lista entrega os
     * devedores da plataforma inteira com nome e telefone do tutor -- a carteira
     * de inadimplentes da concorrente, e de quebra os ids dos atendimentos.
     */
    @Query("""
            SELECT e FROM EventoClinico e
            WHERE e.statusEvento = br.com.fiap.clyvovet.model.StatusEvento.REALIZADO
              AND e.data <= :limite
              AND (:clinicaId IS NULL OR e.clinica.id = :clinicaId)
            ORDER BY e.data
            """)
    List<EventoClinico> realizadosAte(
            @Param("limite") LocalDate limite,
            @Param("clinicaId") UUID clinicaId);

    default EventoClinico obterPorId(UUID id) {
        return obterPorId(id, Recurso.EVENTO_CLINICO);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.EVENTO_CLINICO);
    }
}
