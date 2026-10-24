package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.config.WebConfig;
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
import jakarta.servlet.http.HttpServletRequestWrapper;
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
import java.util.Collections;

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

    private static final Duration UM_MINUTO = Duration.ofMinutes(1);
    private static final Duration UMA_HORA = Duration.ofHours(1);

    /**
     * Faixas de limite, cada uma sabendo a propria capacidade, a propria janela e
     * como se reconhecer numa requisicao.
     *
     * Antes isso vivia em dois metodos que precisavam concordar entre si: um
     * devolvia a categoria como String, o outro traduzia a mesma String em
     * limite, num switch com "default". Uma faixa nova exigia acertar os dois,
     * e um erro de digitacao caia silenciosamente no limite geral.
     */
    private enum Faixa {

        /** Login pela API e pela tela no MESMO balde: limitar so um deixaria o outro como atalho. */
        LOGIN(10, UM_MINUTO),
        /**
         * Criar conta, pela tela e pela API, no MESMO balde. Janela de uma hora: o
         * abuso aqui nao e rajada, e volume lento -- 100 contas por minuto da
         * faixa geral enchem a tabela de tutores em horas. Dez por hora por
         * endereco deixa folga para uma sala ou clinica atras de um IP so.
         */
        CADASTRO(10, UMA_HORA),
        AUTH(30, UM_MINUTO),
        GERAL(100, UM_MINUTO);

        private final int capacidade;
        private final Duration janela;

        Faixa(int capacidade, Duration janela) {
            this.capacidade = capacidade;
            this.janela = janela;
        }

        private static final String ROTA_AUTH = WebConfig.PREFIXO_API + "/auth";
        private static final String ROTA_LOGIN = ROTA_AUTH + "/login";
        private static final String ROTA_LOGIN_NA_TELA = "/login";
        private static final String ROTA_REGISTRAR_NA_API = ROTA_AUTH + "/registrar";
        private static final String ROTA_CADASTRO_NA_TELA = "/cadastro";

        static Faixa de(HttpServletRequest request) {
            // Os caminhos saem da mesma constante que o WebConfig usa para
            // prefixar os controllers. Ja estiveram escritos a mao aqui, e
            // quando o prefixo /api/v1 entrou eles pararam de casar em
            // silencio: o login caiu na faixa GERAL, com 100 tentativas por
            // minuto em vez de 10. Nada quebrou, nada apareceu no log -- so a
            // protecao contra forca bruta havia sumido.
            String path = request.getRequestURI();
            boolean ehPost = "POST".equals(request.getMethod());
            if (ehPost && (ROTA_LOGIN.equals(path) || ROTA_LOGIN_NA_TELA.equals(path))) {
                return LOGIN;
            }
            // So o POST conta: abrir a tela de cadastro nao cria conta nenhuma.
            if (ehPost && (ROTA_REGISTRAR_NA_API.equals(path) || ROTA_CADASTRO_NA_TELA.equals(path))) {
                return CADASTRO;
            }
            return path.startsWith(ROTA_AUTH) ? AUTH : GERAL;
        }

        Bucket novoBucket() {
            return Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(capacidade)
                            .refillIntervally(capacidade, janela)
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

    // Maior que a maior janela (uma hora): se o balde expirasse antes de
    // reabastecer, quem ficasse ocioso por meia hora ganharia um balde cheio e o
    // limite por hora valeria o dobro.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
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

        recusarPorExcessoDeRequisicoes(request, response, probe);
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

    private void recusarPorExcessoDeRequisicoes(HttpServletRequest request, HttpServletResponse response,
                                                ConsumptionProbe probe) throws IOException {
        long esperaSegundos = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        response.setHeader("Retry-After", String.valueOf(esperaSegundos));
        if (!request.getRequestURI().startsWith(WebConfig.PREFIXO_API)) {
            // Rota de tela: JSON cru nao serve a quem esta num navegador. O sendError
            // entrega a error.html, que explica e oferece voltar.
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
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
     * Quantos valores, contados da DIREITA do X-Forwarded-For, a infraestrutura
     * acrescentou. O IP do cliente e o primeiro deles.
     *
     * <p>Cada proxy acrescenta ao FIM da lista o endereco de quem se conectou a
     * ele; o que vem antes foi escrito por quem mandou a requisicao. Ler o
     * primeiro valor, como este filtro fazia, deixava o cliente escolher a
     * propria chave: medido na Render, 14 logins com um cabecalho diferente
     * cada passaram sem um unico 429. Com 1, o cliente e o ultimo valor; com 2
     * (o cliente mais um balanceador da plataforma), o penultimo.</p>
     */
    @Value("${clyvovet.rate-limit.proxies-confiaveis:1}")
    private int proxiesConfiaveis;

    /**
     * Sem o cabecalho (execucao local, sem proxy), vale o endereco da conexao.
     *
     * <p>O cabecalho e lido da requisicao ORIGINAL, e nao da que chega ao filtro:
     * com {@code server.forward-headers-strategy=framework}, o
     * ForwardedHeaderFilter do Spring esconde o X-Forwarded-For e entrega em
     * {@code getRemoteAddr()} o PRIMEIRO valor da lista -- justamente o que o
     * cliente escreve. A estrategia continua ligada porque o redirect do OAuth2
     * precisa de X-Forwarded-Proto e -Host; so a escolha do IP e feita aqui.</p>
     *
     * <p>Se a lista tiver menos valores do que o configurado, o cabecalho nao veio
     * da cadeia esperada; usa-se o primeiro, que e o melhor palpite e nao junta
     * todo mundo num balde so. Varias linhas do mesmo cabecalho valem como uma
     * lista: mandar duas linhas nao desloca a posicao.</p>
     */
    private String ipDoCliente(HttpServletRequest request) {
        String forwarded = String.join(",",
                Collections.list(semEnvoltorios(request).getHeaders("X-Forwarded-For")));
        if (forwarded.isBlank()) {
            return semPorta(request.getRemoteAddr());
        }
        String[] valores = forwarded.split(",");
        int posicao = Math.max(valores.length - Math.max(proxiesConfiaveis, 1), 0);
        return semPorta(valores[posicao].trim());
    }

    /** Desce os envoltorios (Spring, Security) ate a requisicao que o container entregou. */
    private static HttpServletRequest semEnvoltorios(HttpServletRequest request) {
        HttpServletRequest atual = request;
        while (atual instanceof HttpServletRequestWrapper envoltorio
                && envoltorio.getRequest() instanceof HttpServletRequest interno) {
            atual = interno;
        }
        return atual;
    }

    /**
     * O proxy do App Service anexa "ip:porta" e a porta de origem muda a cada
     * conexao: com ela na chave, cada tentativa cairia num balde novo. Aceita
     * "1.2.3.4:5678" e "[::1]:5678"; IPv6 sem colchetes fica como veio.
     */
    private static String semPorta(String enderecoComPossivelPorta) {
        if (enderecoComPossivelPorta.startsWith("[")) {
            int fim = enderecoComPossivelPorta.indexOf(']');
            return fim > 0 ? enderecoComPossivelPorta.substring(1, fim) : enderecoComPossivelPorta;
        }
        int doisPontos = enderecoComPossivelPorta.indexOf(':');
        boolean ipv4ComPorta = doisPontos > 0 && doisPontos == enderecoComPossivelPorta.lastIndexOf(':');
        return ipv4ComPorta ? enderecoComPossivelPorta.substring(0, doisPontos) : enderecoComPossivelPorta;
    }
}
