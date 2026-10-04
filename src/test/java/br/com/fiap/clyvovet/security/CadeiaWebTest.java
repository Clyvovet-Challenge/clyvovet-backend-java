package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A cadeia de seguranca das telas: sessao, CSRF, login por formulario e
 * acesso por perfil (ADR 002).
 *
 * <p>O par deste teste e o {@link CadeiaDaApiTest}: la, a API continua sem
 * sessao e sem CSRF; aqui, as telas tem os dois.</p>
 */
class CadeiaWebTest extends TesteDeTela {

    /** Usuario proprio: errar a senha de um usuario do seed o aproximaria do bloqueio. */
    private static final String EMAIL = "login.web@teste.com";
    private static final String SENHA = "senhaCorreta123";

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarUsuario() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
        usuarioRepository.save(Usuario.novo(EMAIL, passwordEncoder.encode(SENHA), Perfil.TUTOR));
    }

    @Test
    @DisplayName("tela protegida sem sessao redireciona para o login")
    void telaSemSessaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/tutor"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("a pagina de login e publica e traz o token CSRF no formulario")
    void paginaDeLoginEPublica() throws Exception {
        String html = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("name=\"_csrf\"", "name=\"email\"", "name=\"senha\"");
    }

    @Test
    @DisplayName("formulario enviado sem token CSRF e recusado com 403")
    void formularioSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/login").param("email", EMAIL).param("senha", SENHA))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("login com a senha certa abre a sessao e leva para a pagina inicial")
    void loginComSenhaCertaAbreSessao() throws Exception {
        enviarFormulario("/login", "email", EMAIL, "senha", SENHA)
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(EMAIL));
    }

    @Test
    @DisplayName("login com senha errada volta ao formulario e conta a tentativa")
    void loginComSenhaErradaContaTentativa() throws Exception {
        enviarFormulario("/login", "email", EMAIL, "senha", "senhaErrada999")
                .andExpect(redirectedUrl("/login?erro"))
                .andExpect(unauthenticated());

        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getTentativasFalhas()).isEqualTo(1);
    }

    @Test
    @DisplayName("depois da senha errada, o e-mail digitado volta preenchido no formulario")
    void senhaErradaMantemOEmailPreenchido() throws Exception {
        MockHttpSession sessao = (MockHttpSession) enviarFormulario("/login", "email", EMAIL, "senha", "senhaErrada999")
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/login").param("erro", "").session(sessao))
                .andExpect(content().string(containsString("value=\"" + EMAIL + "\"")));
    }

    @Test
    @DisplayName("login bem-sucedido pelo formulario zera as falhas acumuladas")
    void loginBemSucedidoZeraFalhas() throws Exception {
        Usuario comFalhas = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        comFalhas.setTentativasFalhas(2);
        usuarioRepository.save(comFalhas);

        enviarFormulario("/login", "email", EMAIL, "senha", SENHA)
                .andExpect(authenticated());

        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getTentativasFalhas()).isZero();
    }

    @Test
    @DisplayName("tutor que abre a area do veterinario recebe 403")
    void tutorNaAreaDoVeterinarioRecebe403() throws Exception {
        abrir("/veterinario", LUCAS).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a pagina inicial leva cada perfil para a propria area")
    void raizLevaCadaPerfilParaSuaArea() throws Exception {
        abrir("/", LUCAS).andExpect(redirectedUrl("/tutor"));
        abrir("/", VETERINARIA).andExpect(redirectedUrl("/veterinario"));
    }

    @Test
    @DisplayName("o cabecalho da tela mostra quem esta logado e o botao de sair")
    void cabecalhoMostraUsuarioEBotaoSair() throws Exception {
        abrir("/tutor", LUCAS)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LUCAS)))
                .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @Test
    @DisplayName("sair encerra a sessao e volta ao login")
    void sairEncerraASessao() throws Exception {
        enviarFormulario("/logout", comoUsuario(LUCAS))
                .andExpect(redirectedUrl("/login?saiu"))
                .andExpect(unauthenticated());
    }
}
