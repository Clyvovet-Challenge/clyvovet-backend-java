package br.com.fiap.clyvovet.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticacao e autorizacao por perfil, exercitadas pela cadeia real de
 * filtros — inclusive o JwtAuthenticationFilter. Usa os usuarios criados pelo
 * DevDataSeeder sobre o seed da migration V2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacaoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token(String email, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("accessToken").asText();
    }

    private String tokenAdmin() throws Exception {
        return token("admin@clyvovet.com", "admin12345");
    }

    private String tokenVeterinario() throws Exception {
        return token("camila.ferreira@vetcare.com.br", "vet12345");
    }

    private String tokenTutor() throws Exception {
        return token("lucas.santos@email.com", "tutor12345");
    }

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
        mockMvc.perform(get("/animais").header("Authorization", "Bearer token.completamente.invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("swagger continua publico para a avaliacao")
    void swaggerEPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor nao lista tutores nem cria evento clinico")
    void tutorNaoAcessaRecursosDoCorpoClinico() throws Exception {
        String tutor = tokenTutor();

        mockMvc.perform(get("/tutores").header("Authorization", "Bearer " + tutor))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/eventos-clinicos").header("Authorization", "Bearer " + tutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/usuarios").header("Authorization", "Bearer " + tutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario lista tutores, mas nao administra clinicas")
    void veterinarioTemEscopoClinicoENaoAdministrativo() throws Exception {
        String vet = tokenVeterinario();

        mockMvc.perform(get("/tutores").header("Authorization", "Bearer " + vet))
                .andExpect(status().isOk());
        mockMvc.perform(post("/clinicas").header("Authorization", "Bearer " + vet)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin alcanca as rotas administrativas")
    void adminAlcancaRotasAdministrativas() throws Exception {
        String admin = tokenAdmin();

        mockMvc.perform(get("/tutores").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        // 400 e nao 403: passou pela autorizacao e parou na validacao do corpo.
        mockMvc.perform(post("/clinicas").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refresh token nao autentica chamadas da API")
    void refreshTokenNaoAutenticaApi() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lucas.santos@email.com\",\"senha\":\"tutor12345\"}"))
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(corpo).get("refreshToken").asText();

        mockMvc.perform(get("/animais").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
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
