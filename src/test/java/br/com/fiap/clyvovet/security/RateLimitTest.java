package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limite de requisicoes por minuto.
 *
 * O resto da suite roda com clyvovet.rate-limit.enabled=false, porque dezenas
 * de chamadas em sequencia do mesmo endereco bateriam no teto por volume e nao
 * por falha real. Esta classe religa o limite so para si.
 *
 * POR QUE ELA EXISTE: o filtro reconhecia a faixa comparando o caminho com
 * "/auth/login" escrito a mao. Quando o prefixo /api/v1 entrou, a comparacao
 * parou de casar e o login caiu na faixa geral -- 100 tentativas por minuto em
 * vez de 10. Nenhum teste quebrou, nenhum log reclamou: a protecao contra forca
 * bruta simplesmente deixou de existir.
 */
@TestPropertySource(properties = "clyvovet.rate-limit.enabled=true")
class RateLimitTest extends TesteDeApi {

    private static final String LOGIN_INVALIDO = """
            {"email":"ninguem-com-esse-email@exemplo.com","senha":"senha-errada-123"}""";

    @Test
    @DisplayName("login estoura o limite na 11a tentativa do mesmo endereco")
    void loginEstouraOLimiteNaDecimaPrimeira() throws Exception {
        // A faixa LOGIN permite 10 por minuto. Um e-mail inexistente e usado de
        // proposito: nao ha conta real para bloquear, entao o que responde e o
        // rate limit, e nao o controle de tentativas por usuario.
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("o mesmo cliente com a porta de origem mudando continua no mesmo balde")
    void portaDeOrigemNaoMudaOBalde() throws Exception {
        // O proxy do App Service anexa "ip:porta" ao X-Forwarded-For, e a porta
        // de origem muda a cada conexao. Se a chave levasse a porta, cada
        // tentativa cairia num balde novo e o limite de forca bruta nunca
        // dispararia.
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113.7:" + (50000 + tentativa))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.7:60000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Medido na Render: 14 logins, cada um com um X-Forwarded-For diferente,
     * passaram todos sem 429. O proxy so ACRESCENTA o IP de quem se conectou ao
     * fim da lista; o primeiro valor e o que o cliente escreveu. A chave do
     * balde precisa vir do valor que o proxy escreveu, e nao do que o cliente
     * mandou.
     */
    @Test
    @DisplayName("forjar o primeiro valor do X-Forwarded-For nao escapa do limite")
    void primeiroValorForjadoNaoEscapaDoLimite() throws Exception {
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "10.0.0." + tentativa + ", 203.0.113.50")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.0.0.99, 203.0.113.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("clientes diferentes, vistos pelo proxy, continuam com baldes separados")
    void clientesDiferentesNaoDividemOBalde() throws Exception {
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "203.0.113.60")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.61")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isUnauthorized());
    }
}
