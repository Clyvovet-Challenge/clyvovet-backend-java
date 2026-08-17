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
 * tratado em outro lugar: o bloqueio por conta do ControleTentativasLogin
 * (5 falhas seguidas = 15 min bloqueado).
 *
 * A divisao importa. Um limite curto por IP no login (algo como 5 a cada 15 min)
 * parece mais seguro, mas nao e: contra forca bruta ele agrega pouco, ja que o
 * bloqueio por conta ja barra o ataque, e quebra uso legitimo — uma clinica
 * inteira atras de um mesmo IP publico compartilha o balde e trava depois de
 * poucos logins. Quem limita tentativa por conta e o lockout; aqui so se contem
 * volume.
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

    private static final Duration JANELA = Duration.ofMinutes(1);

    /**
     * Faixas de limite, cada uma sabendo a propria capacidade e como se
     * reconhecer numa requisicao.
     *
     * Antes isso vivia em dois metodos que precisavam concordar entre si: um
     * devolvia a categoria como String, o outro traduzia a mesma String em
     * limite, num switch com "default". Uma faixa nova exigia acertar os dois,
     * e um erro de digitacao caia silenciosamente no limite geral.
     */
    private enum Faixa {

        LOGIN(10),
        AUTH(30),
        GERAL(100);

        private final int capacidade;

        Faixa(int capacidade) {
            this.capacidade = capacidade;
        }

        static Faixa de(HttpServletRequest request) {
            String path = request.getRequestURI();
            if ("POST".equals(request.getMethod()) && "/auth/login".equals(path)) {
                return LOGIN;
            }
            return path.startsWith("/auth") ? AUTH : GERAL;
        }

        Bucket novoBucket() {
            return Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(capacidade)
                            .refillIntervally(capacidade, JANELA)
                            .build())
                    .build();
        }
    }

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

        Faixa faixa = Faixa.de(request);
        Bucket bucket = buckets.get(chave(faixa, request), k -> faixa.novoBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        recusarPorExcessoDeRequisicoes(response, probe);
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

    private void recusarPorExcessoDeRequisicoes(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long esperaSegundos = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(esperaSegundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErroValidacao("requisicoes",
                "Muitas requisicoes. Tente novamente em " + esperaSegundos + " segundos."));
    }

    /** Um balde por faixa e por origem: estourar o login nao derruba o resto da API. */
    private String chave(Faixa faixa, HttpServletRequest request) {
        return faixa.name() + ":" + ipDoCliente(request);
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
