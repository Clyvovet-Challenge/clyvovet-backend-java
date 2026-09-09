package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.AutorizacaoAcessoRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
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
    private final VeterinarioRepository veterinarioRepository;
    private final ServicoRepository servicoRepository;

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
            // O ADMIN_CLINICA tem o MESMO recorte do veterinario da casa: a
            // propria clinica. O que muda entre os dois nao e o alcance das
            // listagens -- e o que cada um pode ESCREVER, e isso vive nas regras
            // de rota, nao aqui.
            case ADMIN_CLINICA -> RecorteDeAcesso.daClinica(usuario.getClinicaId());
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

        // O ADMIN_CLINICA chega aqui: ele nao tem tutorId, entao o ramo acima nao
        // o alcanca, e getClinicaId() resolve pelo vinculo direto. O resultado e o
        // que se espera — ele ve o que aconteceu na casa dele, e fora dela so com
        // consentimento, exatamente como os proprios veterinarios.
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

    /**
     * VETERINARIO e ADMIN enxergam toda a base; TUTOR so o proprio escopo.
     *
     * <p><b>O ADMIN_CLINICA fica de fora de proposito.</b> Isto aqui abre o
     * cadastro de QUALQUER tutor e QUALQUER animal da plataforma — e o
     * veterinario esta na lista porque precisa: ele atende um animal que nunca
     * viu, e o nivel 0 do historico existe para isso.</p>
     *
     * <p>O administrador da clinica nao atende ninguem. Ele responde pelo negocio:
     * servicos, profissionais, agenda, faturamento. Incluir o perfil aqui entregaria
     * o CPF e o e-mail de todos os tutores da plataforma a quem so precisa
     * administrar um estabelecimento.</p>
     *
     * <p>Ele nao fica sem nada: o que e da clinica dele chega por outro caminho —
     * {@link #recorte()} devolve {@code daClinica(...)}, que filtra as listagens, e
     * {@code podeAlcancarOAtendimento} deixa passar o que aconteceu na casa. O que
     * ele perde e so o que nao lhe diz respeito.</p>
     */
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
        // O administrador da clinica gerencia a grade de QUALQUER profissional da
        // casa -- e essa e a diferenca dele para o veterinario, que so mexe na
        // propria. Sem isto, "gerenciar seus veterinarios" nao passa de um titulo.
        if (ehAdministradorDe(clinicaDoVeterinario(veterinarioId))) {
            return true;
        }
        UsuarioAutenticado usuario = autenticado();
        return usuario != null && veterinarioId.equals(usuario.getVeterinarioId());
    }

    // ------------------------------------------------------------------
    // Administrador da clinica
    // ------------------------------------------------------------------

    /**
     * Se quem chama administra ESTA clinica.
     *
     * <p>O ADMIN da plataforma passa em todas; o ADMIN_CLINICA so na sua. E os
     * demais perfis nao passam em nenhuma — inclusive o veterinario, que trabalha
     * na clinica sem responder por ela.</p>
     *
     * <p>Nulo devolve false, e nao true. O caminho que leva um {@code clinicaId} a
     * ser nulo aqui e o recurso nao ter clinica, ou nao existir — e nenhum dos dois
     * e motivo para autorizar.</p>
     */
    public boolean ehAdministradorDe(UUID clinicaId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        if (clinicaId == null) {
            return false;
        }
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null || usuario.getUsuario().getPerfil() != Perfil.ADMIN_CLINICA) {
            return false;
        }
        return clinicaId.equals(usuario.getClinicaId());
    }

    /**
     * Se quem chama pode mexer no catalogo desta clinica.
     *
     * <p>Existe com nome proprio, em vez de {@code ehAdministradorDe(#clinicaId)}
     * direto na anotacao, porque preco e duracao decidem quanto se cobra e como a
     * agenda e ocupada — e quem le a regra de rota merece ver isso dito.</p>
     */
    public boolean podeGerirCatalogoDe(UUID clinicaId) {
        return ehAdministradorDe(clinicaId);
    }

    /**
     * Se quem chama pode mexer NESTE servico.
     *
     * <p>Recurso inexistente devolve <b>true</b>, e isso e deliberado: e a mesma
     * escolha ja documentada em {@code podeAcessarEvento}. A autorizacao nao decide
     * sobre o que nao existe — quem responde 404 e o service. Com false aqui,
     * apagar um servico e busca-lo em seguida devolveria 403, o que sugere que ele
     * existe e nao e seu.</p>
     */
    public boolean podeGerirServico(UUID servicoId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        return servicoRepository.findById(servicoId)
                .map(servico -> ehAdministradorDe(
                        servico.getClinica() != null ? servico.getClinica().getId() : null))
                .orElse(true);
    }

    /** Mesma regra, para o cadastro do profissional. Ver a nota acima sobre o 404. */
    public boolean podeGerirVeterinario(UUID veterinarioId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        return veterinarioRepository.findById(veterinarioId)
                .map(vet -> ehAdministradorDe(
                        vet.getClinica() != null ? vet.getClinica().getId() : null))
                .orElse(true);
    }

    /**
     * Se quem chama pode abrir o painel DESTA clinica.
     *
     * <p>Passam o ADMIN da plataforma, o ADMIN_CLINICA da casa e — esta e a parte
     * que merece justificativa — o VETERINARIO que atende nela.</p>
     *
     * <p>O veterinario entra porque ele JA alcanca cada linha que o painel soma: a
     * lista de inadimplencia da clinica, o extrato dos tutores dela e os
     * atendimentos, todos recortados pela mesma clinica e todos abertos ao corpo
     * clinico. Negar so o total seria uma regra que parece mais estrita do que e —
     * bastaria somar a mao o que a API ja entrega item a item. Regra que nao segura
     * nada e pior que regra nenhuma: ela e lida como protecao.</p>
     *
     * <p>O TUTOR nao passa em nenhuma. Nao ha versao "so a minha parte" disto: o
     * painel e faturamento, taxa de falta e desfecho clinico de pacientes que nao
     * sao dele.</p>
     */
    public boolean podeVerPainelDe(UUID clinicaId) {
        if (ehAdministradorDaPlataforma()) {
            return true;
        }
        if (clinicaId == null) {
            return false;
        }
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null) {
            return false;
        }
        Perfil perfil = usuario.getUsuario().getPerfil();
        if (perfil != Perfil.ADMIN_CLINICA && perfil != Perfil.VETERINARIO) {
            return false;
        }
        return clinicaId.equals(usuario.getClinicaId());
    }

    /** A clinica de um veterinario, ou null se ele nao existe ou nao tem uma. */
    private UUID clinicaDoVeterinario(UUID veterinarioId) {
        if (veterinarioId == null) {
            return null;
        }
        return veterinarioRepository.findById(veterinarioId)
                .map(v -> v.getClinica() != null ? v.getClinica().getId() : null)
                .orElse(null);
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
