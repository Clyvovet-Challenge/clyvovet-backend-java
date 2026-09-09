package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoPatchRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.mapper.PagamentoMapper;
import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import br.com.fiap.clyvovet.security.RecorteDeAcesso;
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
    /**
     * Ver a nota sobre a chave de cache em {@link AnimalService#listarTodos}.
     *
     * <p>O filtro por evento existe para a tela de cobranca de UM atendimento: sem
     * ele, saber quais pagamentos pertencem aquele evento exigiria varrer a listagem
     * inteira, pagina a pagina, e filtrar no cliente — e nenhuma pagina traz garantia
     * de conter todos.</p>
     *
     * <p>Ele entra na CHAVE junto com os demais. Chave mais estreita que o filtro
     * serve a lista de um atendimento a quem pediu a de outro.</p>
     */
    @Cacheable(value = "pagamentos",
            key = "#statusPagamento + '-' + #formaPagamento + '-' + #eventoClinicoId"
                    + " + '-' + @seguranca.recorte().chaveDeCache() + '-' + #pageable")
    public Page<PagamentoResponse> listarTodos(StatusPagamento statusPagamento,
                                               FormaPagamento formaPagamento,
                                               UUID eventoClinicoId, Pageable pageable) {
        RecorteDeAcesso recorte = seguranca.recorte();
        return pagamentoRepository.buscarPorFiltros(statusPagamento, formaPagamento,
                        recorte.tutorId(), recorte.clinicaId(), eventoClinicoId, pageable)
                .map(pagamentoMapper::toResponse);
    }

    public PagamentoResponse buscarPorId(UUID id) {
        return pagamentoMapper.toResponse(pagamentoRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse criar(PagamentoRequest request) {
        garantirQueNasceEmEstadoValido(request.getStatusPagamento());
        Pagamento pagamento = pagamentoMapper.toEntity(
                request, eventoClinicoRepository.obterPorId(request.getEventoClinicoId()));
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public PagamentoResponse atualizar(UUID id, PagamentoRequest request) {
        Pagamento pagamento = pagamentoRepository.obterPorId(id);
        garantirQueNaoPulaAAcaoPropria(pagamento.getStatusPagamento(), request.getStatusPagamento());
        pagamentoMapper.atualizar(pagamento, request,
                eventoClinicoRepository.obterPorId(request.getEventoClinicoId()));
        return pagamentoMapper.toResponse(pagamentoRepository.save(pagamento));
    }

    /**
     * Um pagamento nasce PENDENTE ou PAGO.
     *
     * <p>É o que o {@code PagamentoRequest} já dizia em javadoc — "CANCELADO e
     * REEMBOLSADO não se declaram no cadastro" — e o que o código não fazia.
     * Verificado contra a pilha no ar: {@code POST /pagamentos} com
     * {@code statusPagamento: "REEMBOLSADO"} respondia <b>201</b>.</p>
     *
     * <p>O buraco não é de forma, é de contabilidade. {@link CobrancaService#estornar}
     * guarda a regra P11 — só um pagamento PAGO pode ser estornado — e devolve 409 a
     * quem tenta estornar um pendente. Nascer REEMBOLSADO produz exatamente o que a
     * P11 existe para impedir: um estorno de dinheiro que nunca entrou. O mesmo vale
     * para CANCELADO e a ação de cancelamento.</p>
     *
     * <p>O PATCH já estava fechado, e por outro caminho: {@code PagamentoPatchRequest}
     * simplesmente não tem o campo. Só o POST e o PUT ficaram abertos.</p>
     */
    private void garantirQueNasceEmEstadoValido(StatusPagamento status) {
        if (status == StatusPagamento.CANCELADO || status == StatusPagamento.REEMBOLSADO) {
            throw new RegraDeNegocioException("statusPagamento",
                    "Um pagamento nasce PENDENTE ou PAGO. Para chegar a " + status
                            + ", use a ação própria (/confirmar, /estornar)");
        }
    }

    /**
     * O PUT edita o pagamento, e não o faz mudar de estado.
     *
     * <p>A regra é sobre MUDANÇA, e não sobre o valor em si: {@code statusPagamento}
     * é obrigatório no corpo do PUT, então corrigir a descrição de um pagamento já
     * REEMBOLSADO exige reenviar REEMBOLSADO. Recusar o valor igual ao atual deixaria
     * todo pagamento estornado impossível de editar — trocaria um buraco por uma
     * parede.</p>
     */
    private void garantirQueNaoPulaAAcaoPropria(StatusPagamento atual, StatusPagamento novo) {
        if (novo != atual) {
            garantirQueNasceEmEstadoValido(novo);
        }
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

    @Transactional
    @CacheEvict(value = "pagamentos", allEntries = true)
    public void deletar(UUID id) {
        pagamentoRepository.garantirQueExiste(id);
        pagamentoRepository.deleteById(id);
    }
}
