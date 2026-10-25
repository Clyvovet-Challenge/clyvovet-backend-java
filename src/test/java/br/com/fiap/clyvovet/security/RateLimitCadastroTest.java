package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limite de criacao de contas por endereco: 10 por hora, na tela e na API.
 *
 * <p>Criar conta e aberto a qualquer um, e cada conta cria um tutor no banco. Sem
 * limite proprio, um script enchia a tabela: a faixa geral deixa 100 por minuto,
 * e a da API de auth, 30. Por isso a tela {@code /cadastro} e a API
 * {@code /auth/registrar} dividem UM balde -- limitar so a tela deixaria a API
 * como atalho.</p>
 *
 * <p>Os formularios daqui sao invalidos de proposito (senhas diferentes): o
 * filtro conta toda requisicao, e assim nenhuma conta de verdade e criada. Cada
 * teste usa um endereco proprio no {@code X-Forwarded-For}, porque o balde mora
 * no filtro, que e unico, e os testes dividiriam o mesmo.</p>
 */
@TestPropertySource(properties = "clyvovet.rate-limit.enabled=true")
class RateLimitCadastroTest extends TesteDeTela {

    private static final int LIMITE_POR_HORA = 10;

    private ResultActions enviarCadastroInvalido(String ip) throws Exception {
        return mockMvc.perform(post("/cadastro")
                .with(csrf())
                .header("X-Forwarded-For", ip)
                .param("nome", "Pessoa Teste")
                .param("email", "pessoa@exemplo.com")
                .param("senha", "Senha-Forte-2026")
                .param("confirmacaoDaSenha", "outra-senha-qualquer"));
    }

    private void esgotar(String ip) throws Exception {
        for (int i = 0; i < LIMITE_POR_HORA; i++) {
            enviarCadastroInvalido(ip).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("a tela de cadastro aceita 10 envios por hora do mesmo endereco e recusa o 11o")
    void telaRecusaOOnzeEnvio() throws Exception {
        String ip = "198.51.100.1";
        esgotar(ip);

        enviarCadastroInvalido(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("a API de registro divide o balde com a tela: esgotar uma fecha a outra")
    void apiDivideOBaldeComATela() throws Exception {
        String ip = "198.51.100.2";
        esgotar(ip);

        mockMvc.perform(post("/api/v1/auth/registrar")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(containsString("requisicoes")));
    }

    @Test
    @DisplayName("abrir a tela de cadastro nao gasta o limite: so o envio conta")
    void abrirATelaNaoGastaLimite() throws Exception {
        String ip = "198.51.100.3";
        for (int i = 0; i < 2 * LIMITE_POR_HORA; i++) {
            mockMvc.perform(get("/cadastro").header("X-Forwarded-For", ip))
                    .andExpect(status().isOk());
        }

        enviarCadastroInvalido(ip).andExpect(status().isOk());
    }

    @Test
    @DisplayName("outro endereco tem balde proprio")
    void outroEnderecoTemBaldeProprio() throws Exception {
        esgotar("198.51.100.4");

        enviarCadastroInvalido("198.51.100.5").andExpect(status().isOk());
    }
}
