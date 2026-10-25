package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato da cadeia de seguranca da API, fixado antes de ela dividir o
 * filtro com as telas Thymeleaf (ADR 002).
 *
 * <p>As telas vao precisar de sessao, CSRF e redirecionamento para o login --
 * exatamente o contrario do que o app mobile espera. Cada teste aqui e uma
 * promessa da API que a cadeia web nao pode vazar para dentro de /api/**: se
 * um deles quebrar depois da divisao, foi a configuracao da web que invadiu a
 * da API.</p>
 */
class CadeiaDaApiTest extends TesteDeApi {

    private static final String ORIGEM_PERMITIDA = "http://localhost:3000";

    @Test
    @DisplayName("sem token, a API responde 401 em JSON, e nao redireciona para tela de login")
    void semTokenResponde401EmJson() throws Exception {
        ResultActions resposta = mockMvc.perform(get("/api/v1/animais"))
                .andExpect(status().isUnauthorized());

        assertThat(resposta.andReturn().getResponse().getContentType())
                .startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(corpoDe(resposta).get("campo").asText()).isEqualTo("autenticacao");
    }

    @Test
    @DisplayName("perfil sem permissao recebe 403 em JSON, e nao uma pagina de erro")
    void semPermissaoResponde403EmJson() throws Exception {
        ResultActions resposta = buscar("/api/v1/tutores", tokenTutor(LUCAS))
                .andExpect(status().isForbidden());

        assertThat(resposta.andReturn().getResponse().getContentType())
                .startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(corpoDe(resposta).get("campo").asText()).isEqualTo("autorizacao");
    }

    @Test
    @DisplayName("login e chamada autenticada nao abrem sessao nem devolvem cookie")
    void apiNaoCriaSessao() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"tutor12345\"}".formatted(LUCAS)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
        MvcResult chamada = buscar("/api/v1/animais", token).andExpect(status().isOk()).andReturn();

        for (MvcResult resultado : new MvcResult[] {login, chamada}) {
            assertThat(resultado.getRequest().getSession(false)).isNull();
            assertThat(resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .noneMatch(cookie -> cookie.startsWith("JSESSIONID"));
        }
    }

    @Test
    @DisplayName("escrita na API com token dispensa token CSRF")
    void escritaComTokenDispensaCsrf() throws Exception {
        // Status de validacao, e nao 403: o pedido chegou ao controller, ou seja,
        // nenhum filtro de CSRF o barrou no caminho.
        criar("/api/v1/animais", tokenTutor(LUCAS), "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("respostas da API levam os cabecalhos de seguranca")
    void respostasLevamCabecalhosDeSeguranca() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(get("/api/v1/animais").secure(true))
                .andReturn().getResponse();

        assertThat(resposta.getHeader("Content-Security-Policy"))
                .isEqualTo("default-src 'self'; frame-ancestors 'none'");
        assertThat(resposta.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000 ; includeSubDomains");
        assertThat(resposta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(resposta.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("preflight CORS de origem permitida libera PATCH e o X-Correlation-Id")
    void preflightCorsDeOrigemPermitida() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(options("/api/v1/clinicas/qualquer")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, X-Correlation-Id"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(resposta.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ORIGEM_PERMITIDA);
        assertThat(resposta.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("PATCH");
        assertThat(resposta.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).containsIgnoringCase("X-Correlation-Id");
    }

    @Test
    @DisplayName("preflight CORS de origem desconhecida e recusado")
    void preflightCorsDeOrigemDesconhecida() throws Exception {
        mockMvc.perform(options("/api/v1/animais")
                        .header(HttpHeaders.ORIGIN, "https://site-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("health continua publico e em JSON")
    void healthContinuaPublico() throws Exception {
        ResultActions resposta = mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        JsonNode corpo = corpoDe(resposta);
        assertThat(corpo.get("status").asText()).isEqualTo("UP");
    }
}
