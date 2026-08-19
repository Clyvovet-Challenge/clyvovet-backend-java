package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida da sessao: /auth/me, /auth/refresh, /auth/logout e a criacao de
 * usuario por ADMIN em /auth/usuarios.
 *
 * Estes quatro endpoints existiam sem nenhum teste. A lacuna nao era teorica: o
 * rate limit do login ficou desprotegido por uma troca de caminho e ninguem
 * percebeu, porque tambem nao havia teste ali. O que nao e verificado aqui so
 * aparece em producao.
 */
class CicloDeSessaoTest extends TesteDeApi {

    private JsonNode login(String email, String senha) throws Exception {
        return corpoDe(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk()));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    // ------------------------------------------------------------------
    // /auth/login — o que o corpo da resposta promete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("login devolve os dois tokens, o tipo e a validade")
    void loginDevolveOContratoCompleto() throws Exception {
        JsonNode corpo = login(LUCAS, "tutor12345");

        assertThat(corpo.get("accessToken").asText()).isNotBlank();
        assertThat(corpo.get("refreshToken").asText()).isNotBlank();
        assertThat(corpo.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(corpo.get("expiraEmSegundos").asLong()).isPositive();

        // Os dois tokens sao distintos: se fossem o mesmo, revogar um no logout
        // derrubaria o outro, e o access nao seria mais stateless.
        assertThat(corpo.get("accessToken").asText())
                .isNotEqualTo(corpo.get("refreshToken").asText());
    }

    // ------------------------------------------------------------------
    // /auth/me
    // ------------------------------------------------------------------

    @Test
    @DisplayName("me devolve o usuario do token, com o vinculo de tutor")
    void meDevolveOUsuarioDoToken() throws Exception {
        JsonNode eu = corpoDe(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenTutor(LUCAS)))
                .andExpect(status().isOk()));

        assertThat(eu.get("email").asText()).isEqualTo(LUCAS);
        assertThat(eu.get("perfil").asText()).isEqualTo("TUTOR");
        assertThat(eu.get("ativo").asBoolean()).isTrue();
        // O vinculo com o dominio e o que viabiliza o ownership.
        assertThat(eu.get("tutorId").isNull()).isFalse();
    }

    @Test
    @DisplayName("me nunca devolve a senha")
    void meNaoVazaSenha() throws Exception {
        JsonNode eu = corpoDe(mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk()));

        assertThat(eu.has("senha")).isFalse();
    }

    @Test
    @DisplayName("me responde 401 sem token e com token invalido")
    void meExigeTokenValido() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer nao.e.um.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("me segue o token, nao o ultimo login")
    void meSegueOToken() throws Exception {
        String tutor = tokenTutor(MARIA);
        String veterinaria = tokenVeterinaria();

        JsonNode comoTutor = corpoDe(mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + tutor)).andExpect(status().isOk()));
        JsonNode comoVet = corpoDe(mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + veterinaria)).andExpect(status().isOk()));

        assertThat(comoTutor.get("email").asText()).isEqualTo(MARIA);
        assertThat(comoVet.get("email").asText()).isEqualTo(VETERINARIA);
    }

    // ------------------------------------------------------------------
    // /auth/refresh
    // ------------------------------------------------------------------

    @Test
    @DisplayName("refresh devolve um access novo que abre rota protegida")
    void refreshDevolveAccessUtilizavel() throws Exception {
        String refresh = login(LUCAS, "tutor12345").get("refreshToken").asText();

        JsonNode renovado = corpoDe(refresh(refresh).andExpect(status().isOk()));
        String accessNovo = renovado.get("accessToken").asText();

        // Nao basta vir um token: ele precisa de fato autorizar.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessNovo))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh recusa token invalido")
    void refreshRecusaTokenInvalido() throws Exception {
        refresh("nao.e.um.jwt").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("access token nao serve como refresh token")
    void accessNaoServeComoRefresh() throws Exception {
        // Os dois sao JWT assinados pela mesma chave; o que os separa e o tipo
        // declarado no proprio token. Se o refresh aceitasse um access, um
        // access vazado viraria sessao renovavel por sete dias.
        String access = login(LUCAS, "tutor12345").get("accessToken").asText();

        refresh(access).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // /auth/logout
    // ------------------------------------------------------------------

    @Test
    @DisplayName("logout responde 204 e invalida o refresh")
    void logoutRevogaORefresh() throws Exception {
        String refresh = login(MARIA, "tutor12345").get("refreshToken").asText();

        refresh(refresh).andExpect(status().isOk());       // antes: funciona
        logout(refresh).andExpect(status().isNoContent());
        refresh(refresh).andExpect(status().isUnauthorized()); // depois: revogado
    }

    @Test
    @DisplayName("logout repetido nao explode")
    void logoutRepetidoEIdempotente() throws Exception {
        String refresh = login(MARIA, "tutor12345").get("refreshToken").asText();

        logout(refresh).andExpect(status().isNoContent());
        // Cliente que reenvia o logout (retry, aba duplicada) nao deve tomar
        // erro: o efeito desejado -- token revogado -- ja aconteceu.
        logout(refresh).andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // /auth/usuarios — criacao com perfil arbitrario
    // ------------------------------------------------------------------

    private ResultActions criarUsuario(String token, String email, String perfil) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","senha":"senha12345","perfil":"%s"}"""
                        .formatted(email, perfil)));
    }

    private static String emailUnico() {
        // Os testes compartilham o mesmo H2 durante a execucao, e nao ha DELETE
        // de usuario para limpar depois. E-mail unico evita o 409 por duplicata.
        return "usuario-" + UUID.randomUUID() + "@teste.com";
    }

    @Test
    @DisplayName("admin cria usuario com perfil arbitrario")
    void adminCriaUsuarioComPerfilArbitrario() throws Exception {
        JsonNode criado = corpoDe(criarUsuario(tokenAdmin(), emailUnico(), "VETERINARIO")
                .andExpect(status().isCreated()));

        // A diferenca para /auth/registrar, que fixa TUTOR e por isso e publico.
        assertThat(criado.get("perfil").asText()).isEqualTo("VETERINARIO");
        assertThat(criado.has("senha")).isFalse();
    }

    @Test
    @DisplayName("tutor e veterinario nao criam usuario")
    void apenasAdminCriaUsuario() throws Exception {
        criarUsuario(tokenTutor(LUCAS), emailUnico(), "ADMIN").andExpect(status().isForbidden());
        criarUsuario(tokenVeterinaria(), emailUnico(), "ADMIN").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("criar usuario sem token responde 401")
    void criarUsuarioExigeAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/auth/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"anonimo@teste.com","senha":"senha12345","perfil":"ADMIN"}"""))
                .andExpect(status().isUnauthorized());
    }
}
