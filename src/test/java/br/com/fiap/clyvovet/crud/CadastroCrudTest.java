package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD dos cadastros: tutor, clinica e veterinario.
 *
 * Cada teste percorre o ciclo inteiro — criar, ler, alterar, remover e conferir
 * que sumiu — porque e assim que erro de mapeamento aparece: um campo que o
 * POST grava e o PUT esquece so se revela quando se le depois de alterar.
 *
 * O que cada teste cria e removido no fim, via removerDepois: os testes de
 * isolamento por tutor conferem contagens exatas e nao podem herdar sobras.
 */
class CadastroCrudTest extends TesteDeApi {

    /**
     * CPF e CNPJ tem constraint de unicidade e o seed ja ocupa varios deles —
     * inclusive numa sequencia previsivel (…44455566677). Os documentos daqui
     * comecam com 9, faixa que o seed nao usa, para o teste falhar por um
     * motivo real e nao por colisao com o dado de partida.
     */
    private static final String CPF_JOANA = "90000000001";
    private static final String CPF_REPETIDO = "90000000002";
    private static final String CPF_MARINA = "90000000003";
    private static final String CPF_PAULO = "90000000004";
    private static final String CPF_SEM_CLINICA = "90000000005";
    private static final String CNPJ_NOVA = "99000000000191";
    private static final String CNPJ_REPETIDO = "99000000000192";

    private static final String TUTOR = """
            {"nome":"%s","cpf":"%s","email":"%s","telefone":"11970001122",
             "sexo":"%s","dataNascimento":"1990-05-10",
             "endereco":{"logradouro":"Av. Brasil","numero":"20","bairro":"Jardins",
                         "cidade":"Campinas","estado":"SP","cep":"13010000"}}""";

    private static final String CLINICA = """
            {"nome":"%s","cnpj":"%s","telefone":"1133334444","email":"contato@novaclinica.com.br",
             "endereco":{"logradouro":"Rua das Flores","numero":"100","bairro":"Centro",
                         "cidade":"Santos","estado":"SP","cep":"11010000"}}""";

    private static final String VETERINARIO = """
            {"nome":"%s","cpf":"%s","crmv":"%s","especialidade":"%s",
             "telefone":"11970003344","email":"novo.vet@vetcare.com.br",
             "dataNascimento":"1988-02-20","sexo":"FEMININO","clinicaId":"%s",
             "endereco":{"logradouro":"Rua Vet","numero":"5","bairro":"Centro",
                         "cidade":"Santos","estado":"SP","cep":"11010001"}}""";

    // ---------------------------------------------------------------- tutor

    @Test
    @DisplayName("tutor: cria, le, altera, remove e some")
    void cicloDeVidaDoTutor() throws Exception {
        String admin = tokenAdmin();

        JsonNode criado = corpoDe(criar("/tutores", admin,
                TUTOR.formatted("Joana Teste", CPF_JOANA, "joana.teste@email.com", "FEMININO"))
                .andExpect(status().isCreated()));
        String id = criado.get("id").asText();
        removerDepois("/tutores/" + id);

        assertThat(criado.get("nome").asText()).isEqualTo("Joana Teste");
        assertThat(criado.get("cpf").asText()).isEqualTo(CPF_JOANA);
        assertThat(criado.get("sexo").asText()).isEqualTo("FEMININO");
        assertThat(criado.get("endereco").get("cidade").asText()).isEqualTo("Campinas");
        assertThat(criado.get("endereco").get("complemento").isNull()).isTrue();

        assertThat(corpoDe(buscar("/tutores/" + id, admin).andExpect(status().isOk())))
                .isEqualTo(criado);

        JsonNode alterado = corpoDe(atualizar("/tutores/" + id, admin,
                TUTOR.formatted("Joana Alterada", CPF_JOANA, "joana.nova@email.com", "OUTRO"))
                .andExpect(status().isOk()));
        assertThat(alterado.get("id").asText()).isEqualTo(id);
        assertThat(alterado.get("nome").asText()).isEqualTo("Joana Alterada");
        assertThat(alterado.get("email").asText()).isEqualTo("joana.nova@email.com");
        assertThat(alterado.get("sexo").asText()).isEqualTo("OUTRO");

        // A alteracao precisa ter chegado ao banco, e nao so a resposta.
        assertThat(corpoDe(buscar("/tutores/" + id, admin)).get("nome").asText())
                .isEqualTo("Joana Alterada");

        remover("/tutores/" + id, admin).andExpect(status().isNoContent());
        buscar("/tutores/" + id, admin).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("tutor: CPF repetido responde 409, nao 500")
    void cpfRepetidoResponde409() throws Exception {
        String admin = tokenAdmin();

        String id = corpoDe(criar("/tutores", admin,
                TUTOR.formatted("Primeira", CPF_REPETIDO, "primeira@email.com", "FEMININO"))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/tutores/" + id);

        criar("/tutores", admin,
                TUTOR.formatted("Segunda", CPF_REPETIDO, "segunda@email.com", "FEMININO"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("tutor: id inexistente responde 404 no GET, no PUT e no DELETE")
    void tutorInexistenteResponde404() throws Exception {
        String admin = tokenAdmin();
        String url = "/tutores/" + SeedV2.ID_INEXISTENTE;

        buscar(url, admin).andExpect(status().isNotFound());
        atualizar(url, admin, TUTOR.formatted("Fantasma", "11122233399", "fantasma@email.com", "OUTRO"))
                .andExpect(status().isNotFound());
        remover(url, admin).andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------- clinica

    @Test
    @DisplayName("clinica: cria, le, altera, remove e some")
    void cicloDeVidaDaClinica() throws Exception {
        String admin = tokenAdmin();

        JsonNode criada = corpoDe(criar("/clinicas", admin, CLINICA.formatted("Clinica Nova", CNPJ_NOVA))
                .andExpect(status().isCreated()));
        String id = criada.get("id").asText();
        removerDepois("/clinicas/" + id);

        assertThat(criada.get("nome").asText()).isEqualTo("Clinica Nova");
        assertThat(criada.get("endereco").get("cidade").asText()).isEqualTo("Santos");

        buscar("/clinicas/" + id, admin).andExpect(status().isOk());

        JsonNode alterada = corpoDe(atualizar("/clinicas/" + id, admin,
                CLINICA.formatted("Clinica Renomeada", CNPJ_NOVA)).andExpect(status().isOk()));
        assertThat(alterada.get("nome").asText()).isEqualTo("Clinica Renomeada");
        assertThat(alterada.get("cnpj").asText()).isEqualTo(CNPJ_NOVA);

        remover("/clinicas/" + id, admin).andExpect(status().isNoContent());
        buscar("/clinicas/" + id, admin).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("clinica: CNPJ repetido responde 409")
    void cnpjRepetidoResponde409() throws Exception {
        String admin = tokenAdmin();

        String id = corpoDe(criar("/clinicas", admin, CLINICA.formatted("Primeira", CNPJ_REPETIDO))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/clinicas/" + id);

        criar("/clinicas", admin, CLINICA.formatted("Segunda", CNPJ_REPETIDO))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------- veterinario

    @Test
    @DisplayName("veterinario: cria, le, altera a clinica, remove e some")
    void cicloDeVidaDoVeterinario() throws Exception {
        String admin = tokenAdmin();

        JsonNode criado = corpoDe(criar("/veterinarios", admin,
                VETERINARIO.formatted("Marina Teste", CPF_MARINA, "CRMV-SP 99999",
                        "Dermatologia", SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated()));
        String id = criado.get("id").asText();
        removerDepois("/veterinarios/" + id);

        assertThat(criado.get("nome").asText()).isEqualTo("Marina Teste");
        assertThat(criado.get("crmv").asText()).isEqualTo("CRMV-SP 99999");
        assertThat(criado.get("clinicaId").asText()).isEqualTo(SeedV2.CLINICA_VETCARE);
        assertThat(criado.get("clinicaNome").asText()).isEqualTo("VetCare Prime");

        buscar("/veterinarios/" + id, admin).andExpect(status().isOk());

        JsonNode alterado = corpoDe(atualizar("/veterinarios/" + id, admin,
                VETERINARIO.formatted("Marina Teste", CPF_MARINA, "CRMV-SP 99999",
                        "Oncologia", SeedV2.CLINICA_PETMED))
                .andExpect(status().isOk()));
        assertThat(alterado.get("especialidade").asText()).isEqualTo("Oncologia");
        assertThat(alterado.get("clinicaNome").asText()).isEqualTo("PetMed Centro");

        remover("/veterinarios/" + id, admin).andExpect(status().isNoContent());
        buscar("/veterinarios/" + id, admin).andExpect(status().isNotFound());
    }

    /**
     * O CRMV do seed tem 13 caracteres ("CRMV-SP 14320") e a coluna aceita 30,
     * mas o DTO limitava a 6: era impossivel cadastrar pela API um veterinario
     * no formato que o proprio sistema usa.
     */
    @Test
    @DisplayName("veterinario: aceita o CRMV no formato do conselho")
    void aceitaCrmvNoFormatoReal() throws Exception {
        String id = corpoDe(criar("/veterinarios", tokenAdmin(),
                VETERINARIO.formatted("Paulo Teste", CPF_PAULO, "CRMV-SP 14321",
                        "Clinica Geral", SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated())).get("id").asText();

        removerDepois("/veterinarios/" + id);
    }

    @Test
    @DisplayName("veterinario: clinica inexistente responde 404")
    void clinicaInexistenteResponde404() throws Exception {
        criar("/veterinarios", tokenAdmin(),
                VETERINARIO.formatted("Sem Clinica", CPF_SEM_CLINICA, "CRMV-SP 88888",
                        "Clinica Geral", SeedV2.ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }
}
