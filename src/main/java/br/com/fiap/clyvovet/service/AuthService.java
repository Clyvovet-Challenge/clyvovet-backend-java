package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.auth.*;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import br.com.fiap.clyvovet.security.ControleTentativasLogin;
import br.com.fiap.clyvovet.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Mensagem unica para credencial errada, e-mail inexistente e conta bloqueada.
     * Mensagens distintas permitiriam descobrir quais e-mails existem na base
     * apenas observando a resposta (enumeracao de usuarios).
     */
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais invalidas";

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ControleTentativasLogin controleTentativas;

    /**
     * Sem @Transactional de proposito: o login so le, e o registro da tentativa
     * e commitado a parte pelo ControleTentativasLogin. Abrir uma transacao aqui
     * faria o rollback do BadCredentialsException apagar a contagem de falhas.
     */
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(request.getEmail());

        if (encontrado.isEmpty()) {
            // Gasta o mesmo tempo de um BCrypt real para nao vazar, pelo tempo de
            // resposta, se o e-mail existe ou nao.
            passwordEncoder.encode(request.getSenha());
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        Usuario usuario = encontrado.get();

        if (!usuario.isAtivo() || usuario.estaBloqueado()) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            controleTentativas.registrarFalha(usuario);
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        controleTentativas.registrarSucesso(usuario);
        return montarResposta(usuario);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.lerClaims(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token invalido ou expirado");
        }

        // Um access token nao pode ser usado para renovar a si mesmo.
        if (!jwtService.ehRefreshToken(claims)) {
            throw new BadCredentialsException("Token informado nao e um refresh token");
        }

        Usuario usuario = usuarioRepository.findById(jwtService.extrairUsuarioId(claims))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido ou expirado"));

        if (!usuario.isAtivo() || usuario.estaBloqueado()) {
            throw new BadCredentialsException("Refresh token invalido ou expirado");
        }

        return montarResposta(usuario);
    }

    /** Auto-cadastro publico. O perfil e sempre TUTOR, nunca vem da requisicao. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        garantirEmailDisponivel(request.getEmail());

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setPerfil(Perfil.TUTOR);
        usuario.setAtivo(true);

        if (request.getTutorId() != null) {
            usuario.setTutor(buscarTutor(request.getTutorId()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    /** Criacao com perfil arbitrario. Restrito a ADMIN pela regra de rota. */
    @Transactional
    public UsuarioResponse criarUsuario(UsuarioRequest request) {
        garantirEmailDisponivel(request.getEmail());

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setPerfil(request.getPerfil());
        usuario.setAtivo(true);

        if (request.getTutorId() != null) {
            usuario.setTutor(buscarTutor(request.getTutorId()));
        }
        if (request.getVeterinarioId() != null) {
            usuario.setVeterinario(buscarVeterinario(request.getVeterinarioId()));
        }

        validarVinculo(usuario);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado com ID: " + id));
    }

    private LoginResponse montarResposta(Usuario usuario) {
        return LoginResponse.de(
                jwtService.gerarAccessToken(usuario),
                jwtService.gerarRefreshToken(usuario),
                jwtService.getValidadeAccessSegundos(),
                usuario.getPerfil());
    }

    private void garantirEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("email", "Ja existe usuario com o e-mail informado");
        }
    }

    private void validarVinculo(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.TUTOR && usuario.getVeterinario() != null) {
            throw new RegraDeNegocioException("veterinarioId", "Usuario com perfil TUTOR nao pode ser vinculado a um veterinario");
        }
        if (usuario.getPerfil() == Perfil.VETERINARIO && usuario.getTutor() != null) {
            throw new RegraDeNegocioException("tutorId", "Usuario com perfil VETERINARIO nao pode ser vinculado a um tutor");
        }
    }

    private Tutor buscarTutor(UUID id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor nao encontrado com ID: " + id));
    }

    private Veterinario buscarVeterinario(UUID id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Veterinario nao encontrado com ID: " + id));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        Tutor tutor = usuario.getTutor();
        Veterinario veterinario = usuario.getVeterinario();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                tutor != null ? tutor.getId() : null,
                tutor != null ? tutor.getNome() : null,
                veterinario != null ? veterinario.getId() : null,
                veterinario != null ? veterinario.getNome() : null);
    }
}
