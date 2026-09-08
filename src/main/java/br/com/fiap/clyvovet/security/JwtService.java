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

    /**
     * Identidade do tutor, para a API .NET.
     *
     * O subject deste token e o id do USUARIO. A API .NET precisa do id do TUTOR
     * para recortar lembretes e sugestoes pelo dono, e ela nao mapeia
     * t_clyvo_usuario -- sem esta claim ela teria que passar a ler uma tabela que
     * hoje nao conhece, so para traduzir um id no outro.
     *
     * NAO E TODO TOKEN QUE A CARREGA, E ISSO E O PONTO
     * Ela entra apenas no access token, de 15 minutos. O refresh dura 7 dias e
     * fica guardado em disco no aparelho: se ele tambem carregasse identidade
     * utilizavel, viraria uma credencial de sete dias aceita pela .NET -- e a
     * revogacao por jti que esta API mantem no logout nao alcanca a outra
     * aplicacao. O refresh serve para uma coisa so, que e obter um access novo.
     *
     * ADMIN e VETERINARIO nao tem tutor, e para eles a claim simplesmente nao
     * aparece. Quem consome precisa tratar a ausencia como "nao ha tutor", e
     * nunca como "sem filtro".
     */
    private static final String CLAIM_TUTOR_ID = "tutorId";

    /**
     * Emissor e publico. Sao aditivos e ninguem nesta API os exige hoje --
     * existem porque do outro lado eles sao exigidos por padrao: o
     * TokenValidationParameters do .NET valida issuer e audience a menos que se
     * desligue explicitamente. Emitir os dois deixa a validacao la mais forte em
     * vez de mais fraca.
     */
    private static final String EMISSOR = "clyvovet-api-java";
    private static final String PUBLICO = "clyvovet";

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
        return gerar(usuario, TIPO_ACCESS, validadeAccess, true);
    }

    public String gerarRefreshToken(Usuario usuario) {
        return gerar(usuario, TIPO_REFRESH, validadeRefresh, false);
    }

    /**
     * @param comIdentidade se a claim {@code tutorId} entra. Verdadeiro so para o
     *                      access token -- ver a justificativa em CLAIM_TUTOR_ID.
     */
    private String gerar(Usuario usuario, String tipo, Duration validade, boolean comIdentidade) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(EMISSOR)
                .audience().add(PUBLICO).and()
                .subject(usuario.getId().toString())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)));

        // getTutor() e EAGER (Usuario.java), entao nao ha risco de
        // LazyInitializationException aqui mesmo fora de transacao.
        if (comIdentidade && usuario.getTutor() != null) {
            builder.claim(CLAIM_TUTOR_ID, usuario.getTutor().getId().toString());
        }

        return builder.signWith(chave).compact();
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

    /**
     * O id do tutor, ou {@code null} quando o token nao o carrega -- o que
     * acontece com refresh token, com ADMIN e com VETERINARIO.
     *
     * Ausencia significa "este token nao identifica um tutor". Nao significa
     * "sem restricao": quem for recortar dados por dono precisa NEGAR quando vier
     * null, nunca devolver tudo.
     */
    public UUID extrairTutorId(Claims claims) {
        String valor = claims.get(CLAIM_TUTOR_ID, String.class);
        return valor == null ? null : UUID.fromString(valor);
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
