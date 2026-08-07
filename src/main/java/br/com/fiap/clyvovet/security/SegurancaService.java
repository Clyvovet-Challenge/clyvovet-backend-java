package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        if (temVisaoAmpla()) {
            return true;
        }
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && meuTutorId.equals(tutorId);
    }

    public boolean podeAcessarAnimal(UUID animalId) {
        if (temVisaoAmpla()) {
            return true;
        }
        return animalRepository.findById(animalId)
                .map(Animal::getTutor)
                .map(Tutor::getId)
                .map(this::ehMeuTutor)
                .orElse(false);
    }

    public boolean podeAcessarEvento(UUID eventoId) {
        if (temVisaoAmpla()) {
            return true;
        }
        return eventoClinicoRepository.findById(eventoId)
                .map(EventoClinico::getAnimal)
                .map(Animal::getTutor)
                .map(Tutor::getId)
                .map(this::ehMeuTutor)
                .orElse(false);
    }

    public boolean podeAcessarPagamento(UUID pagamentoId) {
        if (temVisaoAmpla()) {
            return true;
        }
        return pagamentoRepository.findById(pagamentoId)
                .map(Pagamento::getEventoClinico)
                .map(EventoClinico::getAnimal)
                .map(Animal::getTutor)
                .map(Tutor::getId)
                .map(this::ehMeuTutor)
                .orElse(false);
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

    private boolean ehMeuTutor(UUID tutorId) {
        UUID meuTutorId = tutorIdDoUsuario();
        return meuTutorId != null && meuTutorId.equals(tutorId);
    }

    private UUID tutorIdDoUsuario() {
        UsuarioAutenticado usuario = autenticado();
        return usuario != null ? usuario.getTutorId() : null;
    }

    private UsuarioAutenticado autenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return null;
        }
        return usuario;
    }
}
