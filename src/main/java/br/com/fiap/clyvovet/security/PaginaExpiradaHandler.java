package br.com.fiap.clyvovet.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Troca o 403 cru por um aviso quando o token CSRF de um formulario PUBLICO
 * nao confere.
 *
 * <p>Isso acontece sem culpa de ninguem: a pessoa deixou o /login aberto, o
 * cookie do token expirou ou o app foi trocado. O formulario nao faz nada
 * (nenhum login, nenhuma conta criada) -- so volta ao login pedindo para tentar
 * de novo. Qualquer outra recusa, inclusive CSRF em tela de quem ja entrou,
 * segue como 403.</p>
 */
@Component
public class PaginaExpiradaHandler implements AccessDeniedHandler {

    private static final Set<String> FORMULARIOS_PUBLICOS = Set.of("/login", "/cadastro");

    private final AccessDeniedHandler padrao = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excecao) throws IOException, jakarta.servlet.ServletException {
        if (excecao instanceof CsrfException && FORMULARIOS_PUBLICOS.contains(caminho(request))) {
            response.sendRedirect(request.getContextPath() + "/login?expirou");
            return;
        }
        padrao.handle(request, response, excecao);
    }

    private static String caminho(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
