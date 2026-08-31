package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.pagamento.PagamentoPatchRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.PagamentoMapper;
import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final SegurancaService seguranca;

    /** Ver a nota sobre a chave de cache em {@link AnimalService#listarTodos}. */
    @Cacheable(value = "pagamentos",
            // Ver a nota sobre a clinica na chave em EventoClinicoService.
            key = "#statusPagamento + '-' + #formaPagamento + '-' + @seguranca.tutorIdParaFiltro()"
                    + " + '-' + @seguranca.clinicaParaFiltro() + '-' + #pageable")
    public Page<PagamentoResponse> listarTodos(StatusPagamento statusPagamento, FormaPagamento formaPagamento, Pageable pageable) {
        return pagamentoRepository.buscarPorFiltros(statusPagamento, formaPagamento,
                        seguranca.tutorIdParaFiltro(), seguranca.clinicaParaFiltro(), pageable)
                .map(pagamentoMapper::toResponse);
    }

    public PagamentoResponse buscarPorId(UUID id) {
        return pagamentoMapper.toResponse(pagamentoRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse criar(PagamentoRequest request) {
        Pagamento pagamento = pagamentoMapper.toEntity(
                request, eventoClinicoRepository.obterPorId(request.getEventoClinicoId()));
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse atualizar(UUID id, PagamentoRequest request) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);
        pagamentoMapper.atualizar(pagamento, request,
                eventoClinicoRepository.obterPorId(request.getEventoClinicoId()));
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse atualizarParcialmente(UUID id, PagamentoPatchRequest patch) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);
        EventoClinico evento = patch.getEventoClinicoId() == null
                ? null
                : eventoClinicoRepository.obterPorId(patch.getEventoClinicoId());
        pagamentoMapper.aplicarPatch(pagamento, patch, evento);
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    /**
     * P9 — pagamento confirmado nao se apaga, se estorna.
     *
     * DELETE apagaria a receita do historico junto com a linha, e com ela a
     * resposta para "quanto essa clinica faturou". O estorno deixa as duas
     * pontas registradas: entrou e voltou, com o motivo.
     */
    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public void deletar(UUID id) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);
        if (pagamento.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new RegraDeNegocioException("statusPagamento",
                    "Um pagamento confirmado não é removido. Use POST /pagamentos/" + id + "/estornar");
        }
        pagamentoRepository.deleteById(id);
    }
}
