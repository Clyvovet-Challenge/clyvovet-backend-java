package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.agenda.BloqueioRequest;
import br.com.fiap.clyvovet.dto.agenda.BloqueioResponse;
import br.com.fiap.clyvovet.dto.agenda.DisponibilidadeRequest;
import br.com.fiap.clyvovet.dto.agenda.DisponibilidadeResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Bloqueio;
import br.com.fiap.clyvovet.model.DisponibilidadeVeterinario;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.repository.BloqueioRepository;
import br.com.fiap.clyvovet.repository.DisponibilidadeVeterinarioRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.service.AgendaService.Janela;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manutencao da grade do veterinario: as faixas em que ele atende e os furos.
 *
 * Separado do {@link AgendaService}, que apenas LE a grade para decidir se um
 * horario esta livre. Aqui e onde a grade e escrita. A divisao mantem o leitor
 * — chamado a cada agendamento e a cada busca de vagas — livre de dependencias
 * de escrita.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgendaCadastroService {

    private final DisponibilidadeVeterinarioRepository disponibilidadeRepository;
    private final BloqueioRepository bloqueioRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final SegurancaService seguranca;

    public List<DisponibilidadeResponse> gradeDe(UUID veterinarioId) {
        veterinarioRepository.garantirQueExiste(veterinarioId);
        return disponibilidadeRepository
                .findByVeterinarioIdOrderByDiaSemanaAscHoraInicioAsc(veterinarioId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DisponibilidadeResponse criarFaixa(DisponibilidadeRequest request) {
        Veterinario veterinario = veterinarioRepository.obterPorId(request.getVeterinarioId());
        garantirQueEDonoDaAgenda(veterinario.getId());
        garantirFaixaCoerente(request);
        garantirQueNaoSobrepoe(request);

        DisponibilidadeVeterinario faixa = new DisponibilidadeVeterinario();
        faixa.setVeterinario(veterinario);
        faixa.setDiaSemana(request.getDiaSemana());
        faixa.setHoraInicio(request.getHoraInicio());
        faixa.setHoraFim(request.getHoraFim());
        faixa.setVigenciaInicio(request.getVigenciaInicio());
        faixa.setVigenciaFim(request.getVigenciaFim());

        return toResponse(disponibilidadeRepository.save(faixa));
    }

    @Transactional
    public void removerFaixa(UUID id) {
        DisponibilidadeVeterinario faixa = disponibilidadeRepository.obterPorId(id);
        garantirQueEDonoDaAgenda(faixa.getVeterinario().getId());
        disponibilidadeRepository.delete(faixa);
    }

    @Transactional
    public BloqueioResponse criarBloqueio(BloqueioRequest request) {
        Veterinario veterinario = veterinarioRepository.obterPorId(request.getVeterinarioId());
        garantirQueEDonoDaAgenda(veterinario.getId());
        garantirBloqueioCoerente(request);

        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setVeterinario(veterinario);
        bloqueio.setDataInicio(request.getDataInicio());
        bloqueio.setDataFim(request.getDataFim());
        bloqueio.setHoraInicio(request.getHoraInicio());
        bloqueio.setHoraFim(request.getHoraFim());
        bloqueio.setMotivo(request.getMotivo());

        return toResponse(bloqueioRepository.save(bloqueio));
    }

    @Transactional
    public void removerBloqueio(UUID id) {
        Bloqueio bloqueio = bloqueioRepository.obterPorId(id);
        garantirQueEDonoDaAgenda(bloqueio.getVeterinario().getId());
        bloqueioRepository.delete(bloqueio);
    }

    // ------------------------------------------------------------------

    /**
     * A grade e do profissional.
     *
     * A regra de rota so alcanca "e do corpo clinico?", e isso e grosso demais
     * para esta decisao: sem a checagem por recurso, um veterinario apagava a
     * grade de qualquer outro — inclusive de clinica concorrente, o que a tira
     * inteira da busca por vagas. Pelo POST, dava para criar disponibilidade
     * falsa no nome de outro profissional e gerar agendamentos que ninguem
     * atenderia.
     *
     * O ADMIN da plataforma passa: e ele quem monta a agenda inicial da clinica
     * e quem corrige quando o profissional nao tem mais acesso a conta.
     */
    private void garantirQueEDonoDaAgenda(UUID veterinarioId) {
        if (!seguranca.podeGerenciarAgendaDe(veterinarioId)) {
            throw new AccessDeniedException("A agenda é do próprio veterinário");
        }
    }

    private void garantirFaixaCoerente(DisponibilidadeRequest request) {
        if (!Janela.de(request.getHoraInicio(), request.getHoraFim()).ehCoerente()) {
            throw new RegraDeNegocioException("horaFim",
                    "O fim da faixa precisa ser posterior ao início");
        }
        if (request.getVigenciaFim() != null
                && request.getVigenciaFim().isBefore(request.getVigenciaInicio())) {
            throw new RegraDeNegocioException("vigenciaFim",
                    "O fim da vigência não pode ser anterior ao início");
        }
    }

    /**
     * Duas faixas sobrepostas no mesmo dia gerariam a MESMA vaga duas vezes na
     * listagem — e o tutor veria dois horarios identicos, um dos quais falharia
     * ao ser marcado. O banco nao pega isso: nao ha constraint capaz de
     * expressar sobreposicao de intervalos.
     */
    private void garantirQueNaoSobrepoe(DisponibilidadeRequest request) {
        Janela nova = Janela.de(request.getHoraInicio(), request.getHoraFim());

        // A colisao e perguntada a Janela. Antes a formula estava reescrita aqui
        // -- uma segunda implementacao da mesma regra que vive em colideCom, e
        // livre para divergir dela.
        boolean sobrepoe = disponibilidadeRepository
                .vigentesEm(request.getVeterinarioId(), request.getDiaSemana(), request.getVigenciaInicio())
                .stream()
                .map(existente -> Janela.de(existente.getHoraInicio(), existente.getHoraFim()))
                .anyMatch(nova::colideCom);

        if (sobrepoe) {
            throw new RegraDeNegocioException("horaInicio",
                    "Já existe uma faixa de atendimento sobreposta neste dia");
        }
    }

    private void garantirBloqueioCoerente(BloqueioRequest request) {
        if (request.getDataFim().isBefore(request.getDataInicio())) {
            throw new RegraDeNegocioException("dataFim",
                    "A data final não pode ser anterior à inicial");
        }
        boolean umaSo = (request.getHoraInicio() == null) != (request.getHoraFim() == null);
        if (umaSo) {
            throw new RegraDeNegocioException("horaInicio",
                    "Informe as duas horas para bloquear uma faixa, ou nenhuma para bloquear o dia inteiro");
        }
        if (request.getHoraInicio() != null
                && !Janela.de(request.getHoraInicio(), request.getHoraFim()).ehCoerente()) {
            throw new RegraDeNegocioException("horaFim",
                    "O fim do bloqueio precisa ser posterior ao início");
        }
    }

    private DisponibilidadeResponse toResponse(DisponibilidadeVeterinario faixa) {
        return new DisponibilidadeResponse(
                faixa.getId(),
                faixa.getVeterinario().getId(),
                faixa.getVeterinario().getNome(),
                faixa.getDiaSemana(),
                faixa.getHoraInicio(),
                faixa.getHoraFim(),
                faixa.getVigenciaInicio(),
                faixa.getVigenciaFim());
    }

    private BloqueioResponse toResponse(Bloqueio bloqueio) {
        return new BloqueioResponse(
                bloqueio.getId(),
                bloqueio.getVeterinario().getId(),
                bloqueio.getVeterinario().getNome(),
                bloqueio.getDataInicio(),
                bloqueio.getDataFim(),
                bloqueio.getHoraInicio(),
                bloqueio.getHoraFim(),
                bloqueio.getMotivo());
    }
}
