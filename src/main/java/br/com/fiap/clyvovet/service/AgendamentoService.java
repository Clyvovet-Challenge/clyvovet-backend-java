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
 * O fluxo de agendamento pelo tutor — regras A1 a A15 da spec 08.
 *
 * E o primeiro fluxo nao-CRUD do projeto, e a diferenca esta na estrutura: os
 * services existentes recebem um DTO, mapeiam e salvam. Aqui entre a entrada e
 * a gravacao ha uma decisao que consulta cinco entidades e pode recusar por
 * seis motivos distintos, cada um com uma mensagem que diz o que fazer a
 * seguir.
 *
 * O QUE ESTE SERVICE DELIBERADAMENTE NAO FAZ
 * Nao decide se o horario esta livre — isso e do {@link AgendaService}, porque
 * a mesma resposta e necessaria na listagem de vagas. Nao decide quem pode
 * agendar para qual animal — isso e do SegurancaService, que ja resolve
 * ownership para o resto do sistema. Concentrar aqui so o que e especifico do
 * agendamento e o que impede este arquivo de virar o depositario de tudo.
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
    private final AnimalRepository animalRepository;
    private final ServicoRepository servicoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final AgendaService agendaService;
    private final SegurancaService seguranca;
    private final AutorizacaoService autorizacaoService;

    /**
     * Marca o atendimento. Entrada, decisao, resultado.
     *
     * A ordem das validacoes segue o custo: as que so olham objetos ja
     * carregados vem antes das que consultam a agenda. Quem tenta agendar um
     * exame numa clinica que so faz consulta recebe a recusa sem que o banco
     * seja consultado sobre horarios.
     */
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

        EventoClinico salvo = eventoClinicoRepository.save(evento);

        // O CONSENTIMENTO E O AGENDAMENTO (spec 08, regras C8 a C11).
        //
        // Nao ha endpoint de concessao, e isso e o desenho: o tutor ja esta
        // decidindo onde atender, e liberar o historico e parte da mesma
        // escolha. Some um ciclo inteiro de pedir-esperar-aprovar, e com ele
        // uma tela e uma espera -- sem que a decisao saia das maos do tutor.
        //
        // Recusar e permitido e nao impede nada: o atendimento acontece com os
        // niveis 0 e 1. E o que faz o consentimento ser real em vez de um
        // pedagio na tela de agendamento.
        if (request.consentiu()) {
            autorizacaoService.conceder(animal, servico.getClinica(), salvo);
        }

        return eventoClinicoMapper.toResponse(salvo);
    }

    /**
     * Cancela um agendamento (A12, A13, A14).
     *
     * Nao apaga a linha. Um evento cancelado precisa continuar existindo para
     * que a taxa de cancelamento seja calculavel e para que o horario liberado
     * tenha rastro — DELETE aqui destruiria os dois.
     */
    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse cancelar(UUID id, String motivo) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);

        if (evento.getStatusEvento() != StatusEvento.AGENDADO) {
            throw new RegraDeNegocioException("statusEvento",
                    "Só é possível cancelar um atendimento que ainda está agendado");
        }

        // A13: o cancelamento em cima da hora e permitido, mas fica anotado. A
        // alternativa — recusar — empurraria o tutor a simplesmente nao
        // aparecer, o que e pior para a clinica: ela perde o horario sem saber.
        String marca = cancelamentoEmCimaDaHora(evento) ? "[TARDIO] " : "";
        evento.setStatusEvento(StatusEvento.CANCELADO);
        evento.setMotivoCancelamento(marca + motivo);

        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    /**
     * As vagas livres de um servico num intervalo de datas.
     *
     * E a consulta que o frontend usa para desenhar o calendario, e por isso
     * ela devolve o que PODE ser marcado, e nao o que existe: um dia sem
     * nenhuma vaga simplesmente nao aparece.
     */
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

    private void garantirVeterinarioDaClinica(Veterinario veterinario, Servico servico) {
        if (veterinario.getClinica() == null
                || !veterinario.getClinica().getId().equals(servico.getClinica().getId())) {
            throw new RegraDeNegocioException("veterinarioId",
                    "O veterinário escolhido não atende na clínica deste serviço");
        }
    }

    /**
     * A9 e A10 juntas.
     *
     * O @Future do DTO ja barra a data de ontem, mas nao alcanca o caso de hoje
     * as 08:00 com o relogio marcando 08:30 — a data e valida, o horario nao.
     * Aqui data e hora sao avaliadas como um instante so, que e como o tutor as
     * percebe.
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

    private boolean cancelamentoEmCimaDaHora(EventoClinico evento) {
        return LocalDateTime.of(evento.getData(), LocalTime.parse(evento.getHora()))
                .isBefore(LocalDateTime.now().plusHours(HORAS_PARA_CANCELAR_SEM_MARCA));
    }

    /**
     * Teto na janela de busca de vagas.
     *
     * A consulta e um produto cartesiano de veterinarios x dias x slots. Sem
     * teto, um "de 2026-01-01 ate 2030-01-01" varreria a agenda inteira da
     * clinica e devolveria dezenas de milhares de linhas — e o custo cairia no
     * banco, nao em quem pediu.
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
