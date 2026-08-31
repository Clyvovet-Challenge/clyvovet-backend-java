package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.autorizacao.AutorizacaoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.AutorizacaoAcessoRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida do consentimento de acesso ao historico.
 *
 * Nao ha endpoint de concessao: a autorizacao nasce dentro do agendamento. O
 * que sobra aqui e estender, revogar e listar.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacaoService {

    /**
     * Dois anos apos o ULTIMO atendimento, e nao apos a concessao: quem continua
     * indo mantem sem renovar, quem parou de ir expira sem precisar revogar.
     */
    private static final int ANOS_DE_VIGENCIA = 2;

    private final AutorizacaoAcessoRepository autorizacaoRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final SegurancaService seguranca;

    /**
     * Estende a autorizacao existente em vez de criar outra: tres anos de
     * consultas produziriam trinta linhas para o tutor revogar uma a uma.
     *
     * Reativa a revogada de proposito — quem revogou e marcou de novo
     * consentindo mudou de ideia.
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

    /**
     * C14 — cancelar o agendamento revoga o consentimento SE nunca houve
     * atendimento naquela clinica.
     *
     * As duas metades importam. Sem a revogacao, consentir e cancelar em
     * seguida deixava a clinica com dois anos de acesso ao historico de um
     * animal que ela nunca viu — bastava induzir um agendamento para comprar o
     * prontuario. Com a revogacao incondicional, cancelar uma consulta depois
     * de tres anos de relacao apagaria o acesso construido nesses tres anos,
     * no meio de um tratamento.
     *
     * Silenciosa quando nao ha o que revogar: o cancelamento nao pode falhar
     * porque o consentimento nunca existiu.
     */
    @Transactional
    public void revogarSeNuncaHouveAtendimento(EventoClinico cancelado) {
        if (cancelado.getAnimal() == null || cancelado.getClinica() == null) {
            return;
        }
        UUID animalId = cancelado.getAnimal().getId();
        UUID clinicaId = cancelado.getClinica().getId();

        if (eventoClinicoRepository.houveAtendimento(animalId, clinicaId)) {
            return;
        }
        autorizacaoRepository.findByAnimalIdAndClinicaId(animalId, clinicaId)
                .filter(a -> a.getStatus() == StatusAutorizacao.VIGENTE)
                .ifPresent(autorizacao -> {
                    autorizacao.setStatus(StatusAutorizacao.REVOGADA);
                    autorizacao.setRevogadaEm(LocalDate.now());
                    autorizacaoRepository.save(autorizacao);
                    log.info("Consentimento revogado com o cancelamento: animal {} nunca foi atendido na clinica {}",
                            animalId, clinicaId);
                });
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
     * Nao apaga o que o veterinario escreveu: a clinica mantem a guarda dos
     * atendimentos realizados nela. Encerra a leitura do historico consolidado.
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
