package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH: atualizacao parcial.
 *
 * O que distingue o PATCH do PUT e o que estes testes precisam provar: campo
 * omitido NAO e apagado, campo enviado continua sendo validado, e o relaxamento
 * das restricoes de presenca nao abriu buraco na regra de ownership.
 */
class AtualizacaoParcialTest extends TesteDeApi {

    @Test
    @DisplayName("PATCH altera so o campo enviado e preserva o resto")
    void patchAlteraApenasOCampoEnviado() throws Exception {
        String vet = tokenVeterinaria();
        String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        JsonNode antes = corpoDe(buscar(url, vet).andExpect(status().isOk()));

        JsonNode depois = corpoDe(
                atualizarParcialmente(url, vet, """
                        {"cor":"Caramelo"}""").andExpect(status().isOk()));

        assertThat(depois.get("cor").asText()).isEqualTo("Caramelo");
        // Tudo o que nao foi citado no corpo continua como estava. E aqui que um
        // PATCH mal implementado se denuncia: ele zera o que nao recebeu.
        assertThat(depois.get("nome").asText()).isEqualTo(antes.get("nome").asText());
        assertThat(depois.get("raca").asText()).isEqualTo(antes.get("raca").asText());
        assertThat(depois.get("especie").asText()).isEqualTo(antes.get("especie").asText());
        assertThat(depois.get("dataNascimento").asText()).isEqualTo(antes.get("dataNascimento").asText());

        // Devolve o pet ao estado do seed, que os demais testes assumem.
        atualizarParcialmente(url, vet, "{\"cor\":\"" + antes.get("cor").asText() + "\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH nao troca o dono quando o tutorId nao vem no corpo")
    void patchSemTutorIdPreservaODono() throws Exception {
        String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        JsonNode depois = corpoDe(
                atualizarParcialmente(url, tokenVeterinaria(), """
                        {"observacao":"retorno em 30 dias"}""").andExpect(status().isOk()));

        assertThat(depois.get("tutorId").asText()).isEqualTo(SeedV2.TUTOR_LUCAS);
    }

    @Test
    @DisplayName("tutor edita o proprio pet sem reenviar o proprio id")
    void tutorEditaOProprioPetSemReenviarOId() throws Exception {
        // Regressao do podeAtribuirTutor: com o @PreAuthorize do PUT, um patch
        // sem tutorId cairia em podeAcessarTutor(null), que devolve false, e o
        // tutor tomaria 403 ao mexer no proprio animal.
        atualizarParcialmente(
                "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                tokenTutor(LUCAS),
                """
                {"cor":"Dourado"}""")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor nao transfere o proprio pet para outro tutor via PATCH")
    void tutorNaoTransferePetViaPatch() throws Exception {
        atualizarParcialmente(
                "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                tokenTutor(LUCAS),
                """
                {"tutorId":"%s"}""".formatted(SeedV2.TUTOR_MARIA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tutor nao alcanca pet de terceiro via PATCH")
    void tutorNaoAlcancaPetDeTerceiroViaPatch() throws Exception {
        atualizarParcialmente(
                "/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA,
                tokenTutor(LUCAS),
                """
                {"cor":"Preto"}""")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH valida o formato do campo que foi enviado")
    void patchValidaOCampoEnviado() throws Exception {
        String admin = tokenAdmin();
        String url = "/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE;

        // Presenca deixou de ser exigida, mas formato nao: e-mail invalido
        // continua sendo 400, nao 500 la no banco.
        atualizarParcialmente(url, admin, """
                {"email":"nao-e-email"}""").andExpect(status().isBadRequest());

        // E o campo omitido nao e cobrado.
        atualizarParcialmente(url, admin, """
                {"telefone":"1131000001"}""").andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH em id inexistente responde 404")
    void patchEmIdInexistenteResponde404() throws Exception {
        atualizarParcialmente("/api/v1/clinicas/" + SeedV2.ID_INEXISTENTE, tokenAdmin(), """
                {"nome":"Qualquer"}""").andExpect(status().isNotFound());
    }
}
