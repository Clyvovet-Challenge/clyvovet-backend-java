package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão do item E3 — invalidação de cache entre entidades relacionadas.
 *
 * Cada service invalidava só o próprio cache, mas as respostas se cruzam:
 * AnimalResponse carrega tutorNome, EventoClinicoResponse carrega animalNome e
 * veterinarioNome. Renomear um tutor deixava a listagem de animais devolvendo o
 * nome antigo por até 10 minutos — o dado certo no banco, o errado na tela.
 */
class CacheCruzadoTest extends TesteDeApi {

    @Test
    @DisplayName("renomear o tutor atualiza o nome dentro da listagem de animais")
    void tutorRenomeadoAparecemNaListaDeAnimais() throws Exception {
        String admin = tokenAdmin();

        // Popula o cache de animais com o nome atual.
        buscar("/api/v1/animais?size=50", admin).andExpect(status().isOk());

        atualizarParcialmente("/api/v1/tutores/" + SeedV2.TUTOR_LUCAS, admin, """
                {"nome":"Lucas Renomeado"}""")
                .andExpect(status().isOk());

        JsonNode animais = corpoDe(buscar("/api/v1/animais?size=50", admin));
        boolean achou = false;
        for (JsonNode animal : animais.get("content")) {
            if (SeedV2.TUTOR_LUCAS.equals(animal.path("tutorId").asText())) {
                achou = true;
                assertThat(animal.get("tutorNome").asText()).isEqualTo("Lucas Renomeado");
            }
        }
        assertThat(achou).isTrue();

        // Devolve o nome original: a suíte grava de verdade.
        atualizarParcialmente("/api/v1/tutores/" + SeedV2.TUTOR_LUCAS, admin, """
                {"nome":"Lucas M. Santos"}""")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("renomear o animal atualiza o nome dentro da listagem de eventos")
    void animalRenomeadoAparecemNaListaDeEventos() throws Exception {
        String vet = tokenVeterinaria();

        buscar("/api/v1/eventos-clinicos?size=50", vet).andExpect(status().isOk());

        atualizarParcialmente("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, vet, """
                {"nome":"Bolinha Renomeado"}""")
                .andExpect(status().isOk());

        JsonNode eventos = corpoDe(buscar("/api/v1/eventos-clinicos?size=50", vet));
        boolean achou = false;
        for (JsonNode evento : eventos.get("content")) {
            if (SeedV2.ANIMAL_BOLINHA_DO_LUCAS.equals(evento.path("animalId").asText())) {
                achou = true;
                assertThat(evento.get("animalNome").asText()).isEqualTo("Bolinha Renomeado");
            }
        }
        assertThat(achou).isTrue();

        atualizarParcialmente("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, vet, """
                {"nome":"Bolinha"}""")
                .andExpect(status().isOk());
    }
}
