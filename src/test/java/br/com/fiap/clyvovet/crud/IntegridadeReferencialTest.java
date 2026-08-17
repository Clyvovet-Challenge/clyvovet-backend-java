package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Remocao de registro que tem dependentes.
 *
 * O banco recusa — as chaves estrangeiras nao tem ON DELETE CASCADE, e nao
 * deveriam mesmo ter: apagar um tutor nao pode levar junto o historico clinico
 * dos pets dele. O que se verifica aqui e a TRADUCAO dessa recusa: o cliente
 * precisa receber 409 com uma mensagem em portugues, e nao um 500 carregando o
 * nome da constraint, que revelaria a estrutura interna do schema.
 *
 * Como a exclusao falha, nada do seed e alterado por estes testes.
 */
class IntegridadeReferencialTest extends TesteDeApi {

    private void naoRemoveEExplica(String url) throws Exception {
        JsonNode erro = corpoDe(remover(url, tokenAdmin()).andExpect(status().isConflict()));

        assertThat(erro.get("mensagem").asText())
                .isEqualTo("Registro duplicado ou em uso por outro cadastro.")
                .doesNotContainIgnoringCase("constraint")
                .doesNotContainIgnoringCase("sql");
    }

    @Test
    @DisplayName("tutor com pet cadastrado nao e removido: responde 409")
    void tutorComPetNaoERemovido() throws Exception {
        naoRemoveEExplica("/tutores/" + SeedV2.TUTOR_LUCAS);

        // O tutor e o pet continuam la.
        buscar("/tutores/" + SeedV2.TUTOR_LUCAS, tokenAdmin()).andExpect(status().isOk());
        buscar("/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenAdmin()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("animal com evento clinico nao e removido: responde 409")
    void animalComEventoNaoERemovido() throws Exception {
        naoRemoveEExplica("/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS);

        buscar("/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenAdmin()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("clinica com veterinario vinculado nao e removida: responde 409")
    void clinicaComVeterinarioNaoERemovida() throws Exception {
        naoRemoveEExplica("/clinicas/" + SeedV2.CLINICA_VETCARE);

        buscar("/clinicas/" + SeedV2.CLINICA_VETCARE, tokenAdmin()).andExpect(status().isOk());
    }
}
