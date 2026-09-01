package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.pagamento.ConfirmacaoRequest;
import br.com.fiap.clyvovet.dto.pagamento.EstornoRequest;
import br.com.fiap.clyvovet.dto.pagamento.ExtratoResponse;
import br.com.fiap.clyvovet.dto.pagamento.InadimplenciaResponse;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.dto.pagamento.SaldoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.PagamentoMapper;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.StatusEvento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Cobrança — regras P1 a P14 da spec 08.
 *
 * Transições: PENDENTE -> PAGO | CANCELADO; PAGO -> REEMBOLSADO. CANCELADO e
 * REEMBOLSADO são terminais, e nada volta para PENDENTE.
 *
 * O valor do atendimento vem de {@code Servico.preco}, gravado no evento pelo
 * agendamento. Sem ele não haveria contra o que comparar o que foi recebido.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CobrancaService {

    private final PagamentoRepository pagamentoRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final TutorRepository tutorRepository;
    private final PagamentoMapper pagamentoMapper;
    private final SegurancaService seguranca;

    /**
     * PENDENTE -> PAGO. Único caminho para essa transição.
     *
     * A data entra aqui, e não no cadastro: um pagamento pendente não tem data
     * de pagamento, porque ele não foi pago.
     */
    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse confirmar(UUID id, ConfirmacaoRequest request) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);

        if (pagamento.getStatusPagamento() != StatusPagamento.PENDENTE) {
            throw new RegraDeNegocioException("statusPagamento",
                    "Só um pagamento pendente pode ser confirmado");
        }
        EventoClinico evento = pagamento.getEventoClinico();
        garantirQueOEventoAceitaCobranca(evento);                       // P4
        garantirDataCoerente(request.getDataPagamento(), evento);       // P2
        garantirQueNaoExcedeOValorDoServico(pagamento, evento);         // P7

        pagamento.setStatusPagamento(StatusPagamento.PAGO);
        pagamento.setDataPagamento(request.getDataPagamento());
        if (request.getFormaPagamento() != null) {
            pagamento.setFormaPagamento(request.getFormaPagamento());
        }
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    /**
     * PAGO -> REEMBOLSADO.
     *
     * É o único jeito de desfazer um pagamento. DELETE apagaria a receita do
     * histórico, e com ela a resposta para "quanto essa clínica faturou".
     */
    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse estornar(UUID id, EstornoRequest request) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);

        if (pagamento.getStatusPagamento() != StatusPagamento.PAGO) {   // P11
            throw new RegraDeNegocioException("statusPagamento",
                    "Só um pagamento confirmado pode ser estornado");
        }
        pagamento.setStatusPagamento(StatusPagamento.REEMBOLSADO);
        pagamento.setObservacao(request.getMotivo());                   // P10
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    /** Quanto o atendimento custou, quanto entrou e quanto falta. */
    public SaldoResponse saldo(UUID eventoId) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(eventoId);
        BigDecimal cobrado = valorDoAtendimento(evento);
        BigDecimal pago = totalPago(eventoId);

        return new SaldoResponse(
                eventoId,
                evento.getServico() != null ? evento.getServico().getNome() : evento.getDescricao(),
                cobrado,
                pago,
                cobrado.subtract(pago).max(BigDecimal.ZERO),
                pago.compareTo(cobrado) >= 0);
    }

    /**
     * Atendimentos realizados com saldo em aberto há mais de N dias (P13).
     *
     * Não inclui evento sem serviço vinculado: sem preço não há dívida a
     * afirmar, e listar como inadimplente quem talvez nada deva é pior que
     * não listar.
     */
    public List<InadimplenciaResponse> inadimplencia(int diasMinimos) {
        LocalDate limite = LocalDate.now().minusDays(Math.max(diasMinimos, 0));

        return eventoClinicoRepository.realizadosAte(limite, seguranca.clinicaParaFiltro()).stream()
                .filter(evento -> evento.getServico() != null)
                .map(evento -> {
                    BigDecimal cobrado = evento.getServico().getPreco();
                    BigDecimal pago = totalPago(evento.getId());
                    return new InadimplenciaResponse(
                            evento.getId(),
                            evento.getData(),
                            evento.getAnimal() != null ? evento.getAnimal().getNome() : null,
                            tutorDe(evento),
                            telefoneDe(evento),
                            cobrado,
                            pago,
                            cobrado.subtract(pago),
                            (int) java.time.temporal.ChronoUnit.DAYS.between(evento.getData(), LocalDate.now()));
                })
                .filter(linha -> linha.emAberto().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(InadimplenciaResponse::diasEmAberto).reversed())
                .toList();
    }

    /** O que o tutor deve e o que já pagou, num período. */
    public ExtratoResponse extrato(UUID tutorId, LocalDate de, LocalDate ate) {
        tutorRepository.garantirQueExiste(tutorId);
        LocalDate inicio = de != null ? de : LocalDate.now().minusYears(1);
        LocalDate fim = ate != null ? ate : LocalDate.now();

        List<Pagamento> pagamentos = pagamentoRepository.doTutorNoPeriodo(
                tutorId, inicio, fim, seguranca.clinicaParaFiltro());

        BigDecimal pago = somar(pagamentos, StatusPagamento.PAGO);
        BigDecimal pendente = somar(pagamentos, StatusPagamento.PENDENTE);
        BigDecimal estornado = somar(pagamentos, StatusPagamento.REEMBOLSADO);

        return new ExtratoResponse(tutorId, inicio, fim, pago, pendente, estornado,
                pagamentos.stream().map(pagamentoMapper::toResponse).toList());
    }

    // ------------------------------------------------------------------

    /**
     * P4 e P6. Evento cancelado não gera cobrança; agendado aceita
     * pré-pagamento, que é o que dá sentido à antecedência mínima e reduz
     * no-show.
     */
    private void garantirQueOEventoAceitaCobranca(EventoClinico evento) {
        if (evento == null) {
            return;
        }
        if (evento.getStatusEvento() == StatusEvento.CANCELADO) {
            throw new RegraDeNegocioException("eventoClinicoId",
                    "Um atendimento cancelado não gera cobrança");
        }
    }

    /** P2 e P3: não se paga antes de o atendimento existir, nem no futuro. */
    private void garantirDataCoerente(LocalDate dataPagamento, EventoClinico evento) {
        if (dataPagamento.isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("dataPagamento",
                    "A data de pagamento não pode ser futura");
        }
        // Pré-pagamento de atendimento agendado é legítimo: a data do evento
        // ainda está à frente, e comparar as duas recusaria o caso válido.
        if (evento != null && evento.getStatusEvento() != StatusEvento.AGENDADO
                && dataPagamento.isBefore(evento.getData())) {
            throw new RegraDeNegocioException("dataPagamento",
                    "A data de pagamento não pode ser anterior à do atendimento");
        }
    }

    /**
     * P7 e P8. Pagamento parcial é permitido — parcelamento é comum em
     * cirurgia —, mas a soma dos confirmados não passa do preço do serviço.
     */
    private void garantirQueNaoExcedeOValorDoServico(Pagamento pagamento, EventoClinico evento) {
        if (evento == null || evento.getServico() == null) {
            return;
        }
        BigDecimal cobrado = evento.getServico().getPreco();
        BigDecimal jaPago = totalPago(evento.getId());
        BigDecimal comEste = jaPago.add(pagamento.getValor());

        if (comEste.compareTo(cobrado) > 0) {
            throw new RegraDeNegocioException("valor",
                    "A soma dos pagamentos (%s) excede o valor do atendimento (%s)"
                            .formatted(comEste, cobrado));
        }
    }

    private BigDecimal valorDoAtendimento(EventoClinico evento) {
        return evento.getServico() != null ? evento.getServico().getPreco() : BigDecimal.ZERO;
    }

    private BigDecimal totalPago(UUID eventoId) {
        BigDecimal total = pagamentoRepository.totalPorStatus(eventoId, StatusPagamento.PAGO);
        return total != null ? total : BigDecimal.ZERO;
    }

    private BigDecimal somar(List<Pagamento> pagamentos, StatusPagamento status) {
        return pagamentos.stream()
                .filter(p -> p.getStatusPagamento() == status)
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String tutorDe(EventoClinico evento) {
        return evento.getAnimal() != null && evento.getAnimal().getTutor() != null
                ? evento.getAnimal().getTutor().getNome() : null;
    }

    private String telefoneDe(EventoClinico evento) {
        return evento.getAnimal() != null && evento.getAnimal().getTutor() != null
                ? evento.getAnimal().getTutor().getTelefone() : null;
    }
}
