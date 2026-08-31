package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
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

    /**
     * Id do tutor a usar como filtro nas listagens.
     * Devolve null para VETERINARIO e ADMIN, que enxergam a base inteira —
     * e o mesmo contrato dos demais filtros opcionais das queries.
     */
    public UUID tutorIdParaFiltro() {
        UsuarioAutenticado usuario = autenticado();
        if (usuario == null || usuario.getUsuario().getPerfil() != Perfil.TUTOR) {
            return null;
        }
        return usuario.getTutorId();
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

    public boolean podeAcessarEvento(UUID eventoId) {
        return podeAcessar(() -> eventoClinicoRepository.findById(eventoId)
                .map(EventoClinico::getAnimal)
                .map(Animal::getTutor)
                .map(Tutor::getId));
    }

    public boolean podeAcessarPagamento(UUID pagamentoId) {
        return podeAcessar(() -> pagamentoRepository.findById(pagamentoId)
                .map(Pagamento::getEventoClinico)
                .map(EventoClinico::getAnimal)
                .map(Animal::getTutor)
                .map(Tutor::getId));
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
