package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.exception.LimiteDeAcessoExcedidoException;
import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.AcessoHistorico;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.NivelAcesso;
import br.com.fiap.clyvovet.repository.AcessoHistoricoRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O rastro de quem leu o historico: a linha de auditoria do dia, o teto de
 * animais distintos, o alarme da quebra de vidro e o aviso ao tutor.
 *
 * <p>Morava no {@code HistoricoService}, e o {@code DocumentoClinicoService}
 * dependia dele inteiro so para registrar o download do laudo. Agora os dois
 * registram aqui. Nao confundir com o {@code AuditoriaService}, que e a LEITURA
 * dessas linhas pelo admin.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RegistroDeAcesso {

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

    private final AcessoHistoricoRepository acessoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SegurancaService seguranca;

    /**
     * Conta ANIMAIS DISTINTOS no dia, nao requisicoes: a tabela de auditoria ja
     * agrega por (usuario, animal, dia). Reabrir o mesmo prontuario nao consome
     * teto — e o que separa este controle do rate limit por IP.
     */
    void aplicarTetoDiario(Animal animal) {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            return;
        }
        // Trava o usuario: dois animais novos consultados juntos liam a mesma
        // contagem e passavam os dois pelo ultimo lugar do teto.
        usuarioRepository.travarPorId(usuarioId, Recurso.USUARIO);
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
    void alarmarSeQuebraDeVidroVirouRotina() {
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

    /**
     * Uma linha por (usuario, animal, dia); a segunda leitura incrementa.
     *
     * <p>Visivel ao pacote pelo mesmo motivo que {@code nivelSobre}: baixar o
     * laudo E ler o prontuario, e precisa aparecer na mesma auditoria que o
     * tutor consulta. Um download que nao registrasse acesso seria a unica porta
     * do sistema por onde se le o historico sem deixar rastro.</p>
     */
    void registrarAcesso(Animal animal, NivelAcesso nivel, boolean emergencial, String motivo) {
        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();
        if (usuario == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();

        acessoRepository.travarDoDia(animal.getId(), usuario.getId(), hoje, emergencial)
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
    void notificarTutor(Animal animal, String mensagem) {
        if (animal.getTutor() != null) {
            log.info("NOTIFICACAO ao tutor {}: {}", animal.getTutor().getId(), mensagem);
        }
    }

    /**
     * O veterinario abriu a quebra de vidro para este animal hoje?
     *
     * <p>Reusa a linha de auditoria que {@code acessoEmergencial} ja grava, em
     * vez de criar um token ou uma tabela de liberacao temporaria. O efeito
     * colateral e a propriedade que se queria: a liberacao do arquivo <b>e</b> o
     * registro dela — nao existe uma sem a outra. E ela expira sozinha a
     * meia-noite, porque a chave da linha inclui o dia.</p>
     */
    boolean quebrouOVidroHoje(Animal animal) {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            return false;
        }
        return acessoRepository.findByAnimalIdAndUsuarioIdAndDiaAndEmergencial(
                animal.getId(), usuarioId, LocalDate.now(), true).isPresent();
    }
}
