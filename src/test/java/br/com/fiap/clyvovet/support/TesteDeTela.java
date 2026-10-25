package br.com.fiap.clyvovet.support;

import br.com.fiap.clyvovet.security.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base dos testes que exercitam as telas Thymeleaf pela cadeia web.
 *
 * <p>Estende o {@link TesteDeApi} para herdar os e-mails do seed, o MockMvc e a
 * limpeza entre testes: um fluxo de tela as vezes precisa conferir pela API o que
 * o formulario gravou.</p>
 *
 * <p>A sessao e montada com o {@code user(...)} do spring-security-test, mas com o
 * MESMO principal que o login de verdade produz -- um {@code UsuarioAutenticado}
 * carregado do banco. Um usuario "de mentira" com so o perfil passaria nas regras
 * de rota e quebraria no {@code @seguranca}, que precisa do tutor e da clinica.</p>
 */
public abstract class TesteDeTela extends TesteDeApi {

    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    protected RequestPostProcessor comoUsuario(String email) {
        return user(usuarioDetailsService.loadUserByUsername(email));
    }

    protected ResultActions abrir(String url, String email) throws Exception {
        return mockMvc.perform(get(url).with(comoUsuario(email)));
    }

    /**
     * Envia um formulario sem sessao, como a propria tela de login.
     *
     * <p>Sem o {@code anonymous()} do spring-security-test de proposito: ele fixa
     * o contexto como anonimo, e o login de verdade feito por esta requisicao
     * nao conseguiria substitui-lo -- a sessao nasceria e o teste nao a veria.</p>
     */
    protected ResultActions enviarFormulario(String url, String... camposEValores) throws Exception {
        return enviarFormulario(url, requisicao -> requisicao, camposEValores);
    }

    /**
     * Envia um formulario como o navegador enviaria: POST com os campos e o token
     * CSRF que o Thymeleaf coloca em todo {@code <form>}.
     *
     * @param camposEValores pares nome, valor, nome, valor...
     */
    protected ResultActions enviarFormulario(String url, RequestPostProcessor quem,
                                             String... camposEValores) throws Exception {
        MockHttpServletRequestBuilder requisicao = post(url).with(csrf()).with(quem);
        for (int i = 0; i < camposEValores.length; i += 2) {
            requisicao.param(camposEValores[i], camposEValores[i + 1]);
        }
        return mockMvc.perform(requisicao);
    }
}
