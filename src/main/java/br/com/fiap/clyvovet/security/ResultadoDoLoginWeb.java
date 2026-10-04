package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * O que acontece depois do login pelo formulario das telas.
 *
 * <p>Existe para que o formulario respeite o MESMO bloqueio por conta que o
 * {@code POST /auth/login} da API: 5 senhas erradas e a conta trava. Sem isto,
 * o bloqueio valeria so para o app, e a tela de login viraria a porta dos fundos
 * para tentar senhas a vontade.</p>
 *
 * <p>So a senha errada conta como falha ({@link BadCredentialsException} de um
 * e-mail que existe). Conta bloqueada ou inativa e recusada antes da senha ser
 * conferida e nao soma -- igual ao {@code AuthService}. Para quem tenta entrar,
 * todos os casos mostram a mesma mensagem: detalhar o motivo diria a um atacante
 * quais e-mails existem.</p>
 */
@Component
@RequiredArgsConstructor
public class ResultadoDoLoginWeb implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final ControleTentativasLogin controleTentativas;
    private final UsuarioRepository usuarioRepository;

    /** Volta para a tela que pediu o login, ou para "/" se o login foi direto. */
    private final AuthenticationSuccessHandler destino = new SavedRequestAwareAuthenticationSuccessHandler();

    private final AuthenticationFailureHandler voltaAoFormulario =
            new SimpleUrlAuthenticationFailureHandler("/login?erro");

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof UsuarioAutenticado usuario) {
            controleTentativas.registrarSucesso(usuario.getUsuario());
        }
        destino.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof BadCredentialsException) {
            usuarioRepository.findByEmail(request.getParameter("email"))
                    .ifPresent(controleTentativas::registrarFalha);
        }
        voltaAoFormulario.onAuthenticationFailure(request, response, exception);
    }
}
