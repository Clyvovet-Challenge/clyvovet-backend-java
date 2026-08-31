package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoPatchRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.EventoClinicoMapper;
import br.com.fiap.clyvovet.mapper.RelacionamentosDoEvento;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.StatusEvento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoClinicoService {

    private final EventoClinicoRepository eventoClinicoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AnimalRepository animalRepository;
    private final ClinicaRepository clinicaRepository;
    private final ServicoRepository servicoRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final SegurancaService seguranca;

    /** Ver a nota sobre a chave de cache em {@link AnimalService#listarTodos}. */
    @Cacheable(value = "eventos",
            // A clinica entra na chave junto com o tutor. Sem ela, a pagina montada
            // para um veterinario seria servida ao de outra clinica na mesma
            // consulta -- o recorte existiria na query e vazaria pelo cache.
            key = "#tipoEvento + '-' + #animalNome + '-' + @seguranca.tutorIdParaFiltro()"
                    + " + '-' + @seguranca.clinicaParaFiltro() + '-' + #pageable")
    public Page<EventoClinicoResponse> listarTodos(TipoEvento tipoEvento, String animalNome, Pageable pageable) {
        return eventoClinicoRepository.buscarPorFiltros(tipoEvento, animalNome,
                        seguranca.tutorIdParaFiltro(), seguranca.clinicaParaFiltro(), pageable)
                .map(eventoClinicoMapper::toResponse);
    }

    public EventoClinicoResponse buscarPorId(UUID id) {
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = {"eventos", "pagamentos"}, allEntries = true)
    public EventoClinicoResponse criar(EventoClinicoRequest request) {
        garantirQueEDaPropriaClinica(request.getClinicaId());
        garantirQueRetornoVemPelaRotaDeRetorno(request.getTipoEvento());   // R9

        EventoClinico evento = eventoClinicoMapper.toEntity(request, resolverRelacionamentos(request));
        evento.setStatusEvento(statusPelaData(evento.getData()));          // R1

        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = {"eventos", "pagamentos"}, allEntries = true)
    public EventoClinicoResponse atualizar(UUID id, EventoClinicoRequest request) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);
        eventoClinicoMapper.atualizar(evento, request, resolverRelacionamentos(request));
        garantirQueORetornoTemOrigem(evento);                              // R9
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = {"eventos", "pagamentos"}, allEntries = true)
    public EventoClinicoResponse atualizarParcialmente(UUID id, EventoClinicoPatchRequest patch) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);
        eventoClinicoMapper.aplicarPatch(evento, patch, resolverRelacionamentos(patch));
        garantirQueORetornoTemOrigem(evento);                              // R9
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = {"eventos", "pagamentos"}, allEntries = true)
    public void deletar(UUID id) {
        eventoClinicoRepository.garantirQueExiste(id);
        // R19: a FK de pagamento ja barrava a remocao, mas com erro de
        // integridade -- que fala do banco, e nao da regra, e barra igual
        // quando o pagamento e apenas PENDENTE.
        if (pagamentoRepository.existsByEventoClinicoIdAndStatusPagamento(id, StatusPagamento.PAGO)) {
            throw new RegraDeNegocioException("id",
                    "Este atendimento tem pagamento confirmado. Estorne o pagamento antes de removê-lo");
        }
        eventoClinicoRepository.deleteById(id);
    }

    /**
     * R1 — a data decide o estado inicial.
     *
     * Data futura e marcacao; data de hoje ou passada e registro do que ja
     * aconteceu. Nascer sempre AGENDADO fazia o atendimento lancado
     * retroativamente entrar na varredura de faltas de R18 e ser marcado
     * FALTOU no dia seguinte — o pet foi atendido e o sistema registrava que
     * ele nao apareceu.
     */
    private StatusEvento statusPelaData(LocalDate data) {
        return data != null && data.isAfter(LocalDate.now())
                ? StatusEvento.AGENDADO
                : StatusEvento.REALIZADO;
    }

    /**
     * R9 — RETORNO exige o evento de origem, e este corpo nao tem onde
     * declara-lo.
     *
     * Sem a recusa, POST /eventos-clinicos criava retorno orfao: contava como
     * retorno nas metricas sem estar ligado a consulta nenhuma, e a consulta
     * que deveria te-lo gerado seguia como vencida em R17. A rota que faz isso
     * direito e POST /eventos-clinicos/{origemId}/retorno.
     */
    private void garantirQueRetornoVemPelaRotaDeRetorno(TipoEvento tipoEvento) {
        if (tipoEvento == TipoEvento.RETORNO) {
            throw new RegraDeNegocioException("tipoEvento",
                    "Um retorno precisa apontar para a consulta que o gerou. "
                            + "Use POST /eventos-clinicos/{id}/retorno");
        }
    }

    /**
     * A mesma regra pela porta da edicao: um PUT ou PATCH que apenas troque o
     * tipo para RETORNO deixaria orfao um evento que nasceu CONSULTA. O retorno
     * criado pela rota propria passa, porque la a origem e gravada.
     */
    private void garantirQueORetornoTemOrigem(EventoClinico evento) {
        if (evento.getTipoEvento() == TipoEvento.RETORNO && evento.getEventoOrigem() == null) {
            throw new RegraDeNegocioException("tipoEvento",
                    "Um retorno precisa apontar para a consulta que o gerou. "
                            + "Use POST /eventos-clinicos/{id}/retorno");
        }
    }

    /**
     * Criar e atualizar precisam das mesmas tres entidades, com a mesma regra de
     * "existe ou 404". Estava escrito duas vezes, doze linhas cada.
     */
    /**
     * O veterinario registra atendimento na clinica onde trabalha.
     *
     * Sem isso ele podia criar um evento numa clinica qualquer e, desde a
     * inversao do acesso (B1), ficar sem conseguir le-lo em seguida — criava e
     * perdia. E o espelho da regra A3 do agendamento, que ja exigia o
     * veterinario pertencer a clinica do servico.
     *
     * O ADMIN da plataforma passa: e ele quem corrige registro de qualquer
     * clinica.
     */
    private void garantirQueEDaPropriaClinica(UUID clinicaId) {
        UUID minhaClinica = seguranca.clinicaParaFiltro();
        if (minhaClinica != null && !minhaClinica.equals(clinicaId)) {
            throw new RegraDeNegocioException("clinicaId",
                    "O atendimento precisa ser registrado na clínica onde o veterinário atende");
        }
    }

    private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoRequest request) {
        return new RelacionamentosDoEvento(
                veterinarioRepository.obterPorId(request.getVeterinarioId()),
                animalRepository.obterPorId(request.getAnimalId()),
                clinicaRepository.obterPorId(request.getClinicaId()),
                request.getServicoId() == null ? null : servicoRepository.obterPorId(request.getServicoId()));
    }

    /**
     * Mesma resolucao, mas para PATCH: cada id ausente vira null, e o mapper
     * entende null como "mantenha o vinculo atual". Buscar mesmo assim custaria
     * tres consultas a toa e transformaria um id omitido num 404.
     */
    private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoPatchRequest patch) {
        return new RelacionamentosDoEvento(
                patch.getVeterinarioId() == null ? null : veterinarioRepository.obterPorId(patch.getVeterinarioId()),
                patch.getAnimalId() == null ? null : animalRepository.obterPorId(patch.getAnimalId()),
                patch.getClinicaId() == null ? null : clinicaRepository.obterPorId(patch.getClinicaId()),
                patch.getServicoId() == null ? null : servicoRepository.obterPorId(patch.getServicoId()));
    }
}
