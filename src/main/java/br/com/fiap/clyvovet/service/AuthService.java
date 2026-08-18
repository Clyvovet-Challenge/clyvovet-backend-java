package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.auth.LoginRequest;
import br.com.fiap.clyvovet.dto.auth.LoginResponse;
import br.com.fiap.clyvovet.dto.auth.RefreshRequest;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.security.ControleTentativasLogin;
import br.com.fiap.clyvovet.security.JwtService;
import br.com.fiap.clyvovet.security.RevogacaoTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Autenticacao: provar quem e o usuario e emitir tokens.
 *
 * O cadastro de usuarios ficou no {@link UsuarioService}. Sao responsabilidades
 * com motivos de mudanca diferentes — uma muda quando a politica de credencial
 * muda, a outra quando o cadastro ganha campo ou regra de vinculo — e estavam
 * na mesma classe apenas por compartilharem o prefixo /auth na URL.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Mensagem unica para credencial errada, e-mail inexistente e conta bloqueada.
     * Mensagens distintas permitiriam descobrir quais e-mails existem na base
     * apenas observando a resposta (enumeracao de usuarios).
     */
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais invalidas";
    private static final String REFRESH_INVALIDO = "Refresh token invalido ou expirado";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ControleTentativasLogin controleTentativas;
    private final RevogacaoTokenService revogacaoToken;

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

        if (!podeAutenticar(usuario)) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            controleTentativas.registrarFalha(usuario);
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        controleTentativas.registrarSucesso(usuario);
        return montarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        Claims claims = lerClaimsDoRefresh(request.getRefreshToken());

        // Um access token nao pode ser usado para renovar a si mesmo.
        if (!jwtService.ehRefreshToken(claims)) {
            throw new BadCredentialsException("Token informado nao e um refresh token");
        }

        if (revogacaoToken.estaRevogado(jwtService.extrairJti(claims))) {
            throw new BadCredentialsException(REFRESH_INVALIDO);
        }

        Usuario usuario = usuarioRepository.findById(jwtService.extrairUsuarioId(claims))
                .filter(this::podeAutenticar)
                .orElseThrow(() -> new BadCredentialsException(REFRESH_INVALIDO));

        return montarResposta(usuario);
    }

    /**
     * Revoga o refresh token informado. O access token emitido junto continua
     * valido ate expirar sozinho (ate 15 min) — e a mesma janela curta que jah
     * limita o estrago de um access token vazado, descrita no {@link JwtService}.
     */
    public void logout(RefreshRequest request) {
        Claims claims = lerClaimsDoRefresh(request.getRefreshToken());

        if (!jwtService.ehRefreshToken(claims)) {
            throw new BadCredentialsException("Token informado nao e um refresh token");
        }

        revogacaoToken.revogar(jwtService.extrairJti(claims));
    }

    private Claims lerClaimsDoRefresh(String refreshToken) {
        try {
            return jwtService.lerClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException(REFRESH_INVALIDO);
        }
    }

    /** Conta inativa e conta bloqueada barram tanto o login quanto a renovacao. */
    private boolean podeAutenticar(Usuario usuario) {
        return usuario.isAtivo() && !usuario.estaBloqueado();
    }

    private LoginResponse montarResposta(Usuario usuario) {
        return LoginResponse.de(
                jwtService.gerarAccessToken(usuario),
                jwtService.gerarRefreshToken(usuario),
                jwtService.getValidadeAccessSegundos(),
                usuario.getPerfil());
    }
}
