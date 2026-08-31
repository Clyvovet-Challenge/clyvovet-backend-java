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
 * O acesso ao historico clinico, nos tres niveis da spec 08.
 *
 *   0  operacional        quem tem agendamento
 *   1  resumo de seguranca  qualquer veterinario autenticado, sempre
 *   2  historico completo  so com consentimento do tutor
 *
 * O QUE SEPARA OS NIVEIS
 * Nem todo o historico pesa igual. O que salva a vida no primeiro minuto —
 * alergia, condicao cronica, medicacao continua, antirrabica, ultimo peso — e
 * pouco dado e expoe pouco. O que expoe muito — quais clinicas o animal
 * frequentou, laudos, diagnosticos, CPF e endereco do tutor — nao e o que
 * resolve a emergencia. Trancar os dois no mesmo cofre obrigaria a escolher
 * entre travar o atendimento de urgencia e abrir o prontuario inteiro.
 *
 * O MICROCHIP IDENTIFICA, NUNCA AUTORIZA
 * Ele esta impresso na carteira de vacinacao e no contrato de adocao, e
 * qualquer leitor de pet shop o le — como senha nao valeria nada. Quem
 * credencia o nivel 1 e a autenticacao do veterinario; o nivel 2, o tutor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoricoService {

    /**
     * OS DOIS TETOS DA REGRA C6 — e por que sao dois, e nao um.
     *
     * TETO_DE_ALERTA e o volume clinico plausivel de um dia cheio. Passar dele
     * nao bloqueia: sinaliza. Um plantao de feriado, um mutirao de castracao ou
     * um surto de parvovirose produzem picos legitimos, e travar o profissional
     * no meio deles seria transformar uma medida de privacidade em risco ao
     * paciente.
     *
     * TETO_ABSOLUTO e o volume que nenhuma jornada clinica alcanca. Passar dele
     * bloqueia, porque a essa altura ja nao ha leitura de boa-fe a proteger.
     *
     * Um teto so obrigaria a escolher entre bloquear cedo demais (e atrapalhar
     * atendimento) ou tarde demais (e nao proteger ninguem).
     */
    private static final int TETO_DE_ALERTA_POR_DIA = 30;
    private static final int TETO_ABSOLUTO_POR_DIA = 150;

    /**
     * O teto da regra C22 — quebra de vidro.
     *
     * ELE NUNCA BLOQUEIA, e essa e a decisao mais importante deste arquivo.
     * Quebra de vidro e o caminho do animal que chega atropelado, sem
     * agendamento e sem o tutor por perto. Bloquear no limite significaria que,
     * em algum atendimento, o veterinario abriria a tela e receberia um 429 no
     * lugar do historico — e a conta desse erro e paga pelo paciente, nao pelo
     * atacante.
     *
     * O controle e outro: o acesso passa, e o alarme sobe. Auditoria depois do
     * fato e a resposta certa para excecao de emergencia; bloqueio nao e.
     */
    private static final int QUEBRAS_DE_VIDRO_POR_MES_ANTES_DE_ALARMAR = 5;

    private final AnimalRepository animalRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final AlertaClinicoRepository alertaRepository;
    private final AutorizacaoAcessoRepository autorizacaoRepository;
    private final AcessoHistoricoRepository acessoRepository;
    private final SegurancaService seguranca;

    /**
     * Nivel 1 — o resumo de seguranca, alcancado pelo numero do microchip.
     *
     * Nao exige consentimento nem vinculo previo: e o caso do animal que chega
     * numa clinica que nunca o atendeu. Em compensacao, toda leitura e
     * registrada e o tutor e avisado — a transparencia e o que torna o acesso
     * sem consentimento aceitavel.
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
     * Nivel 2 — o historico completo.
     *
     * O que volta depende do nivel que o solicitante alcanca: sem autorizacao,
     * o veterinario recebe o mesmo objeto com a linha do tempo restrita aos
     * atendimentos da propria clinica (C0b). Nao e um 403 — e uma resposta
     * menor, e a diferenca importa: 403 esconderia que o animal existe e
     * atrapalharia o atendimento em curso.
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
     * Quebra de vidro — nivel 2 sem consentimento, em emergencia.
     *
     * DEIXOU DE SER OPCIONAL quando o consentimento passou a nascer no
     * agendamento: todo atendimento SEM agendamento — o pronto-socorro, o
     * animal atropelado, o encaixe — ficaria sem caminho de acesso. O modelo
     * travaria exatamente na emergencia, que e quando o historico mais importa.
     *
     * O que a torna aceitavel e o custo: motivo obrigatorio, registro
     * destacado, notificacao imediata ao tutor. Sem esses tres, "qualquer
     * veterinario com um campo de texto" seria uma porta aberta com livro de
     * visitas, e anularia todo o resto.
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

    /**
     * Quanto este usuario alcanca sobre este animal.
     *
     * A ordem das checagens e a ordem das bases legais: o dono primeiro, depois
     * a guarda do proprio registro (C0b), depois o consentimento. A primeira
     * que responde COMPLETO encerra — nao ha razao para consultar a tabela de
     * autorizacao quando o solicitante e o proprio tutor.
     */
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

        // Veterinario: consentimento vigente, ou a guarda do proprio registro.
        UUID clinicaId = usuario.getClinicaId();
        if (clinicaId != null && temAutorizacaoVigente(animal.getId(), clinicaId)) {
            return NivelAcesso.COMPLETO;
        }
        // C0b — a clinica sempre ve o que foi realizado nela. Sem consentimento
        // ela nao ve mais que isso, mas o proprio registro nao se tranca contra
        // quem tem o dever de guarda-lo.
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
                // Do tutor, so o telefone. Nome, CPF e endereco sao nivel 2:
                // para atender uma emergencia basta conseguir ligar.
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
                        // A marcacao "desta clinica x de outras" e o que da
                        // sentido ao nivel 2: sem consentimento o veterinario ve
                        // so a fatia dele; com ele, a linha inteira.
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
     * Derivado dos eventos, nunca digitado a parte.
     *
     * Um resumo de vacinas mantido a mao envelhece em silencio, e um resumo de
     * imunizacao desatualizado leva a revacinar sem necessidade — ou, pior, a
     * nao vacinar achando que ja se vacinou.
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
     * O teto de leitura do nivel 1 (regra C6).
     *
     * Conta ANIMAIS DISTINTOS no dia, nao requisicoes — a tabela de auditoria ja
     * agrega por (usuario, animal, dia), entao a contagem de linhas e exatamente
     * a metrica certa e sai de graca. Reabrir o mesmo prontuario nao consome
     * teto; e o que separa este controle do rate limit por IP, que protege a
     * infraestrutura contra rajada e nao sabe nada sobre pacientes.
     *
     * O animal ja consultado hoje nao entra na conta de novo: sem isso, um
     * atendimento longo com varias consultas ao resumo poderia esbarrar no teto
     * sem que nenhum paciente novo tivesse sido aberto.
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
            // Uma vez, na travessia do limiar — e nao a cada consulta acima
            // dele. Repetir transformaria o alerta em ruido, e ruido nao e lido.
            log.warn("VOLUME ATIPICO: usuario {} passou de {} animais distintos hoje. "
                            + "Visivel em GET /auditoria/excessos",
                    usuarioId, TETO_DE_ALERTA_POR_DIA);
        }
    }

    /**
     * A quebra de vidro que virou rotina (regra C22).
     *
     * Alarma; nao bloqueia. Ver a nota em QUEBRAS_DE_VIDRO_POR_MES_ANTES_DE_ALARMAR.
     */
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
        // O tutor pode desligar o nivel 1. Forcar seria paternalista — o dado e
        // dele —, mas o desligamento e uma escolha informada, nao o padrao.
        if (Boolean.FALSE.equals(animal.getResumoDeSegurancaAtivo())) {
            throw new RegraDeNegocioException("microchip",
                    "O tutor desativou o resumo de segurança deste animal");
        }
    }

    /**
     * Uma linha por (usuario, animal, dia): a segunda leitura do mesmo dia
     * incrementa o contador em vez de criar registro novo.
     */
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
                            // O nivel registrado e o MAIOR alcancado no dia: uma
                            // leitura completa nao pode ser apagada por uma
                            // consulta de resumo feita depois.
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
     * Placeholder da notificacao ao tutor.
     *
     * Vai para o log ate existir canal real (push ou e-mail). Deixar o ponto de
     * chamada no lugar certo desde agora e o que garante que a notificacao seja
     * ligada trocando UMA implementacao, e nao caçando os pontos onde ela
     * deveria ter sido disparada.
     */
    private void notificarTutor(Animal animal, String mensagem) {
        if (animal.getTutor() != null) {
            log.info("NOTIFICACAO ao tutor {}: {}", animal.getTutor().getId(), mensagem);
        }
    }
}
