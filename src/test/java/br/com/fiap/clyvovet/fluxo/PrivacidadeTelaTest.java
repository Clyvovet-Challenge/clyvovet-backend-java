package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * A politica de privacidade e publica: o Google so deixa tirar o login social do
 * modo de teste se a pagina abrir para qualquer pessoa, sem conta.
 *
 * <p>O que estes testes fixam e o contrato com quem le: a pagina abre sem login,
 * diz com quem falar e quais terceiros recebem dados, e e alcancavel pelas telas
 * onde a pessoa entrega os dados (login e cadastro).</p>
 */
class PrivacidadeTelaTest extends TesteDeTela {

    @Test
    @DisplayName("a politica de privacidade abre sem login")
    void abreSemLogin() throws Exception {
        mockMvc.perform(get("/privacidade"))
                .andExpect(status().isOk())
                .andExpect(view().name("privacidade"));
    }

    @Test
    @DisplayName("diz quem e o responsavel, a lei e com quem os dados sao compartilhados")
    void dizOQuePrecisaDizer() throws Exception {
        mockMvc.perform(get("/privacidade"))
                .andExpect(content().string(containsString("leojp04@gmail.com")))
                .andExpect(content().string(containsString("LGPD")))
                .andExpect(content().string(containsString("Google")))
                .andExpect(content().string(containsString("GitHub")))
                .andExpect(content().string(containsString("Render")))
                .andExpect(content().string(containsString("Aiven")));
    }

    @Test
    @DisplayName("a tela de login leva a politica de privacidade")
    void loginTemLinkParaPrivacidade() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("href=\"/privacidade\"")));
    }

    @Test
    @DisplayName("a tela de criar conta leva a politica de privacidade")
    void cadastroTemLinkParaPrivacidade() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(content().string(containsString("href=\"/privacidade\"")));
    }
}
