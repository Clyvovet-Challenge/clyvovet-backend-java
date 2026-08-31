package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
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
        mockMvc.perform(get("/api/v1/animais")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/tutores")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pagamentos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token invalido responde 401")
    void tokenInvalidoRetorna401() throws Exception {
        buscar("/api/v1/animais", "token.completamente.invalido").andExpect(status().isUnauthorized());
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

        buscar("/api/v1/tutores", tutor).andExpect(status().isForbidden());
        criar("/api/v1/eventos-clinicos", tutor, "{}").andExpect(status().isForbidden());
        criar("/api/v1/auth/usuarios", tutor, "{}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario lista tutores, mas nao administra clinicas")
    void veterinarioTemEscopoClinicoENaoAdministrativo() throws Exception {
        String veterinaria = tokenVeterinaria();

        buscar("/api/v1/tutores", veterinaria).andExpect(status().isOk());
        criar("/api/v1/clinicas", veterinaria, "{}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin alcanca as rotas administrativas")
    void adminAlcancaRotasAdministrativas() throws Exception {
        String admin = tokenAdmin();

        buscar("/api/v1/tutores", admin).andExpect(status().isOk());
        // 400 e nao 403: passou pela autorizacao e parou na validacao do corpo.
        criar("/api/v1/clinicas", admin, "{}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refresh token nao autentica chamadas da API")
    void refreshTokenNaoAutenticaApi() throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"tutor12345\"}".formatted(LUCAS)))
                .andReturn().getResponse().getContentAsString();
        String refresh = objectMapper.readTree(corpo).get("refreshToken").asText();

        buscar("/api/v1/animais", refresh).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auto-cadastro ignora o perfil e sempre cria TUTOR")
    void autoCadastroNaoPermiteEscolherPerfil() throws Exception {
        // Mesmo enviando perfil ADMIN no corpo, o campo nao existe no DTO e e
        // descartado: e a defesa contra escalacao por mass assignment.
        String corpo = mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invasor@teste.com","senha":"senha12345",
                                 "nome":"Invasor Teste","perfil":"ADMIN"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(corpo);
        assertThat(json.get("perfil").asText()).isEqualTo("TUTOR");
        assertThat(json.has("senha")).isFalse();
    }

    @Test
    @DisplayName("auto-cadastro cria o tutor junto, e o novo usuario ja consegue usar a API")
    void autoCadastroCriaOTutorVinculado() throws Exception {
        // Regressao do X12. Antes, o registro gravava so o Usuario: o tutor_id
        // ficava nulo, SegurancaService.podeAcessar() devolvia false em tudo, e
        // quem se cadastrava nao conseguia nem cadastrar o proprio animal. O
        // produto comecava por uma porta que nao levava a lugar nenhum.
        String corpo = mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"novo.tutor@teste.com","senha":"senha12345",
                                 "nome":"Novo Tutor"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(corpo);
        assertThat(json.get("tutorId").isNull()).isFalse();
        assertThat(json.get("tutorNome").asText()).isEqualTo("Novo Tutor");

        // E o vinculo funciona de verdade: com o token dele, ve o proprio escopo.
        String token = token("novo.tutor@teste.com", "senha12345");
        buscar("/api/v1/animais", token).andExpect(status().isOk());
    }

    @Test
    @DisplayName("auto-cadastro nao aceita mais tutorId do corpo")
    void autoCadastroNaoAceitaTutorIdDoCorpo() throws Exception {
        // Regressao do X13. O campo era aceito nesta rota, que e PUBLICA e NAO
        // AUTENTICADA: quem soubesse o UUID de um tutor existente se registrava
        // apontando para ele e passava a enxergar os animais, o historico e os
        // pagamentos daquela pessoa. Hoje o campo nao existe no DTO, entao o
        // Jackson o descarta e o tutor criado e sempre um novo.
        String corpo = mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"oportunista@teste.com","senha":"senha12345",
                                 "nome":"Oportunista","tutorId":"%s"}""".formatted(SeedV2.TUTOR_LUCAS)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(corpo).get("tutorId").asText())
                .isNotEqualTo(SeedV2.TUTOR_LUCAS);
    }

    @Test
    @DisplayName("auto-cadastro recusa e-mail que ja pertence a um tutor cadastrado")
    void autoCadastroRecusaEmailDeTutorExistente() throws Exception {
        // O vinculo automatico por e-mail parece gentil, mas sem confirmacao de
        // posse do e-mail ele reabre o X13 por uma porta mais larga: adivinhar
        // um e-mail e mais facil que adivinhar um UUID. Enquanto nao houver
        // verificacao, a recusa explicita e a resposta honesta.
        //
        // carlos.lima@email.com existe na tabela tutor (seed da V2) e NAO tem
        // usuario. Usar o e-mail do Lucas daria 409 tambem, mas pela checagem
        // de usuario duplicado -- passaria sem exercitar a regra que interessa.
        mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"carlos.lima@email.com","senha":"senha12345",
                                 "nome":"Alguem"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("e-mail duplicado responde 409, nao 500")
    void emailDuplicadoRetorna409() throws Exception {
        String payload = """
                {"email":"duplicado@teste.com","senha":"senha12345","nome":"Duplicado"}""";

        mockMvc.perform(post("/api/v1/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }
}
