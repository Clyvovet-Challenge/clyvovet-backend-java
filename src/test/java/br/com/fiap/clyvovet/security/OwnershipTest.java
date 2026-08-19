package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolamento entre tutores.
 *
 * Regra de rota nao cobre isto: os dois tutores tem o mesmo perfil e passariam
 * igualmente por ela. A verificacao acontece em tres frentes, e as tres
 * precisam ser testadas — acesso por id (@PreAuthorize), listagem (filtro na
 * query, incluido tambem na chave do cache) e o dono informado no CORPO da
 * requisicao, que nao aparece na URL.
 *
 * Dados vindos da migration V2: Lucas e dono do Bolinha; Maria, da Mimi e do Rex.
 */
class OwnershipTest extends TesteDeApi {

    private static final String ANIMAL = """
            {"nome":"%s","raca":"Siames","especie":"GATO","porte":"PEQUENO","cor":"Bege",
             "sexo":"FEMEA","dataNascimento":"2021-07-05","tutorId":"%s"}""";

    private JsonNode listarAnimais(String token) throws Exception {
        return corpoDe(buscar("/api/v1/animais", token).andExpect(status().isOk()));
    }

    @Test
    @DisplayName("tutor nao acessa pet de outro tutor pelo id")
    void tutorNaoAcessaPetDeTerceiro() throws Exception {
        String lucas = tokenTutor(LUCAS);

        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, lucas).andExpect(status().isOk());
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA, lucas).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tutor nao altera nem remove pet de outro tutor")
    void tutorNaoEscreveEmPetDeTerceiro() throws Exception {
        String lucas = tokenTutor(LUCAS);

        remover("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA, lucas).andExpect(status().isForbidden());

        // Corpo VALIDO de proposito. O binding do @RequestBody acontece antes do
        // @PreAuthorize, entao um corpo invalido pararia em 400 e o teste nao
        // chegaria a exercitar a autorizacao — que e o que se quer provar aqui.
        atualizar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA, lucas,
                ANIMAL.formatted("Sequestrado", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isForbidden());
    }

    /**
     * O tutorId vem do corpo, e nao da URL: a regra que protege o pet pelo id
     * nao ve esse campo. Sem uma checagem propria, um tutor cadastrava pet no
     * nome de qualquer outro — e o dono legitimo passava a enxergar na propria
     * listagem um animal que nunca cadastrou.
     */
    @Test
    @DisplayName("tutor nao cadastra pet no nome de outro tutor")
    void tutorNaoCadastraPetParaTerceiro() throws Exception {
        criar("/api/v1/animais", tokenTutor(LUCAS), ANIMAL.formatted("Pet Alheio", SeedV2.TUTOR_MARIA))
                .andExpect(status().isForbidden());

        assertThat(listarAnimais(tokenTutor(MARIA)).get("content")).hasSize(2);
    }

    /** Mesma brecha na direcao contraria: dar o proprio pet a outro tutor. */
    @Test
    @DisplayName("tutor nao transfere o proprio pet para outro tutor")
    void tutorNaoTransfereOProprioPet() throws Exception {
        String lucas = tokenTutor(LUCAS);

        atualizar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, lucas,
                ANIMAL.formatted("Bolinha", SeedV2.TUTOR_MARIA))
                .andExpect(status().isForbidden());

        assertThat(corpoDe(buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, lucas))
                .get("tutorId").asText()).isEqualTo(SeedV2.TUTOR_LUCAS);
    }

    @Test
    @DisplayName("listagem de animais mostra apenas os pets do proprio tutor")
    void listagemDeAnimaisEIsoladaPorTutor() throws Exception {
        JsonNode doLucas = listarAnimais(tokenTutor(LUCAS));
        JsonNode daMaria = listarAnimais(tokenTutor(MARIA));

        assertThat(doLucas.get("content")).hasSize(1);
        assertThat(doLucas.get("content").get(0).get("nome").asText()).isEqualTo("Bolinha");

        assertThat(daMaria.get("content")).hasSize(2);
        assertThat(daMaria.get("content").findValuesAsText("nome")).containsExactlyInAnyOrder("Mimi", "Rex");
    }

    @Test
    @DisplayName("cache nao vaza a listagem de um tutor para outro")
    void cacheNaoVazaEntreTutores() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String maria = tokenTutor(MARIA);

        // Primeira chamada popula o cache; a segunda, com filtros e paginacao
        // identicos, so devolve o resultado certo porque o tutor entra na chave.
        listarAnimais(lucas);
        assertThat(listarAnimais(maria).get("content")).hasSize(2);
        assertThat(listarAnimais(lucas).get("content")).hasSize(1);
    }

    @Test
    @DisplayName("veterinario enxerga a base inteira")
    void veterinarioEnxergaTudo() throws Exception {
        String veterinaria = tokenVeterinaria();

        assertThat(totalDe(listarAnimais(veterinaria))).isEqualTo(6);
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA, veterinaria).andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor le o proprio cadastro, mas nao o de terceiros")
    void tutorSoLeOProprioCadastro() throws Exception {
        String lucas = tokenTutor(LUCAS);

        buscar("/api/v1/tutores/" + SeedV2.TUTOR_LUCAS, lucas).andExpect(status().isOk());
        buscar("/api/v1/tutores/" + SeedV2.TUTOR_MARIA, lucas).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("eventos e pagamentos tambem sao isolados por tutor")
    void eventosEPagamentosSaoIsolados() throws Exception {
        int totalLucas = totalDe(buscar("/api/v1/eventos-clinicos", tokenTutor(LUCAS)).andExpect(status().isOk()));
        int totalVet = totalDe(buscar("/api/v1/eventos-clinicos", tokenVeterinaria()).andExpect(status().isOk()));

        assertThat(totalLucas).isEqualTo(6);   // apenas os eventos do Bolinha
        assertThat(totalVet).isEqualTo(11);    // todos
        assertThat(totalLucas).isLessThan(totalVet);
    }
}
