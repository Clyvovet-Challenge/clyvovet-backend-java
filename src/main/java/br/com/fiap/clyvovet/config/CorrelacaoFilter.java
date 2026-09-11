package br.com.fiap.clyvovet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Da um identificador a cada requisicao, para que um erro visto na tela possa
 * ser encontrado no log.
 *
 * <p>Sem isto, "deu erro ao salvar" e uma frase sem endereco: quem opera precisa
 * adivinhar qual das centenas de linhas do log corresponde aquela tentativa. Com
 * o identificador, o usuario le a referencia na tela e quem investiga procura
 * exatamente por ela.</p>
 *
 * <p>A API .NET ja fazia isso desde o CorrelationIdMiddleware, e o aplicativo ja
 * envia o header quando o tem. As duas APIs conversam com o mesmo aplicativo e
 * gravam no mesmo banco; ter rastreabilidade em uma so era metade do caminho.</p>
 *
 * <p>O id vindo do cliente e aceito de proposito: e assim que uma mesma acao do
 * tutor -- que toca a Java e a .NET -- aparece com o mesmo id nos dois logs.
 * Ele e saneado antes de entrar no MDC: valor vindo de fora que vai parar em
 * linha de log pode injetar quebra de linha e forjar um registro inteiro.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacaoFilter extends OncePerRequestFilter {

    /** O mesmo nome usado pela API .NET, para o id atravessar as duas. */
    public static final String HEADER = "X-Correlation-Id";

    /** A chave que o padrao de log le. */
    public static final String CHAVE_MDC = "correlacao";

    /**
     * Teto de tamanho do id aceito de fora. Um UUID tem 36; o dobro cobre
     * formatos de trace de terceiros sem deixar um header gigante entrar no log.
     */
    private static final int TAMANHO_MAXIMO = 72;

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain corrente) throws ServletException, IOException {
        String id = sanear(requisicao.getHeader(HEADER));
        if (!StringUtils.hasText(id)) {
            id = UUID.randomUUID().toString();
        }

        MDC.put(CHAVE_MDC, id);
        // Devolvido sempre, e nao so no erro: o aplicativo guarda o id da
        // requisicao bem-sucedida tambem, e e isso que permite reconstruir a
        // sequencia de acoes que levou ate a falha.
        resposta.setHeader(HEADER, id);

        try {
            corrente.doFilter(requisicao, resposta);
        } finally {
            // O container reaproveita a thread. Sem esta limpeza, a proxima
            // requisicao herdaria o id da anterior -- pior que nao ter id, porque
            // aponta para a investigacao errada.
            MDC.remove(CHAVE_MDC);
        }
    }

    /** O id que esta valendo agora, ou {@code null} fora de uma requisicao. */
    public static String atual() {
        return MDC.get(CHAVE_MDC);
    }

    /**
     * So letras, digitos, hifen e underline passam.
     *
     * <p>Um header e texto que o cliente escolhe. Se ele fosse direto para o
     * MDC, um valor com {@code \n} escreveria uma linha de log inteira por
     * conta propria -- e um log forjado e pior do que log nenhum, porque
     * engana quem investiga.</p>
     */
    private static String sanear(String bruto) {
        if (!StringUtils.hasText(bruto)) {
            return null;
        }
        String limpo = bruto.trim();
        if (limpo.length() > TAMANHO_MAXIMO) {
            limpo = limpo.substring(0, TAMANHO_MAXIMO);
        }
        return limpo.matches("[A-Za-z0-9_-]+") ? limpo : null;
    }
}
