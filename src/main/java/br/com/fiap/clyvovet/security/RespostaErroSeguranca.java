package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Traduz as falhas do Spring Security para o mesmo formato de erro usado no
 * resto da API (ErroValidacao), em vez da pagina HTML padrao.
 *
 * As mensagens sao deliberadamente genericas: detalhar qual regra barrou a
 * requisicao ajudaria a mapear a superficie de autorizacao.
 */
@Component
@RequiredArgsConstructor
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** 401 — nao autenticado ou token invalido/expirado. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(response, HttpStatus.UNAUTHORIZED, "autenticacao",
                "Autenticacao necessaria. Envie um access token valido no header Authorization.");
    }

    /** 403 — autenticado, mas sem permissao para o recurso. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, "autorizacao",
                "Seu perfil nao tem permissao para acessar este recurso.");
    }

    private void escrever(HttpServletResponse response, HttpStatus status,
                          String campo, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErroValidacao(campo, mensagem));
    }
}
