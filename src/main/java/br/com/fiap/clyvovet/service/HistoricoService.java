package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.historico.*;
import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.LimiteDeAcessoExcedidoException;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.*;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * Acesso ao historico clinico em tres niveis (spec 08, fluxo C).
 *
 *   0  operacional         quem tem agendamento
 *   1  resumo de seguranca  qualquer veterinario autenticado
 *   2  historico completo   so com consentimento do tutor
 *
 * O nivel 1 e o que decide conduta nos primeiros minutos e expoe pouco; o
 * nivel 2 expoe muito e nao e o que resolve a emergencia. Dai a separacao.
 *
 * O microchip identifica, nunca autoriza — ele esta impresso na carteira de
 * vacinacao e qualquer leitor de pet shop o le.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoricoService {

    /**
     * Dois tetos, nao um. O de alerta e o volume de um dia cheio: passar dele
     * sinaliza, mas nao bloqueia — plantao de feriado e mutirao de castracao
     * produzem picos legitimos. O absoluto nenhuma jornada clinica alcanca.
     */
    private static final int TETO_DE_ALERTA_POR_DIA = 30;
    private static final int TETO_ABSOLUTO_POR_DIA = 150;

    /**
     * Quebra de vidro NUNCA bloqueia. Travar no limite significaria devolver 429
     * no lugar do historico em algum atendimento de emergencia, e essa conta e
     * paga pelo paciente. O acesso passa e o alarme sobe.
     */
    private static final int QUEBRAS_DE_VIDRO_POR_MES_ANTES_DE_ALARMAR = 5;

    private final AnimalRepository animalRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final AlertaClinicoRepository alertaRepository;
    private final AutorizacaoAcessoRepository autorizacaoRepository;
    private final AcessoHistoricoRepository acessoRepository;
    private final SegurancaService seguranca;

    /**
     * Nivel 1, pelo microchip. Sem consentimento e sem vinculo previo — e o
     * animal que chega numa clinica que nunca o atendeu. Toda leitura e
     * registrada e o tutor avisado.
     */
    @Transactional
    public ResumoDeSegurancaResponse porMicrochip(String microchip) {
        Animal animal = animalRepository.findByMicrochip(microchip)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        Recurso.ANIMAL, "Nenhum animal com o microchip informado"));

        garantirQueEVeterinario();
        garantirQueOResumoEstaLigado(animal);
        aplicarTetoDiario(animal);                                     // C6

        registrarAcesso(animal, NivelAcesso.RESUMO_DE_SEGURANCA, false, null);
        return montarResumo(animal);
    }

    /**
     * Nivel 2. Sem autorizacao o veterinario recebe o mesmo objeto com a linha
     * do tempo restrita a propria clinica — resposta menor, e nao 403: negar
     * esconderia que o animal existe e atrapalharia o atendimento em curso.
     */
    @Transactional
    public HistoricoResponse historico(UUID animalId) {
        Animal animal = animalRepository.obterPorId(animalId);
        NivelAcesso nivel = nivelSobre(animal);

        if (nivel == NivelAcesso.OPERACIONAL) {
            throw new RegraDeNegocioException("animalId",
                    "Sem acesso ao histórico deste animal");
        }
        registrarAcesso(animal, nivel, false, null);
        return montarHistorico(animal, nivel);
    }

    /**
     * Nivel 2 sem consentimento, em emergencia.
     *
     * Existe porque o consentimento nasce no agendamento: todo atendimento sem
     * agendamento — pronto-socorro, encaixe — ficaria sem caminho. O custo que
     * a torna aceitavel: motivo obrigatorio, registro destacado, aviso ao tutor.
     */
    @Transactional
    public HistoricoResponse acessoEmergencial(UUID animalId, String motivo) {
        Animal animal = animalRepository.obterPorId(animalId);
        garantirQueEVeterinario();

        registrarAcesso(animal, NivelAcesso.COMPLETO, true, motivo);
        log.warn("QUEBRA DE VIDRO: usuario {} acessou o historico do animal {} sem consentimento. Motivo: {}",
                seguranca.usuarioAutenticadoId(), animalId, motivo);
        notificarTutor(animal, "Acesso emergencial ao histórico de " + animal.getNome());
        alarmarSeQuebraDeVidroVirouRotina();                           // C22

        return montarHistorico(animal, NivelAcesso.COMPLETO);
    }

    /** O que o tutor ve quando pergunta quem andou lendo o historico do pet dele. */
    public List<AcessoResponse> acessos(UUID animalId) {
        animalRepository.garantirQueExiste(animalId);
        return acessoRepository.doAnimal(animalId).stream()
                .map(a -> new AcessoResponse(
                        a.getDia(),
                        a.getUsuario().getEmail(),
                        a.getClinica() != null ? a.getClinica().getNome() : null,
                        a.getNivel(),
                        a.getVezes(),
                        a.isEmergencial(),
                        a.getMotivo()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Resolucao de nivel
    // ------------------------------------------------------------------

    /** Quanto este usuario alcanca sobre este animal. */
    private NivelAcesso nivelSobre(Animal animal) {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        if (usuario == null) {
            return NivelAcesso.OPERACIONAL;
        }
        Perfil perfil = usuario.getUsuario().getPerfil();

        // O tutor dono ve tudo do proprio animal. O ADMIN da plataforma tambem.
        if (perfil == Perfil.ADMIN) {
            return NivelAcesso.COMPLETO;
        }
        if (perfil == Perfil.TUTOR) {
            boolean dono = animal.getTutor() != null
                    && animal.getTutor().getId().equals(usuario.getTutorId());
            return dono ? NivelAcesso.COMPLETO : NivelAcesso.OPERACIONAL;
        }

        UUID clinicaId = usuario.getClinicaId();
        if (clinicaId != null && temAutorizacaoVigente(animal.getId(), clinicaId)) {
            return NivelAcesso.COMPLETO;
        }
        // Sem consentimento a clinica ainda ve o que foi realizado nela (C0b).
        return NivelAcesso.RESUMO_DE_SEGURANCA;
    }

    private boolean temAutorizacaoVigente(UUID animalId, UUID clinicaId) {
        return autorizacaoRepository.findByAnimalIdAndClinicaId(animalId, clinicaId)
                .filter(a -> a.vigenteEm(LocalDate.now()))
                .isPresent();
    }

    // ------------------------------------------------------------------
    // Montagem das respostas
    // ------------------------------------------------------------------

    private ResumoDeSegurancaResponse montarResumo(Animal animal) {
        List<EventoClinico> eventos = eventoClinicoRepository.findByAnimalIdOrderByDataAsc(animal.getId());

        return new ResumoDeSegurancaResponse(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getPorte(),
                idadeEmMeses(animal.getDataNascimento()),
                animal.getCastrado(),
                ultimoPeso(eventos),
                alertas(animal.getId()),
                vacinas(eventos),
                // Do tutor, so o telefone: para a emergencia basta conseguir ligar.
                animal.getTutor() != null ? animal.getTutor().getTelefone() : null);
    }

    private HistoricoResponse montarHistorico(Animal animal, NivelAcesso nivel) {
        List<EventoClinico> eventos = eventoClinicoRepository.findByAnimalIdOrderByDataAsc(animal.getId());
        boolean completo = nivel == NivelAcesso.COMPLETO;

        UUID minhaClinica = seguranca.clinicaDoUsuario();

        List<LinhaDoTempoResponse> linha = eventos.stream()
                // Sem nivel 2, so os atendimentos da propria clinica (C0b).
                .filter(e -> completo || (minhaClinica != null && e.getClinica() != null
                        && minhaClinica.equals(e.getClinica().getId())))
                .map(e -> new LinhaDoTempoResponse(
                        e.getId(),
                        e.getData(),
                        e.getTipoEvento(),
                        e.getStatusEvento(),
                        e.getDescricao(),
                        e.getPesoKg(),
                        completo ? e.getDesfecho() : null,
                        e.getClinica() != null ? e.getClinica().getNome() : null,
                        minhaClinica != null && e.getClinica() != null
                                && minhaClinica.equals(e.getClinica().getId())))
                .toList();

        return new HistoricoResponse(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getPorte(),
                animal.getSexo(),
                animal.getDataNascimento(),
                idadeEmMeses(animal.getDataNascimento()),
                animal.getMicrochip(),
                animal.getCastrado(),
                nivel,
                alertas(animal.getId()),
                serieDePeso(eventos),
                vacinas(eventos),
                linha,
                completo && animal.getTutor() != null ? animal.getTutor().getNome() : null,
                animal.getTutor() != null ? animal.getTutor().getTelefone() : null);
    }

    private List<AlertaResponse> alertas(UUID animalId) {
        return alertaRepository.findByAnimalIdAndAtivoTrueOrderByTipoAsc(animalId).stream()
                .map(a -> new AlertaResponse(a.getId(), a.getTipo(), a.getDescricao(),
                        a.getOrigem(), a.getRegistradoEm()))
                .toList();
    }

    /**
     * Derivado dos eventos, nunca digitado a parte: um resumo mantido a mao
     * envelhece, e leva a revacinar sem necessidade ou a nao vacinar.
     */
    private List<VacinaResponse> vacinas(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEvento.VACINA)
                .filter(e -> e.getStatusEvento() == StatusEvento.REALIZADO)
                .map(e -> new VacinaResponse(e.getData(), e.getDescricao()))
                .toList();
    }

    private List<PesoResponse> serieDePeso(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getPesoKg() != null)
                .map(e -> new PesoResponse(e.getData(), e.getPesoKg()))
                .toList();
    }

    private java.math.BigDecimal ultimoPeso(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getPesoKg() != null)
                .reduce((primeiro, ultimo) -> ultimo)
                .map(EventoClinico::getPesoKg)
                .orElse(null);
    }

    private Integer idadeEmMeses(LocalDate nascimento) {
        if (nascimento == null) {
            return null;
        }
        Period p = Period.between(nascimento, LocalDate.now());
        return p.getYears() * 12 + p.getMonths();
    }

    // ------------------------------------------------------------------

    /**
     * Conta ANIMAIS DISTINTOS no dia, nao requisicoes: a tabela de auditoria ja
     * agrega por (usuario, animal, dia). Reabrir o mesmo prontuario nao consome
     * teto — e o que separa este controle do rate limit por IP.
     */
    private void aplicarTetoDiario(Animal animal) {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();

        boolean jaConsultouEsteAnimalHoje = acessoRepository
                .findByAnimalIdAndUsuarioIdAndDiaAndEmergencial(animal.getId(), usuarioId, hoje, false)
                .isPresent();
        if (jaConsultouEsteAnimalHoje) {
            return;
        }

        long animaisHoje = acessoRepository.countByUsuarioIdAndDiaAndEmergencial(usuarioId, hoje, false);

        if (animaisHoje >= TETO_ABSOLUTO_POR_DIA) {
            log.error("COLETA EM MASSA BLOQUEADA: usuario {} tentou o {}o animal distinto hoje",
                    usuarioId, animaisHoje + 1);
            throw new LimiteDeAcessoExcedidoException("microchip",
                    "Limite diário de consultas ao resumo de segurança atingido. "
                            + "Procure o administrador da plataforma");
        }
        if (animaisHoje == TETO_DE_ALERTA_POR_DIA) {
            // Uma vez, na travessia do limiar: repetir viraria ruido.
            log.warn("VOLUME ATIPICO: usuario {} passou de {} animais distintos hoje. "
                            + "Visivel em GET /auditoria/excessos",
                    usuarioId, TETO_DE_ALERTA_POR_DIA);
        }
    }

    /** Alarma; nao bloqueia. Ver a constante. */
    private void alarmarSeQuebraDeVidroVirouRotina() {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            return;
        }
        long noMes = acessoRepository.countByUsuarioIdAndEmergencialTrueAndDiaGreaterThanEqual(
                usuarioId, LocalDate.now().minusMonths(1));

        if (noMes > QUEBRAS_DE_VIDRO_POR_MES_ANTES_DE_ALARMAR) {
            log.error("QUEBRA DE VIDRO RECORRENTE: usuario {} acionou {} vezes em 30 dias. "
                            + "Excecao virou rotina — revisar",
                    usuarioId, noMes);
        }
    }

    private void garantirQueEVeterinario() {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        if (usuario == null || usuario.getUsuario().getPerfil() != Perfil.VETERINARIO) {
            throw new RegraDeNegocioException("perfil",
                    "O resumo de segurança é acessível a veterinários identificados");
        }
    }

    private void garantirQueOResumoEstaLigado(Animal animal) {
        if (Boolean.FALSE.equals(animal.getResumoDeSegurancaAtivo())) {
            throw new RegraDeNegocioException("microchip",
                    "O tutor desativou o resumo de segurança deste animal");
        }
    }

    /** Uma linha por (usuario, animal, dia); a segunda leitura incrementa. */
    private void registrarAcesso(Animal animal, NivelAcesso nivel, boolean emergencial, String motivo) {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        if (usuario == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();

        acessoRepository.findByAnimalIdAndUsuarioIdAndDiaAndEmergencial(
                        animal.getId(), usuario.getId(), hoje, emergencial)
                .ifPresentOrElse(
                        existente -> {
                            existente.setVezes(existente.getVezes() + 1);
                            // O maior nivel do dia prevalece.
                            existente.setNivel(Math.max(existente.getNivel(), nivel.getCodigo()));
                            acessoRepository.save(existente);
                        },
                        () -> {
                            AcessoHistorico acesso = new AcessoHistorico();
                            acesso.setAnimal(animal);
                            acesso.setUsuario(usuario.getUsuario());
                            acesso.setClinica(usuario.getUsuario().getVeterinario() != null
                                    ? usuario.getUsuario().getVeterinario().getClinica() : null);
                            acesso.setDia(hoje);
                            acesso.setNivel(nivel.getCodigo());
                            acesso.setEmergencial(emergencial);
                            acesso.setMotivo(motivo);
                            acessoRepository.save(acesso);
                        });

        if (!emergencial) {
            notificarTutor(animal, "Seu histórico de " + animal.getNome() + " foi consultado");
        }
    }

    /**
     * Placeholder: vai para o log ate existir canal real (push ou e-mail). O
     * ponto de chamada ja esta no lugar certo.
     */
    private void notificarTutor(Animal animal, String mensagem) {
        if (animal.getTutor() != null) {
            log.info("NOTIFICACAO ao tutor {}: {}", animal.getTutor().getId(), mensagem);
        }
    }
}
