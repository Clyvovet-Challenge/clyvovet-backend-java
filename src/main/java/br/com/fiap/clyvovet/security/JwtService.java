package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Geracao e validacao dos tokens JWT.
 *
 * Dois tipos de token, diferenciados pela claim "tipo":
 *   access  — curto (15 min), autoriza as chamadas da API;
 *   refresh — longo (7 dias), so serve para obter um novo access.
 *
 * A separacao limita a janela de uso de um access token vazado sem obrigar
 * o usuario a refazer login a cada 15 minutos.
 */
@Service
public class JwtService {

    private static final String CLAIM_PERFIL = "perfil";
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final Duration validadeAccess;
    private final Duration validadeRefresh;

    public JwtService(
            @Value("${clyvovet.jwt.secret}") String segredo,
            @Value("${clyvovet.jwt.access-token-minutos:15}") long accessMinutos,
            @Value("${clyvovet.jwt.refresh-token-dias:7}") long refreshDias) {
        // HMAC-SHA256 exige no minimo 256 bits de chave; Keys.hmacShaKeyFor
        // rejeita segredos menores, o que evita subir com uma chave fraca.
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredo));
        this.validadeAccess = Duration.ofMinutes(accessMinutos);
        this.validadeRefresh = Duration.ofDays(refreshDias);
    }

    public String gerarAccessToken(Usuario usuario) {
        return gerar(usuario, TIPO_ACCESS, validadeAccess);
    }

    public String gerarRefreshToken(Usuario usuario) {
        return gerar(usuario, TIPO_REFRESH, validadeRefresh);
    }

    private String gerar(Usuario usuario, String tipo, Duration validade) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(usuario.getId().toString())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    /**
     * Devolve as claims se o token for valido, ou lanca JwtException.
     * A verificacao de assinatura e de expiracao e feita pelo parser.
     */
    public Claims lerClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extrairUsuarioId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extrairJti(Claims claims) {
        return claims.getId();
    }

    public boolean ehAccessToken(Claims claims) {
        return TIPO_ACCESS.equals(claims.get(CLAIM_TIPO, String.class));
    }

    public boolean ehRefreshToken(Claims claims) {
        return TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class));
    }

    public boolean tokenValido(String token) {
        try {
            lerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getValidadeAccessSegundos() {
        return validadeAccess.toSeconds();
    }
}
