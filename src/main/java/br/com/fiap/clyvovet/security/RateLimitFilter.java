package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting por origem, com Bucket4j.
 *
 * Este filtro cuida do ataque VOLUMETRICO — rajada de requisicoes vinda de um
 * mesmo endereco. O ataque de forca bruta DIRECIONADO a uma conta especifica e
 * tratado em outro lugar: o bloqueio por conta do AuthService (5 falhas seguidas
 * = 15 min bloqueado).
 *
 * A divisao importa. Um limite curto por IP no login (algo como 5 a cada 15 min)
 * parece mais seguro, mas nao e: contra forca bruta ele agrega pouco, ja que o
 * bloqueio por conta ja barra o ataque, e quebra uso legitimo — uma clinica
 * inteira atras de um mesmo IP publico compartilha o balde e trava depois de
 * poucos logins. Quem limita tentativa por conta e o lockout; aqui so se contem
 * volume.
 *
 * Tres faixas:
 *   POST /auth/login  — 10 por IP por minuto;
 *   /auth/**          — 30 por IP por minuto, contra abuso de cadastro;
 *   demais rotas      — 100 por IP por minuto.
 *
 * Os buckets vivem num cache Caffeine com expiracao, e nao num Map comum:
 * sem expiracao, cada IP visto criaria uma entrada permanente e a memoria
 * cresceria sem limite — o proprio rate limiter viraria um vetor de DoS.
 *
 * LIMITACAO CONHECIDA: o estado e local ao processo. Com mais de uma replica,
 * cada uma teria sua propria contagem; nesse cenario o correto seria Bucket4j
 * sobre Redis (bucket4j-redis).
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_LOGIN = 10;
    private static final int LIMITE_AUTH = 30;
    private static final int LIMITE_GERAL = 100;
    private static final Duration JANELA_PADRAO = Duration.ofMinutes(1);

    private final ObjectMapper objectMapper;

    /**
     * Permite desligar o limite nos testes automatizados, que fazem dezenas de
     * chamadas em sequencia a partir do mesmo endereco e bateriam no teto por
     * volume, nao por falha real.
     */
    @Value("${clyvovet.rate-limit.enabled:true}")
    private boolean habilitado;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Bucket bucket = buckets.get(chave(request), k -> criarBucket(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long esperaSegundos = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(esperaSegundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErroValidacao("requisicoes",
                "Muitas requisicoes. Tente novamente em " + esperaSegundos + " segundos."));
    }

    /** Swagger e console H2 ficam fora do limite para nao atrapalhar a avaliacao. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!habilitado) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    private String chave(HttpServletRequest request) {
        return categoria(request) + ":" + ipDoCliente(request);
    }

    private String categoria(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod()) && path.equals("/auth/login")) {
            return "login";
        }
        return path.startsWith("/auth") ? "auth" : "geral";
    }

    private Bucket criarBucket(HttpServletRequest request) {
        return switch (categoria(request)) {
            case "login" -> comLimite(LIMITE_LOGIN, JANELA_PADRAO);
            case "auth" -> comLimite(LIMITE_AUTH, JANELA_PADRAO);
            default -> comLimite(LIMITE_GERAL, JANELA_PADRAO);
        };
    }

    private Bucket comLimite(int capacidade, Duration janela) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacidade)
                        .refillIntervally(capacidade, janela)
                        .build())
                .build();
    }

    /**
     * Considera X-Forwarded-For por causa do proxy da Azure. Em producao isso
     * so e confiavel se o proxy sobrescrever o header — do contrario o cliente
     * poderia forjar o IP e escapar do limite.
     */
    private String ipDoCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
