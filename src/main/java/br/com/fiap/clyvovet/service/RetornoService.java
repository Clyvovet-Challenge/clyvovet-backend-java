package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.eventoClinico.ConclusaoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.RetornoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.RetornoVencidoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.EventoClinicoMapper;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * O fluxo de retorno e falta — regras R1 a R21 da spec 08.
 *
 * E o segundo fluxo nao-CRUD, e o que responde ao tema do Challenge de forma
 * mais direta: o modelo episodico registra que uma consulta aconteceu; a
 * continuidade do cuidado exige saber que ela DEVIA ter tido sequencia e nao
 * teve. E o que a lista de retornos vencidos entrega.
 *
 * O schema para isto existe desde a migration V5 — status_evento,
 * data_retorno_previsto, evento_origem_id e peso_kg. As colunas ficaram tres
 * meses no banco sem nenhuma linha de Java que as enxergasse.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetornoService {

    /** Acima disto a variacao de peso vira alerta clinico (regra R7). */
    private static final BigDecimal VARIACAO_DE_PESO_QUE_ALERTA = new BigDecimal("0.20");

    /** Janela em que um REALIZADO ainda pode ser corrigido para FALTOU (regra R20). */
    private static final int HORAS_PARA_CORRIGIR_PRESENCA = 24;

    private final EventoClinicoRepository eventoClinicoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final AgendaService agendaService;

    /**
     * O veterinario fecha o atendimento: registra peso, desfecho e quando o pet
     * deve voltar.
     *
     * E a transicao AGENDADO -> REALIZADO, e o unico caminho para ela. Deixar
     * o status editavel por PATCH permitiria marcar como realizado um
     * atendimento futuro — que e o mesmo que registrar consulta que nao houve.
     */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse concluir(UUID id, ConclusaoRequest request) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);

        garantirQuePodeConcluir(evento);                                   // R2, R4, R5
        garantirRetornoCoerente(evento, request.getDataRetornoPrevisto()); // R15

        if (request.getPesoKg() != null) {
            evento.setPesoKg(request.getPesoKg());                         // R6
        }
        evento.setDesfecho(request.getDesfecho());
        evento.setDataRetornoPrevisto(request.getDataRetornoPrevisto());
        evento.setStatusEvento(StatusEvento.REALIZADO);
        if (request.getDescricao() != null) {
            evento.setDescricao(request.getDescricao());
        }

        EventoClinico salvo = eventoClinicoRepository.save(evento);
        avisarSobreVariacaoDePeso(salvo);                                  // R7
        return eventoClinicoMapper.toResponse(salvo);
    }

    /**
     * Agenda o retorno ligado a consulta de origem (R9 a R14).
     *
     * O evento_origem_id e o que transforma RETORNO de rotulo solto em relacao
     * verificavel: sem ele nao da para dizer se o pet voltou, so que houve um
     * atendimento chamado retorno em algum momento.
     */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse agendarRetorno(UUID origemId, RetornoRequest request) {
        EventoClinico origem = eventoClinicoRepository.obterPorId(origemId);

        if (origem.getStatusEvento() != StatusEvento.REALIZADO) {          // R11
            throw new RegraDeNegocioException("eventoOrigemId",
                    "Só é possível marcar retorno de um atendimento que aconteceu");
        }
        if (!request.getData().isAfter(origem.getData())) {                // R12
            throw new RegraDeNegocioException("data",
                    "O retorno precisa ser posterior à consulta de origem");
        }
        if (eventoClinicoRepository.temRetornoEmAberto(origemId)) {        // R14
            throw new RegraDeNegocioException("eventoOrigemId",
                    "Já existe um retorno em aberto para este atendimento");
        }

        Veterinario veterinario = request.getVeterinarioId() != null
                ? veterinarioRepository.obterPorId(request.getVeterinarioId())
                : origem.getVeterinario();

        garantirHorarioLivre(veterinario, origem, request);                // R8

        EventoClinico retorno = new EventoClinico();
        retorno.setAnimal(origem.getAnimal());                             // R10, por construcao
        retorno.setClinica(origem.getClinica());
        retorno.setServico(origem.getServico());
        retorno.setVeterinario(veterinario);
        retorno.setTipoEvento(TipoEvento.RETORNO);
        retorno.setData(request.getData());
        retorno.setHora(request.getHora());
        retorno.setStatusEvento(StatusEvento.AGENDADO);
        retorno.setEventoOrigem(origem);
        retorno.setDescricao("Retorno de " + origem.getData());

        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(retorno));
    }

    /**
     * Os pets que deviam ter voltado e nao voltaram (R17).
     *
     * E o RESULTADO do fluxo — a lista sobre a qual a clinica age. Sem ela, o
     * data_retorno_previsto seria um campo que ninguem le.
     */
    public List<RetornoVencidoResponse> vencidos(UUID veterinarioId, UUID clinicaId) {
        LocalDate hoje = LocalDate.now();
        return eventoClinicoRepository.retornosVencidos(hoje, veterinarioId, clinicaId).stream()
                .map(evento -> new RetornoVencidoResponse(
                        evento.getId(),
                        evento.getAnimal().getId(),
                        evento.getAnimal().getNome(),
                        evento.getAnimal().getTutor() != null ? evento.getAnimal().getTutor().getNome() : null,
                        evento.getAnimal().getTutor() != null ? evento.getAnimal().getTutor().getTelefone() : null,
                        evento.getData(),
                        evento.getDataRetornoPrevisto(),
                        (int) java.time.temporal.ChronoUnit.DAYS.between(evento.getDataRetornoPrevisto(), hoje),
                        evento.getVeterinario() != null ? evento.getVeterinario().getNome() : null))
                .sorted(Comparator.comparing(RetornoVencidoResponse::diasEmAtraso).reversed())
                .toList();
    }

    /**
     * Marca como FALTOU todo agendamento cuja data passou sem conclusao (R18).
     *
     * Sem esta varredura a taxa de falta nunca sai de zero: ninguem volta ao
     * sistema para registrar que o pet nao apareceu. O AGENDADO vencido fica
     * eternamente agendado, e a metrica mente por omissao.
     *
     * E um endpoint, e nao um @Scheduled, de proposito: agendador em aplicacao
     * com mais de uma instancia dispara em todas ao mesmo tempo. Enquanto nao
     * houver coordenacao, quem chama e a clinica — e a chamada fica no log.
     */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public int marcarFaltas() {
        List<EventoClinico> vencidos = eventoClinicoRepository.agendadosVencidos(LocalDate.now());
        vencidos.forEach(evento -> evento.setStatusEvento(StatusEvento.FALTOU));
        eventoClinicoRepository.saveAll(vencidos);

        log.info("Varredura de faltas: {} agendamentos vencidos marcados como FALTOU", vencidos.size());
        return vencidos.size();
    }

    // ------------------------------------------------------------------

    private void garantirQuePodeConcluir(EventoClinico evento) {
        if (evento.getStatusEvento() == StatusEvento.CANCELADO) {          // R4
            throw new RegraDeNegocioException("statusEvento",
                    "Um atendimento cancelado não pode ser concluído");
        }
        if (evento.getStatusEvento() == StatusEvento.REALIZADO) {          // R5
            throw new RegraDeNegocioException("statusEvento",
                    "Este atendimento já foi concluído");
        }
        if (evento.getData().isAfter(LocalDate.now())) {                   // R2
            throw new RegraDeNegocioException("data",
                    "Não é possível concluir um atendimento marcado para o futuro");
        }
    }

    private void garantirRetornoCoerente(EventoClinico evento, LocalDate dataRetorno) {
        if (dataRetorno != null && !dataRetorno.isAfter(evento.getData())) {
            throw new RegraDeNegocioException("dataRetornoPrevisto",
                    "O retorno previsto precisa ser posterior à data do atendimento");
        }
    }

    private void garantirHorarioLivre(Veterinario veterinario, EventoClinico origem, RetornoRequest request) {
        int duracao = origem.getServico() != null ? origem.getServico().getDuracaoMinutos() : 30;
        LocalTime inicio = LocalTime.parse(request.getHora());
        var janela = new AgendaService.Janela(inicio, inicio.plusMinutes(duracao));

        String impedimento = agendaService.porQueNaoEstaLivre(veterinario.getId(), request.getData(), janela);
        if (impedimento != null) {
            throw new RegraDeNegocioException("hora", impedimento);
        }
    }

    /**
     * R7 — variacao de peso acima de 20% em relacao a aferição anterior.
     *
     * AVISA, NAO BLOQUEIA, e a distincao e deliberada. Um filhote de 2 kg que
     * chega com 3 kg variou 50% e esta perfeitamente saudavel; um gato adulto
     * que perde 25% em dois meses pode estar com doenca renal. A regra nao tem
     * como distinguir os dois casos — quem tem e o veterinario. Bloquear o
     * registro do peso real por causa de um limiar estatistico seria impedir a
     * anotacao justamente do dado que importa.
     */
    private void avisarSobreVariacaoDePeso(EventoClinico evento) {
        if (evento.getPesoKg() == null || evento.getAnimal() == null) {
            return;
        }
        eventoClinicoRepository.findByAnimalIdOrderByDataAsc(evento.getAnimal().getId()).stream()
                .filter(anterior -> anterior.getPesoKg() != null)
                .filter(anterior -> !anterior.getId().equals(evento.getId()))
                .filter(anterior -> !anterior.getData().isAfter(evento.getData()))
                .reduce((primeiro, ultimo) -> ultimo)
                .ifPresent(anterior -> {
                    BigDecimal variacao = evento.getPesoKg()
                            .subtract(anterior.getPesoKg())
                            .abs()
                            .divide(anterior.getPesoKg(), 4, RoundingMode.HALF_UP);
                    if (variacao.compareTo(VARIACAO_DE_PESO_QUE_ALERTA) > 0) {
                        log.info("Animal {}: peso variou {}% desde {} ({} kg -> {} kg)",
                                evento.getAnimal().getId(),
                                variacao.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                                anterior.getData(), anterior.getPesoKg(), evento.getPesoKg());
                    }
                });
    }
}
