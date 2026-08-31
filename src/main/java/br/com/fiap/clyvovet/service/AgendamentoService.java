package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.agendamento.AgendamentoRequest;
import br.com.fiap.clyvovet.dto.agendamento.VagaResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.mapper.EventoClinicoMapper;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.*;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agendamento pelo tutor — regras A1 a A15 da spec 08.
 *
 * Nao decide se o horario esta livre (é do {@link AgendaService}, porque a
 * listagem de vagas precisa da mesma resposta) nem quem pode agendar para qual
 * animal (é do SegurancaService).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendamentoService {

    /** Antecedencia minima para marcar (regra A10). */
    private static final int HORAS_MINIMAS_PARA_AGENDAR = 2;

    /** Prazo em que o tutor cancela sem marca de cancelamento tardio (regra A12). */
    private static final int HORAS_PARA_CANCELAR_SEM_MARCA = 24;

    private final EventoClinicoRepository eventoClinicoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AnimalRepository animalRepository;
    private final ServicoRepository servicoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final AgendaService agendaService;
    private final SegurancaService seguranca;
    private final AutorizacaoService autorizacaoService;

    /** A ordem das validacoes segue o custo: as mais baratas primeiro. */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse agendar(AgendamentoRequest request) {
        Animal animal = animalRepository.obterPorId(request.getAnimalId());
        Servico servico = servicoRepository.obterPorId(request.getServicoId());
        Veterinario veterinario = veterinarioRepository.obterPorId(request.getVeterinarioId());

        garantirServicoAtivo(servico);                                    // A2
        garantirVeterinarioDaClinica(veterinario, servico);               // A3
        garantirAntecedenciaMinima(request.getData(), request.getHora()); // A9, A10
        garantirHorarioLivre(veterinario, servico, request);              // A5, A6, A7, A8

        EventoClinico evento = new EventoClinico();
        evento.setAnimal(animal);
        evento.setServico(servico);
        evento.setVeterinario(veterinario);
        evento.setClinica(servico.getClinica());
        evento.setTipoEvento(servico.getTipoEvento());
        evento.setData(request.getData());
        evento.setHora(request.getHora());
        evento.setStatusEvento(StatusEvento.AGENDADO);                    // A11
        evento.setDescricao(servico.getNome());

        garantirQueOConsentimentoEDeQuemPodeDar(animal, request);         // C10

        EventoClinico salvo = eventoClinicoRepository.save(evento);

        // O consentimento e o proprio agendamento (spec 08, C8-C11). Recusar e
        // permitido e nao impede nada: o atendimento acontece nos niveis 0 e 1.
        if (request.consentiu()) {
            autorizacaoService.conceder(animal, servico.getClinica(), salvo);
        }

        return eventoClinicoMapper.toResponse(salvo);
    }

    /**
     * Nao apaga a linha: sem ela a taxa de cancelamento nao seria calculavel e
     * o horario liberado nao teria rastro.
     */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse cancelar(UUID id, String motivo) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);

        if (evento.getStatusEvento() != StatusEvento.AGENDADO) {
            throw new RegraDeNegocioException("statusEvento",
                    "Só é possível cancelar um atendimento que ainda está agendado");
        }
        garantirQueNaoHaDinheiroPreso(evento);                             // R19, P12

        // Cancelar em cima da hora e permitido, mas fica anotado: recusar
        // empurraria o tutor a simplesmente nao aparecer, o que e pior.
        String marca = cancelamentoEmCimaDaHora(evento) ? "[TARDIO] " : "";
        evento.setStatusEvento(StatusEvento.CANCELADO);
        evento.setMotivoCancelamento(marca + motivo);

        EventoClinico salvo = eventoClinicoRepository.save(evento);
        autorizacaoService.revogarSeNuncaHouveAtendimento(salvo);          // C14

        return eventoClinicoMapper.toResponse(salvo);
    }

    /** Devolve o que PODE ser marcado: dia sem vaga nao aparece. */
    public List<VagaResponse> vagas(UUID servicoId, UUID veterinarioId, LocalDate de, LocalDate ate) {
        Servico servico = servicoRepository.obterPorId(servicoId);
        garantirServicoAtivo(servico);
        garantirIntervaloRazoavel(de, ate);

        List<Veterinario> veterinarios = veterinarioRepository.daClinica(servico.getClinica().getId());
        List<VagaResponse> vagas = new ArrayList<>();

        for (Veterinario veterinario : veterinarios) {
            if (veterinarioId != null && !veterinarioId.equals(veterinario.getId())) {
                continue;
            }
            for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
                for (AgendaService.Janela janela : agendaService.vagasLivres(veterinario.getId(), dia, servico)) {
                    if (respeitaAntecedencia(dia, janela.inicio())) {
                        vagas.add(new VagaResponse(dia,
                                janela.inicio().toString(), janela.fim().toString(),
                                veterinario.getId(), veterinario.getNome()));
                    }
                }
            }
        }
        return vagas;
    }

    /** Os agendamentos do tutor autenticado. */
    public Page<EventoClinicoResponse> meus(Pageable pageable) {
        UUID tutorId = seguranca.tutorIdParaFiltro();
        if (tutorId == null) {
            throw new RegraDeNegocioException("perfil",
                    "Esta consulta é do tutor. Use /eventos-clinicos para a visão da clínica");
        }
        return eventoClinicoRepository.doTutor(tutorId, pageable).map(eventoClinicoMapper::toResponse);
    }

    // ------------------------------------------------------------------
    // As regras, uma por metodo, com o numero da spec no nome do erro
    // ------------------------------------------------------------------

    private void garantirServicoAtivo(Servico servico) {
        if (!servico.isAtivo()) {
            throw new RegraDeNegocioException("servicoId",
                    "Este serviço não está sendo oferecido pela clínica");
        }
    }

    /**
     * C10 — consentir e ato do tutor, e de mais ninguem.
     *
     * A rota exige podeAcessarAnimal, que libera todo VETERINARIO para que o
     * profissional possa marcar pelo balcao. Sem esta checagem, o mesmo
     * caminho deixava a clinica marcar consentimentoHistorico=true e conceder A
     * SI MESMA dois anos de acesso ao historico — sem motivo, sem aviso ao
     * tutor e sem a marca de quebra de vidro que C21 existe para cobrar.
     *
     * Recusa em vez de ignorar em silencio: um pedido que diz ter consentimento
     * e e gravado sem ele deixaria a clinica convencida de ter acesso que nao
     * tem, e o tutor sem saber que alguem tentou.
     */
    private void garantirQueOConsentimentoEDeQuemPodeDar(Animal animal, AgendamentoRequest request) {
        if (!request.consentiu()) {
            return;
        }
        UUID tutorAutenticado = seguranca.tutorIdParaFiltro();
        boolean ehODono = tutorAutenticado != null
                && animal.getTutor() != null
                && tutorAutenticado.equals(animal.getTutor().getId());

        if (!ehODono) {
            throw new RegraDeNegocioException("consentimentoHistorico",
                    "Só o tutor do animal libera o histórico clínico. "
                            + "Marque o atendimento sem o consentimento e peça que ele autorize pelo aplicativo");
        }
    }

    private void garantirVeterinarioDaClinica(Veterinario veterinario, Servico servico) {
        if (veterinario.getClinica() == null
                || !veterinario.getClinica().getId().equals(servico.getClinica().getId())) {
            throw new RegraDeNegocioException("veterinarioId",
                    "O veterinário escolhido não atende na clínica deste serviço");
        }
    }

    /**
     * O @Future do DTO barra a data de ontem, mas nao hoje as 08:00 com o
     * relogio em 08:30. Aqui data e hora sao um instante so.
     */
    private void garantirAntecedenciaMinima(LocalDate data, String hora) {
        if (!respeitaAntecedencia(data, LocalTime.parse(hora))) {
            throw new RegraDeNegocioException("hora",
                    "Agendamentos exigem no mínimo " + HORAS_MINIMAS_PARA_AGENDAR
                            + " horas de antecedência. Para atendimento imediato, procure a clínica");
        }
    }

    private boolean respeitaAntecedencia(LocalDate data, LocalTime hora) {
        return LocalDateTime.of(data, hora)
                .isAfter(LocalDateTime.now().plusHours(HORAS_MINIMAS_PARA_AGENDAR));
    }

    private void garantirHorarioLivre(Veterinario veterinario, Servico servico, AgendamentoRequest request) {
        LocalTime inicio = LocalTime.parse(request.getHora());
        var janela = new AgendaService.Janela(inicio, inicio.plusMinutes(servico.getDuracaoMinutos()));

        String impedimento = agendaService.porQueNaoEstaLivre(veterinario.getId(), request.getData(), janela);
        if (impedimento != null) {
            throw new RegraDeNegocioException("hora", impedimento);
        }
    }

    /**
     * R19 e P12 — o mesmo fato pelos dois lados: nao se cancela um atendimento
     * que ja recebeu dinheiro sem devolver o dinheiro primeiro.
     *
     * Cancelar antes do estorno deixaria o pagamento preso a um evento que nao
     * vai acontecer: ele nao entra na inadimplencia (o evento nao e REALIZADO)
     * e nao aparece como devido a ninguem. O valor sumiria do painel sem sair
     * do caixa.
     */
    private void garantirQueNaoHaDinheiroPreso(EventoClinico evento) {
        if (pagamentoRepository.existsByEventoClinicoIdAndStatusPagamento(
                evento.getId(), StatusPagamento.PAGO)) {
            throw new RegraDeNegocioException("statusEvento",
                    "Este atendimento tem pagamento confirmado. Estorne o pagamento antes de cancelar");
        }
    }

    private boolean cancelamentoEmCimaDaHora(EventoClinico evento) {
        return LocalDateTime.of(evento.getData(), LocalTime.parse(evento.getHora()))
                .isBefore(LocalDateTime.now().plusHours(HORAS_PARA_CANCELAR_SEM_MARCA));
    }

    /**
     * A busca e um produto cartesiano de veterinarios x dias x slots: sem teto,
     * um intervalo de quatro anos varreria a agenda inteira da clinica.
     */
    private void garantirIntervaloRazoavel(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraDeNegocioException("ate", "A data final não pode ser anterior à inicial");
        }
        if (de.plusDays(MAXIMO_DE_DIAS_POR_CONSULTA).isBefore(ate)) {
            throw new RegraDeNegocioException("ate",
                    "Consulte no máximo " + MAXIMO_DE_DIAS_POR_CONSULTA + " dias por vez");
        }
    }

    private static final int MAXIMO_DE_DIAS_POR_CONSULTA = 60;
}
