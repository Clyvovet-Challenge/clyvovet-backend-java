package br.com.fiap.clyvovet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Le o header Authorization, valida o access token e popula o SecurityContext.
 *
 * Nao decide sobre autorizacao nem devolve erro: token ausente ou invalido
 * simplesmente deixa o contexto vazio, e a cadeia do Spring Security responde
 * 401 ou 403 conforme a rota. Isso mantem uma unica fonte de decisao.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.lerClaims(token);

            // Refresh token nao autentica chamadas da API — so serve em /auth/refresh.
            if (!jwtService.ehAccessToken(claims)) {
                return;
            }

            UsuarioAutenticado usuario = usuarioDetailsService.carregarPorId(jwtService.extrairUsuarioId(claims));
            if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token invalido, expirado ou de usuario removido: segue sem autenticar.
            SecurityContextHolder.clearContext();
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIXO)) {
            return header.substring(PREFIXO.length()).trim();
        }
        return null;
    }
}
