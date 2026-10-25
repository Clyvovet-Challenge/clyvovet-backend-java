package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O numero de valores que a infraestrutura acrescenta ao X-Forwarded-For e
 * configuravel. Se a Render acrescentar dois (o IP do cliente e o de um
 * balanceador dela), o cliente e o penultimo valor, e o ultimo e igual para
 * todo mundo.
 */
@TestPropertySource(properties = {
        "clyvovet.rate-limit.enabled=true",
        "clyvovet.rate-limit.proxies-confiaveis=2"
})
class RateLimitProxiesTest extends TesteDeApi {

    private static final String LOGIN_INVALIDO = """
            {"email":"ninguem-com-esse-email@exemplo.com","senha":"senha-errada-123"}""";

    @Test
    @DisplayName("com 2 valores confiaveis, o cliente e o penultimo e o balanceador nao entra na chave")
    void clienteEOPenultimoValor() throws Exception {
        for (int tentativa = 1; tentativa <= 10; tentativa++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "10.0.0." + tentativa + ", 203.0.113.70, 192.0.2.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        // Mesmo cliente (.70), outro primeiro valor forjado: o balde e o mesmo.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.0.0.99, 203.0.113.70, 192.0.2.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isTooManyRequests());

        // Outro cliente atras do MESMO balanceador (192.0.2.1): balde proprio.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.0.0.1, 203.0.113.71, 192.0.2.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_INVALIDO))
                .andExpect(status().isUnauthorized());
    }
}
