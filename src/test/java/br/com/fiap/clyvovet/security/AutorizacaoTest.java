package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticacao e autorizacao por perfil, exercitadas pela cadeia real de
 * filtros — inclusive o JwtAuthenticationFilter. Usa os usuarios criados pelo
 * DevDataSeeder sobre o seed da migration V2.
 */
class AutorizacaoTest extends TesteDeApi {

    @Test
    @DisplayName("sem token, qualquer recurso responde 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/animais")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/tutores")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pagamentos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token invalido responde 401")
    void tokenInvalidoRetorna401() throws Exception {
        buscar("/animais", "token.completamente.invalido").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("swagger continua publico para a avaliacao")
    void swaggerEPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor nao lista tutores nem cria evento clinico")
    void tutorNaoAcessaRecursosDoCorpoClinico() throws Exception {
        String tutor = tokenTutor(LUCAS);

        buscar("/tutores", tutor).andExpect(status().isForbidden());
        criar("/eventos-clinicos", tutor, "{}").andExpect(status().isForbidden());
        criar("/auth/usuarios", tutor, "{}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario lista tutores, mas nao administra clinicas")
    void veterinarioTemEscopoClinicoENaoAdministrativo() throws Exception {
        String veterinaria = tokenVeterinaria();

        buscar("/tutores", veterinaria).andExpect(status().isOk());
        criar("/clinicas", veterinaria, "{}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin alcanca as rotas administrativas")
    void adminAlcancaRotasAdministrativas() throws Exception {
        String admin = tokenAdmin();

        buscar("/tutores", admin).andExpect(status().isOk());
        // 400 e nao 403: passou pela autorizacao e parou na validacao do corpo.
        criar("/clinicas", admin, "{}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refresh token nao autentica chamadas da API")
    void refreshTokenNaoAutenticaApi() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"tutor12345\"}".formatted(LUCAS)))
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(corpo).get("refreshToken").asText();

        buscar("/animais", refresh).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auto-cadastro ignora o perfil e sempre cria TUTOR")
    void autoCadastroNaoPermiteEscolherPerfil() throws Exception {
        // Mesmo enviando perfil ADMIN no corpo, o campo nao existe no DTO e e
        // descartado: e a defesa contra escalacao por mass assignment.
        String corpo = mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invasor@teste.com","senha":"senha12345","perfil":"ADMIN"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(corpo);
        assertThat(json.get("perfil").asText()).isEqualTo("TUTOR");
        assertThat(json.has("senha")).isFalse();
    }

    @Test
    @DisplayName("e-mail duplicado responde 409, nao 500")
    void emailDuplicadoRetorna409() throws Exception {
        String payload = """
                {"email":"duplicado@teste.com","senha":"senha12345"}""";

        mockMvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }
}
