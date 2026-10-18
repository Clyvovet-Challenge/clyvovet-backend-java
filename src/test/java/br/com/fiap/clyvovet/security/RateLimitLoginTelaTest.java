package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O login pela tela ({@code POST /login}) tem o mesmo limite do login da API:
 * 10 por minuto por endereco, no MESMO balde.
 *
 * <p>Regressao: o filtro so reconhecia {@code /api/v1/auth/login}, e o
 * formulario caia na faixa geral (100 por minuto). O bloqueio por conta ainda
 * barrava a forca bruta dirigida, mas a doc prometia 10 e a tela entregava 100.
 * Dividir o balde importa pelo mesmo motivo do cadastro: limitar so a tela
 * deixaria a API como atalho.</p>
 *
 * <p>Os logins sao de um e-mail que nao existe: o filtro conta toda requisicao
 * antes de a autenticacao rodar, e nenhuma conta e bloqueada.</p>
 */
@TestPropertySource(properties = "clyvovet.rate-limit.enabled=true")
class RateLimitLoginTelaTest extends TesteDeTela {

    private static final int LIMITE_POR_MINUTO = 10;

    private ResultActions entrarPelaTela(String ip) throws Exception {
        return mockMvc.perform(post("/login")
                .with(csrf())
                .header("X-Forwarded-For", ip)
                .param("email", "ninguem@exemplo.com")
                .param("senha", "senha-errada-123"));
    }

    private void esgotarPelaTela(String ip) throws Exception {
        for (int i = 0; i < LIMITE_POR_MINUTO; i++) {
            entrarPelaTela(ip).andExpect(status().is3xxRedirection());
        }
    }

    @Test
    @DisplayName("o login pela tela aceita 10 tentativas por minuto do mesmo endereco e recusa a 11a")
    void telaRecusaAOnzeTentativa() throws Exception {
        String ip = "198.51.100.11";
        esgotarPelaTela(ip);

        entrarPelaTela(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("o login da API divide o balde com a tela: esgotar uma fecha a outra")
    void apiDivideOBaldeComATela() throws Exception {
        String ip = "198.51.100.12";
        esgotarPelaTela(ip);

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@exemplo.com\",\"senha\":\"senha-errada-123\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("abrir a tela de login nao gasta o limite: so o envio conta")
    void abrirATelaNaoGastaLimite() throws Exception {
        String ip = "198.51.100.13";
        for (int i = 0; i < 2 * LIMITE_POR_MINUTO; i++) {
            mockMvc.perform(get("/login").header("X-Forwarded-For", ip))
                    .andExpect(status().isOk());
        }

        entrarPelaTela(ip).andExpect(status().is3xxRedirection());
    }
}
