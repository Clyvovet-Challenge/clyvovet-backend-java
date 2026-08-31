package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.autorizacao.AutorizacaoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.AutorizacaoAcessoRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O ciclo de vida do consentimento de acesso ao historico.
 *
 * NAO EXISTE ENDPOINT DE CONCESSAO, e isso e o desenho, nao uma lacuna. A
 * autorizacao nasce dentro do agendamento: o tutor ja esta decidindo onde
 * atender, e liberar o historico e parte da mesma escolha. Tirar o ciclo
 * pedir-esperar-aprovar remove uma tela e uma espera do caminho sem tirar a
 * decisao do tutor.
 *
 * O que sobra para este service e o resto do ciclo: estender, revogar,
 * listar — e a expiracao, que nao e um metodo porque nao e um evento.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacaoService {

    /**
     * A autorizacao vive enquanto a relacao viver.
     *
     * Dois anos apos o ULTIMO atendimento, e nao apos a concessao: quem
     * continua frequentando a clinica mantem o acesso sem precisar renovar
     * nada, e quem parou de ir ve a autorizacao expirar sozinha, sem precisar
     * lembrar de revogar. Um prazo curto obrigaria a reconsentir a cada
     * consulta; um prazo infinito seria acesso perpetuo por esquecimento.
     */
    private static final int ANOS_DE_VIGENCIA = 2;

    private final AutorizacaoAcessoRepository autorizacaoRepository;
    private final SegurancaService seguranca;

    /**
     * Chamado pelo agendamento quando o tutor consente.
     *
     * ESTENDE a autorizacao existente em vez de criar outra — e o que a
     * constraint uk_autorizacao_animal_clinica exige e o que mantem a lista do
     * tutor legivel: tres anos de consultas produziriam trinta linhas
     * empilhadas, e ele teria de revogar uma a uma.
     *
     * Reativa a revogada, e isso e deliberado: se o tutor revogou e depois
     * marcou de novo com consentimento, ele mudou de ideia. Manter a revogacao
     * valendo obrigaria a um segundo passo que ele ja deu.
     */
    @Transactional
    public void conceder(Animal animal, Clinica clinica, EventoClinico origem) {
        LocalDate validade = origem.getData().plusYears(ANOS_DE_VIGENCIA);

        AutorizacaoAcesso autorizacao = autorizacaoRepository
                .findByAnimalIdAndClinicaId(animal.getId(), clinica.getId())
                .orElseGet(AutorizacaoAcesso::new);

        autorizacao.setAnimal(animal);
        autorizacao.setClinica(clinica);
        autorizacao.setOrigemEvento(origem);
        autorizacao.setStatus(StatusAutorizacao.VIGENTE);
        autorizacao.setRevogadaEm(null);
        if (autorizacao.getConcedidaEm() == null) {
            autorizacao.setConcedidaEm(LocalDate.now());
        }
        // Nunca encurta: um agendamento para daqui a uma semana nao pode
        // reduzir a validade concedida por um atendimento mais recente.
        if (autorizacao.getValidoAte() == null || validade.isAfter(autorizacao.getValidoAte())) {
            autorizacao.setValidoAte(validade);
        }

        autorizacaoRepository.save(autorizacao);
        log.info("Consentimento registrado: animal {} liberado para a clinica {} ate {}",
                animal.getId(), clinica.getId(), autorizacao.getValidoAte());
    }

    /** O que o tutor ve quando pergunta quem tem acesso aos animais dele. */
    public List<AutorizacaoResponse> minhas() {
        UUID tutorId = seguranca.tutorIdParaFiltro();
        if (tutorId == null) {
            throw new RegraDeNegocioException("perfil",
                    "Esta consulta é do tutor: são as autorizações que ele concedeu");
        }
        LocalDate hoje = LocalDate.now();
        return autorizacaoRepository.doTutor(tutorId).stream()
                .map(a -> new AutorizacaoResponse(
                        a.getId(),
                        a.getAnimal().getId(),
                        a.getAnimal().getNome(),
                        a.getClinica().getId(),
                        a.getClinica().getNome(),
                        a.getStatus(),
                        a.getConcedidaEm(),
                        a.getValidoAte(),
                        a.getRevogadaEm(),
                        a.vigenteEm(hoje)))
                .toList();
    }

    /**
     * O tutor retira o acesso. Sem justificar, a qualquer momento.
     *
     * NAO APAGA o que o veterinario ja escreveu: registro clinico nao se
     * desfaz, e a clinica mantem a guarda dos atendimentos realizados nela
     * (C0b). O que a revogacao encerra e a leitura do historico CONSOLIDADO —
     * o que veio de outras clinicas e os documentos do tutor.
     */
    @Transactional
    public AutorizacaoResponse revogar(UUID id) {
        AutorizacaoAcesso autorizacao = autorizacaoRepository.obterPorId(id);

        UUID tutorId = seguranca.tutorIdParaFiltro();
        boolean dono = autorizacao.getAnimal().getTutor() != null
                && autorizacao.getAnimal().getTutor().getId().equals(tutorId);
        if (!dono) {
            throw new RegraDeNegocioException("id",
                    "Só o tutor do animal pode revogar o acesso ao histórico dele");
        }
        if (autorizacao.getStatus() == StatusAutorizacao.REVOGADA) {
            throw new RegraDeNegocioException("status", "Esta autorização já foi revogada");
        }

        autorizacao.setStatus(StatusAutorizacao.REVOGADA);
        autorizacao.setRevogadaEm(LocalDate.now());
        AutorizacaoAcesso salva = autorizacaoRepository.save(autorizacao);

        log.info("Consentimento revogado: animal {} deixou de liberar a clinica {}",
                salva.getAnimal().getId(), salva.getClinica().getId());

        return new AutorizacaoResponse(
                salva.getId(), salva.getAnimal().getId(), salva.getAnimal().getNome(),
                salva.getClinica().getId(), salva.getClinica().getNome(),
                salva.getStatus(), salva.getConcedidaEm(), salva.getValidoAte(),
                salva.getRevogadaEm(), false);
    }
}
