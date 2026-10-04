package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login das telas com Google e GitHub, visto pela cadeia web (ADR 002).
 *
 * <p>A ida e volta ao provedor de verdade nao roda na suite: precisaria das
 * credenciais e da internet. O que fica coberto aqui e cada ponta dela -- o botao,
 * o redirecionamento com os escopos certos, a mensagem de recusa e a sessao de
 * quem voltou. A decisao de quem entra esta no {@link OAuth2UsuarioServiceTest}.</p>
 */
class LoginOAuth2Test extends TesteDeTela {

    @Autowired UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("a pagina de login oferece entrar com Google e com GitHub")
    void loginOfereceGoogleEGithub() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/oauth2/authorization/google\"",
                                  "href=\"/oauth2/authorization/github\"");
    }

    @Test
    @DisplayName("o botao do Google leva ao Google pedindo o e-mail")
    void botaoDoGoogleLevaAoGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://accounts.google.com/")))
                .andExpect(header().string("Location", containsString("scope=openid%20email%20profile")));
    }

    @Test
    @DisplayName("o botao do GitHub leva ao GitHub pedindo a lista de e-mails")
    void botaoDoGithubLevaAoGithub() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://github.com/login/oauth/authorize")))
                .andExpect(header().string("Location", containsString("user:email")));
    }

    @Test
    @DisplayName("quem volta do provedor sem conta aqui ve a mensagem de conta nao encontrada")
    void recusaMostraContaNaoEncontrada() throws Exception {
        mockMvc.perform(get("/login").param("sem-conta", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma conta do ClyvoVet usa este e-mail")));
    }

    @Test
    @DisplayName("quem entrou pelo GitHub usa as telas como quem entrou por senha")
    void usuarioOAuth2UsaAsTelas() throws Exception {
        DefaultOAuth2User contaGithub = new DefaultOAuth2User(List.of(), Map.of("login", "lucas-gh"), "login");
        UsuarioOAuth2 lucas = new UsuarioOAuth2(usuarioRepository.findByEmail(LUCAS).orElseThrow(), contaGithub);

        mockMvc.perform(get("/").with(oauth2Login().oauth2User(lucas)))
                .andExpect(redirectedUrl("/tutor"));
        mockMvc.perform(get("/tutor").with(oauth2Login().oauth2User(lucas)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LUCAS)));
    }
}
