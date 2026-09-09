package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.repository.projecao.ValorPorServicoEStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends RepositorioBase<Pagamento> {

    /** Ver a nota sobre tutorId em {@link AnimalRepository}. */
    @Query("SELECT p FROM Pagamento p WHERE " +
            "(:statusPagamento IS NULL OR p.statusPagamento = :statusPagamento) AND " +
            "(:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento) AND " +
            "(:tutorId IS NULL OR p.eventoClinico.animal.tutor.id = :tutorId) AND " +
            "(:clinicaId IS NULL OR p.eventoClinico.clinica.id = :clinicaId)")
    Page<Pagamento> buscarPorFiltros(
            @Param("statusPagamento") StatusPagamento statusPagamento,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tutorId") UUID tutorId,
            @Param("clinicaId") UUID clinicaId,
            Pageable pageable);

    /** Soma dos pagamentos de um evento num status. Null se nao houver nenhum. */
    @Query("""
            SELECT SUM(p.valor) FROM Pagamento p
            WHERE p.eventoClinico.id = :eventoId AND p.statusPagamento = :status
            """)
    BigDecimal totalPorStatus(
            @Param("eventoId") UUID eventoId,
            @Param("status") StatusPagamento status);

    /**
     * O extrato do tutor, recortado pela clinica de quem pergunta.
     *
     * O clinicaId nulo significa "sem recorte" -- e o contrato dos demais
     * filtros opcionais daqui, e serve ao TUTOR (que ve o proprio extrato
     * inteiro) e ao ADMIN. Para o VETERINARIO ele vem preenchido: sem isso o
     * extrato entrega o historico financeiro do tutor em clinicas concorrentes,
     * inclusive os ids dos pagamentos, que sao a chave de confirmar e estornar.
     */
    @Query("""
            SELECT p FROM Pagamento p
            WHERE p.eventoClinico.animal.tutor.id = :tutorId
              AND p.eventoClinico.data BETWEEN :de AND :ate
              AND (:clinicaId IS NULL OR p.eventoClinico.clinica.id = :clinicaId)
            ORDER BY p.eventoClinico.data DESC
            """)
    List<Pagamento> doTutorNoPeriodo(
            @Param("tutorId") UUID tutorId,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate,
            @Param("clinicaId") UUID clinicaId);

    /**
     * O caixa do periodo de uma clinica, quebrado por servico e por status.
     *
     * <p>O recorte e {@code e.data} — a data do atendimento —, e nao
     * {@code p.dataPagamento}. Sao dois regimes contabeis diferentes, e a escolha
     * aqui e deliberada: o painel poe o faturamento ao lado do numero de
     * atendimentos, entao o dinheiro mostrado precisa ser o DAQUELES atendimentos.
     * Pela data de pagamento, "10 realizados, R$ 4.000" poderia estar somando a
     * cirurgia do mes passado que so foi quitada agora.</p>
     *
     * <p>Uma consequencia honesta disso: o que foi atendido na janela e ainda nao
     * foi pago aparece em {@code pendente}, e nao some. E exatamente o que o
     * administrador precisa ver.</p>
     */
    @Query("""
            SELECT new br.com.fiap.clyvovet.repository.projecao.ValorPorServicoEStatus(
                       s.id, p.statusPagamento, SUM(p.valor), COUNT(p))
            FROM Pagamento p
            JOIN p.eventoClinico e
            LEFT JOIN e.servico s
            WHERE e.clinica.id = :clinicaId
              AND e.data BETWEEN :de AND :ate
            GROUP BY s.id, p.statusPagamento
            """)
    List<ValorPorServicoEStatus> totaisPorServicoEStatus(
            @Param("clinicaId") UUID clinicaId,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate);

    default Pagamento obterPorId(UUID id) {
        return obterPorId(id, Recurso.PAGAMENTO);
    }

    default void garantirQueExiste(UUID id) {
        garantirQueExiste(id, Recurso.PAGAMENTO);
    }
}
