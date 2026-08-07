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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolamento entre tutores.
 *
 * Regra de rota nao cobre isto: os dois tutores tem o mesmo perfil e passariam
 * igualmente por ela. A verificacao acontece em duas frentes, e as duas
 * precisam ser testadas — acesso por id (@PreAuthorize) e listagem (filtro na
 * query, incluido tambem na chave do cache).
 *
 * Dados vindos da migration V2: Lucas e dono do Bolinha; Maria, da Mimi e do Rex.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnershipTest {

    private static final String BOLINHA_DO_LUCAS = "44444444-4444-4444-4444-000000000001";
    private static final String MIMI_DA_MARIA = "44444444-4444-4444-4444-000000000002";
    private static final String TUTOR_LUCAS = "22222222-2222-2222-2222-000000000001";
    private static final String TUTOR_MARIA = "22222222-2222-2222-2222-000000000002";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token(String email) throws Exception {
        String senha = email.startsWith("camila") ? "vet12345" : "tutor12345";
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("accessToken").asText();
    }

    private JsonNode listarAnimais(String token) throws Exception {
        String corpo = mockMvc.perform(get("/animais").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    @Test
    @DisplayName("tutor nao acessa pet de outro tutor pelo id")
    void tutorNaoAcessaPetDeTerceiro() throws Exception {
        String lucas = token("lucas.santos@email.com");

        mockMvc.perform(get("/animais/" + BOLINHA_DO_LUCAS).header("Authorization", "Bearer " + lucas))
                .andExpect(status().isOk());
        mockMvc.perform(get("/animais/" + MIMI_DA_MARIA).header("Authorization", "Bearer " + lucas))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tutor nao altera nem remove pet de outro tutor")
    void tutorNaoEscreveEmPetDeTerceiro() throws Exception {
        String lucas = token("lucas.santos@email.com");

        mockMvc.perform(delete("/animais/" + MIMI_DA_MARIA).header("Authorization", "Bearer " + lucas))
                .andExpect(status().isForbidden());

        // Corpo VALIDO de proposito. O binding do @RequestBody acontece antes do
        // @PreAuthorize, entao um corpo invalido pararia em 400 e o teste nao
        // chegaria a exercitar a autorizacao — que e o que se quer provar aqui.
        String animalValido = """
                {"nome":"Sequestrado","raca":"Siames","especie":"GATO","porte":"PEQUENO",
                 "cor":"Bege","sexo":"FEMEA","dataNascimento":"2021-07-05",
                 "tutorId":"%s"}""".formatted(TUTOR_LUCAS);

        mockMvc.perform(put("/animais/" + MIMI_DA_MARIA).header("Authorization", "Bearer " + lucas)
                        .contentType(MediaType.APPLICATION_JSON).content(animalValido))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listagem de animais mostra apenas os pets do proprio tutor")
    void listagemDeAnimaisEIsoladaPorTutor() throws Exception {
        JsonNode doLucas = listarAnimais(token("lucas.santos@email.com"));
        JsonNode daMaria = listarAnimais(token("maria.oliveira@email.com"));

        assertThat(doLucas.get("content")).hasSize(1);
        assertThat(doLucas.get("content").get(0).get("nome").asText()).isEqualTo("Bolinha");

        assertThat(daMaria.get("content")).hasSize(2);
        assertThat(daMaria.get("content").findValuesAsText("nome")).containsExactlyInAnyOrder("Mimi", "Rex");
    }

    @Test
    @DisplayName("cache nao vaza a listagem de um tutor para outro")
    void cacheNaoVazaEntreTutores() throws Exception {
        String lucas = token("lucas.santos@email.com");
        String maria = token("maria.oliveira@email.com");

        // Primeira chamada popula o cache; a segunda, com filtros e paginacao
        // identicos, so devolve o resultado certo porque o tutor entra na chave.
        listarAnimais(lucas);
        assertThat(listarAnimais(maria).get("content")).hasSize(2);
        assertThat(listarAnimais(lucas).get("content")).hasSize(1);
    }

    @Test
    @DisplayName("veterinario enxerga a base inteira")
    void veterinarioEnxergaTudo() throws Exception {
        JsonNode todos = listarAnimais(token("camila.ferreira@vetcare.com.br"));

        assertThat(todos.get("totalElements").asInt()).isEqualTo(6);
        mockMvc.perform(get("/animais/" + MIMI_DA_MARIA)
                        .header("Authorization", "Bearer " + token("camila.ferreira@vetcare.com.br")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor le o proprio cadastro, mas nao o de terceiros")
    void tutorSoLeOProprioCadastro() throws Exception {
        String lucas = token("lucas.santos@email.com");

        mockMvc.perform(get("/tutores/" + TUTOR_LUCAS).header("Authorization", "Bearer " + lucas))
                .andExpect(status().isOk());
        mockMvc.perform(get("/tutores/" + TUTOR_MARIA).header("Authorization", "Bearer " + lucas))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("eventos e pagamentos tambem sao isolados por tutor")
    void eventosEPagamentosSaoIsolados() throws Exception {
        String lucas = token("lucas.santos@email.com");
        String vet = token("camila.ferreira@vetcare.com.br");

        String eventosLucas = mockMvc.perform(get("/eventos-clinicos").header("Authorization", "Bearer " + lucas))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String eventosVet = mockMvc.perform(get("/eventos-clinicos").header("Authorization", "Bearer " + vet))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        int totalLucas = objectMapper.readTree(eventosLucas).get("totalElements").asInt();
        int totalVet = objectMapper.readTree(eventosVet).get("totalElements").asInt();

        assertThat(totalLucas).isEqualTo(6);   // apenas os eventos do Bolinha
        assertThat(totalVet).isEqualTo(11);    // todos
        assertThat(totalLucas).isLessThan(totalVet);
    }
}
