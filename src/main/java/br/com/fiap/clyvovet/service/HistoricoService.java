package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.historico.*;
import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.*;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final AnimalRepository animalRepository;
    private final AutorizacaoAcessoRepository autorizacaoRepository;
    private final AcessoHistoricoRepository acessoRepository;
    private final SegurancaService seguranca;
    private final MontadorDeHistorico montador;
    private final RegistroDeAcesso registroDeAcesso;

    /**
     * Nivel 1, pelo microchip. Sem consentimento e sem vinculo previo — e o
     * animal que chega numa clinica que nunca o atendeu. Toda leitura e
     * registrada e o tutor avisado.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResumoDeSegurancaResponse porMicrochip(String microchip) {
        Animal animal = animalRepository.findByMicrochip(microchip)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        Recurso.ANIMAL, "Nenhum animal com o microchip informado"));

        garantirQueEVeterinario();
        garantirQueOResumoEstaLigado(animal);
        registroDeAcesso.aplicarTetoDiario(animal);                                     // C6

        registroDeAcesso.registrarAcesso(animal, NivelAcesso.RESUMO_DE_SEGURANCA, false, null);
        return montador.resumo(animal);
    }

    /**
     * Nivel 2. Sem autorizacao o veterinario recebe o mesmo objeto com a linha
     * do tempo restrita a propria clinica — resposta menor, e nao 403: negar
     * esconderia que o animal existe e atrapalharia o atendimento em curso.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HistoricoResponse historico(UUID animalId) {
        Animal animal = animalRepository.obterPorId(animalId);
        NivelAcesso nivel = nivelSobre(animal);

        if (nivel == NivelAcesso.OPERACIONAL) {
            throw new RegraDeNegocioException("animalId",
                    "Sem acesso ao histórico deste animal");
        }
        registroDeAcesso.registrarAcesso(animal, nivel, false, null);
        return montador.historico(animal, nivel);
    }

    /**
     * Nivel 2 sem consentimento, em emergencia.
     *
     * Existe porque o consentimento nasce no agendamento: todo atendimento sem
     * agendamento — pronto-socorro, encaixe — ficaria sem caminho. O custo que
     * a torna aceitavel: motivo obrigatorio, registro destacado, aviso ao tutor.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HistoricoResponse acessoEmergencial(UUID animalId, String motivo) {
        Animal animal = animalRepository.obterPorId(animalId);
        garantirQueEVeterinario();

        registroDeAcesso.registrarAcesso(animal, NivelAcesso.COMPLETO, true, motivo);
        log.warn("QUEBRA DE VIDRO: usuario {} acessou o historico do animal {} sem consentimento. Motivo: {}",
                seguranca.usuarioAutenticadoId(), animalId, motivo);
        registroDeAcesso.notificarTutor(animal, "Acesso emergencial ao histórico de " + animal.getNome());
        registroDeAcesso.alarmarSeQuebraDeVidroVirouRotina();                           // C22

        return montador.historico(animal, NivelAcesso.COMPLETO);
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
     * <p>Visivel ao pacote, e nao privada, porque {@code DocumentoClinicoService}
     * decide sobre o ARQUIVO do prontuario com a mesma regra que decide sobre o
     * texto dele. Duplicar esta resolucao la seria criar uma segunda politica de
     * acesso ao mesmo dado clinico — e a segunda envelheceria sozinha.</p>
     */
    NivelAcesso nivelSobre(Animal animal) {
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

        // O ADMIN_CLINICA PARA AQUI, e o veterinario nao.
        //
        // O nivel 1 existe por uma razao nomeada — proteger a vida do animal. E o
        // que permite ao profissional saber que o cachorro na mesa e alergico a
        // dipirona ANTES de aplicar, mesmo sem consentimento e mesmo sem nunca o ter
        // atendido. A credencial e a autenticacao do veterinario, e a justificativa
        // e o atendimento em curso.
        //
        // O administrador da clinica nao atende ninguem. Ele responde pelo negocio:
        // servicos, profissionais, agenda, faturamento. Deixa-lo cair no ramo abaixo
        // — que era o comportamento antes desta linha — entregava alergia, condicao
        // cronica, medicacao continua e o telefone do tutor de QUALQUER animal da
        // plataforma a quem so precisa administrar um estabelecimento. Verificado em
        // teste: o pedido respondia 200 com o resumo montado.
        //
        // Com consentimento ele passa, no ramo acima: ai a autorizacao foi dada a
        // clinica, e ele responde por ela.
        if (perfil == Perfil.ADMIN_CLINICA) {
            return NivelAcesso.OPERACIONAL;
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
}
