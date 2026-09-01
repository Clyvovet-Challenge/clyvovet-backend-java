package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.AutorizacaoAcessoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Decisoes de ownership: regra de rota resolve "qual perfil acessa qual rota",
 * mas nao resolve "este tutor pode ver este pet". E consultado tanto pelas
 * anotacoes @PreAuthorize (acesso por id) quanto pelos services (filtro das
 * listagens, via tutorIdParaFiltro).
 *
 * Registrado como bean "seguranca" para uso em SpEL:
 *     @PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
 */
@Service("seguranca")
@RequiredArgsConstructor
public class SegurancaService {

    private final AnimalRepository animalRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AutorizacaoAcessoRepository autorizacaoRepository;

    /**
     * O recorte das listagens deste usuario, resolvido de uma vez.
     *
     * Existe para que a chave do cache e o filtro da consulta saiam da MESMA
     * fonte. Enquanto cada service montava os dois a mao, era possivel — e
     * aconteceu — a consulta recortar por clinica e a chave nao, servindo a
     * pagina de um veterinario ao de outra clinica.
     */
    public RecorteDeAcesso recorte() {
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null) {
            return RecorteDeAcesso.irrestrito();
        }
        return switch (usuario.getUsuario().getPerfil()) {
            case TUTOR -> RecorteDeAcesso.doTutor(usuario.getTutorId());
            case VETERINARIO -> RecorteDeAcesso.daClinica(usuario.getClinicaId());
            case ADMIN -> RecorteDeAcesso.irrestrito();
        };
    }

    public UUID tutorIdParaFiltro() {
        return recorte().tutorId();
    }

    /**
     * Clinica a usar como recorte nas listagens de atendimento e pagamento.
     *
     * Devolve null para TUTOR (que ja e recortado por tutorIdParaFiltro) e para
     * ADMIN. Para o veterinario devolve a clinica dele: sem isso,
     * GET /eventos-clinicos entrega o historico de atendimento de todas as
     * clinicas da plataforma, inclusive concorrentes.
     */
    public UUID clinicaParaFiltro() {
        return recorte().clinicaId();
    }

    public boolean podeAcessarTutor(UUID tutorId) {
        return podeAcessar(() -> Optional.ofNullable(tutorId));
    }

    /**
     * Autoriza a atribuicao de dono vinda de um PATCH.
     *
     * Num PATCH o tutorId ausente significa "nao mexa no dono", e isso e sempre
     * permitido a quem ja pode editar o animal. Chamar podeAcessarTutor(null)
     * direto devolveria false -- o Optional vazio nunca casa com o tutor
     * autenticado -- e um tutor ficaria impedido de, por exemplo, corrigir o
     * nome do proprio pet sem reenviar o proprio id no corpo.
     */
    public boolean podeAtribuirTutor(UUID tutorId) {
        return tutorId == null || podeAcessarTutor(tutorId);
    }

    public boolean podeAcessarAnimal(UUID animalId) {
        return podeAcessar(() -> animalRepository.findById(animalId)
                .map(Animal::getTutor)
                .map(Tutor::getId));
    }

    /**
     * O atendimento e registro clinico: o veterinario nao alcanca o de qualquer
     * animal, so o da propria clinica (regra C0b) ou o que o tutor autorizou.
     *
     * Diferente de podeAcessarAnimal, que continua liberando todo VETERINARIO:
     * o CADASTRO do animal e nivel 0, e o profissional precisa dele para
     * atender. O que muda aqui e o HISTORICO.
     */
    public boolean podeAcessarEvento(UUID eventoId) {
        return eventoClinicoRepository.findById(eventoId)
                .map(this::podeAlcancarOAtendimento)
                // Recurso inexistente PASSA: a autorizacao nao decide sobre o
                // que nao existe, e quem responde 404 e o service. Com false
                // aqui, apagar um evento e busca-lo em seguida devolveria 403 --
                // o que sugere que ele existe e nao e seu.
                .orElse(true);
    }

    public boolean podeAcessarPagamento(UUID pagamentoId) {
        return pagamentoRepository.findById(pagamentoId)
                .map(Pagamento::getEventoClinico)
                .map(this::podeAlcancarOAtendimento)
                .orElse(true);   // ver a nota em podeAcessarEvento
    }

    /**
     * Tutor dono, ADMIN da plataforma, a clinica onde o atendimento aconteceu,
     * ou clinica com consentimento vigente sobre aquele animal.
     */
    private boolean podeAlcancarOAtendimento(EventoClinico evento) {
        if (evento == null) {
            return false;
        }
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null) {
            return false;
        }
        if (usuario.getUsuario().getPerfil() == Perfil.ADMIN) {
            return true;
        }

        UUID meuTutorId = usuario.getTutorId();
        if (meuTutorId != null) {
            return evento.getAnimal() != null && evento.getAnimal().getTutor() != null
                    && meuTutorId.equals(evento.getAnimal().getTutor().getId());
        }

        UUID minhaClinica = usuario.getClinicaId();
        if (minhaClinica == null) {
            return false;
        }
        // C0b: a clinica sempre ve o que foi realizado nela.
        if (evento.getClinica() != null && minhaClinica.equals(evento.getClinica().getId())) {
            return true;
        }
        // Fora dela, so com consentimento do tutor.
        return evento.getAnimal() != null
                && autorizacaoRepository.findByAnimalIdAndClinicaId(evento.getAnimal().getId(), minhaClinica)
                        .filter(a -> a.vigenteEm(LocalDate.now()))
                        .isPresent();
    }

    /**
     * A decisao e sempre a mesma — visao ampla passa; tutor so passa no que e
     * dele — e so muda o caminho ate o dono do recurso. Cada metodo publico
     * declara esse caminho e nada mais.
     *
     * O dono chega como Supplier, e nao como valor pronto, para que a consulta
     * ao banco nao aconteca quando o perfil ja tem visao ampla.
     */
    private boolean podeAcessar(Supplier<Optional<UUID>> tutorDonoDoRecurso) {
        if (temVisaoAmpla()) {
            return true;
        }
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && tutorDonoDoRecurso.get().filter(meuTutorId::equals).isPresent();
    }

    /** VETERINARIO e ADMIN enxergam toda a base; TUTOR so o proprio escopo. */
    private boolean temVisaoAmpla() {
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null) {
            return false;
        }
        Perfil perfil = usuario.getUsuario().getPerfil();
        return perfil == Perfil.ADMIN || perfil == Perfil.VETERINARIO;
    }

    private UUID tutorIdDoUsuario() {
        UsuarioAutenticado usuario = autenticado();
        return usuario != null ? usuario.getTutorId() : null;
    }

    /**
     * O tutor dono do animal, ou o ADMIN da plataforma. E so.
     *
     * DIFERENTE de podeAcessarAnimal, e a diferenca e o ponto: aquele passa por
     * temVisaoAmpla e libera todo VETERINARIO. Serve para o cadastro do animal,
     * que o profissional precisa ler para atender — mas nao serve para o que e
     * do dono e so dele.
     *
     * Hoje sustenta a auditoria de acesso ao historico: a lista de quem leu o
     * prontuario e a ferramenta de transparencia DO TUTOR. Aberta ao corpo
     * clinico, ela vira o contrario disso — expoe o e-mail dos profissionais de
     * outras clinicas e revela quais delas atenderam aquele paciente.
     */
    public boolean ehDonoOuAdministrador(UUID animalId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && animalRepository.findById(animalId)
                .map(Animal::getTutor)
                .map(Tutor::getId)
                .filter(meuTutorId::equals)
                .isPresent();
    }

    /**
     * O usuario pode mexer na agenda deste veterinario?
     *
     * A grade e do profissional. Sem esta checagem, a regra de rota
     * hasAnyRole(VETERINARIO, ADMIN) libera qualquer veterinario a apagar a
     * grade de qualquer outro — inclusive de clinica concorrente, o que tira a
     * clinica inteira da busca por vagas.
     */
    public boolean podeGerenciarAgendaDe(UUID veterinarioId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        UsuarioAutenticado usuario = autenticado();
        return usuario != null && veterinarioId.equals(usuario.getVeterinarioId());
    }

    public boolean ehAdministradorDaPlataforma() {
        UsuarioAutenticado usuario = autenticado();
        return usuario != null && usuario.getUsuario().getPerfil() == Perfil.ADMIN;
    }

    /**
     * O usuario corrente, ou null.
     *
     * Publico porque o HistoricoService precisa resolver NIVEL de acesso, e nao
     * apenas sim-ou-nao: a pergunta dele nao e "pode?", e "quanto?". Manter a
     * resolucao de identidade aqui evita que ele leia o SecurityContextHolder
     * por conta propria e passe a existir uma segunda nocao de "quem esta
     * logado" no sistema.
     */
    public UsuarioAutenticado autenticadoOuNulo() {
        return autenticado();
    }

    public UUID usuarioAutenticadoId() {
        UsuarioAutenticado usuario = autenticado();
        return usuario != null ? usuario.getId() : null;
    }

    /** Clinica do veterinario logado. Sustenta a regra C0b — a guarda do proprio registro. */
    public UUID clinicaDoUsuario() {
        UsuarioAutenticado usuario = autenticado();
        return usuario != null ? usuario.getClinicaId() : null;
    }

    private UsuarioAutenticado autenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return null;
        }
        return usuario;
    }
}
