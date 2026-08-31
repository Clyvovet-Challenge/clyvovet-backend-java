package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD da cadeia clinica: animal → evento clinico → pagamento.
 *
 * Os tres andam juntos porque um depende do outro para existir, e e justamente
 * o elo entre eles que as respostas desnormalizam (o evento devolve o nome do
 * animal; o pagamento, o id do evento). Testar isolado nao pegaria uma troca
 * de relacionamento no mapper.
 */
class AtendimentoCrudTest extends TesteDeApi {

    private static final String ANIMAL = """
            {"nome":"%s","raca":"Vira-lata","especie":"CAO","porte":"%s","cor":"Caramelo",
             "sexo":"MACHO","dataNascimento":"2021-04-01","observacao":"cadastro de teste",
             "tutorId":"%s"}""";

    private static final String EVENTO = """
            {"data":"2026-03-10","hora":"%s","descricao":"%s","tipoEvento":"%s",
             "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}""";

    private static final String PAGAMENTO = """
            {"formaPagamento":"%s","valor":%s,"dataPagamento":"2026-03-10",
             "descricao":"Consulta de teste","observacao":"registro de teste",
             "statusPagamento":"%s","eventoClinicoId":"%s"}""";

    /** Cria um animal do Lucas e ja agenda a remocao. Devolve o id. */
    private String animalDeTeste(String token) throws Exception {
        String id = corpoDe(criar("/api/v1/animais", token, ANIMAL.formatted("Pipoca", "MEDIO", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    private String eventoDeTeste(String token, String animalId) throws Exception {
        String id = corpoDe(criar("/api/v1/eventos-clinicos", token,
                EVENTO.formatted("14:30", "Consulta de rotina", "CONSULTA",
                        SeedV2.VET_CAMILA, animalId, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    // --------------------------------------------------------------- animal

    @Test
    @DisplayName("animal: cria, le, altera, remove e some")
    void cicloDeVidaDoAnimal() throws Exception {
        String admin = tokenAdmin();

        JsonNode criado = corpoDe(criar("/api/v1/animais", admin,
                ANIMAL.formatted("Pipoca", "MEDIO", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isCreated()));
        String id = criado.get("id").asText();
        removerDepois("/api/v1/animais/" + id);

        assertThat(criado.get("nome").asText()).isEqualTo("Pipoca");
        assertThat(criado.get("porte").asText()).isEqualTo("MEDIO");
        assertThat(criado.get("sexo").asText()).isEqualTo("MACHO");
        assertThat(criado.get("tutorId").asText()).isEqualTo(SeedV2.TUTOR_LUCAS);
        assertThat(criado.get("tutorNome").asText()).isEqualTo("Lucas M. Santos");

        assertThat(corpoDe(buscar("/api/v1/animais/" + id, admin).andExpect(status().isOk())))
                .isEqualTo(criado);

        JsonNode alterado = corpoDe(atualizar("/api/v1/animais/" + id, admin,
                ANIMAL.formatted("Pipoca Grande", "GRANDE", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isOk()));
        assertThat(alterado.get("id").asText()).isEqualTo(id);
        assertThat(alterado.get("nome").asText()).isEqualTo("Pipoca Grande");
        assertThat(alterado.get("porte").asText()).isEqualTo("GRANDE");

        assertThat(corpoDe(buscar("/api/v1/animais/" + id, admin)).get("porte").asText()).isEqualTo("GRANDE");

        remover("/api/v1/animais/" + id, admin).andExpect(status().isNoContent());
        buscar("/api/v1/animais/" + id, admin).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("animal: tutor inexistente responde 404")
    void animalComTutorInexistenteResponde404() throws Exception {
        criar("/api/v1/animais", tokenAdmin(), ANIMAL.formatted("Orfao", "PEQUENO", SeedV2.ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    /** O outro lado da regra de dono: no proprio tutor, o cadastro passa. */
    @Test
    @DisplayName("animal: tutor cadastra pet em seu proprio nome")
    void tutorCadastraOProprioPet() throws Exception {
        String lucas = tokenTutor(LUCAS);

        String id = corpoDe(criar("/api/v1/animais", lucas, ANIMAL.formatted("Meu Pet", "PEQUENO", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/api/v1/animais/" + id);

        buscar("/api/v1/animais/" + id, lucas).andExpect(status().isOk());
        remover("/api/v1/animais/" + id, lucas).andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------- evento

    @Test
    @DisplayName("evento clinico: cria, le, altera, remove e some")
    void cicloDeVidaDoEventoClinico() throws Exception {
        String vet = tokenVeterinaria();
        String animalId = animalDeTeste(tokenAdmin());

        JsonNode criado = corpoDe(criar("/api/v1/eventos-clinicos", vet,
                EVENTO.formatted("14:30", "Consulta de rotina", "CONSULTA",
                        SeedV2.VET_CAMILA, animalId, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated()));
        String id = criado.get("id").asText();
        removerDepois("/api/v1/eventos-clinicos/" + id);

        assertThat(criado.get("hora").asText()).isEqualTo("14:30");
        assertThat(criado.get("tipoEvento").asText()).isEqualTo("CONSULTA");
        assertThat(criado.get("animalNome").asText()).isEqualTo("Pipoca");
        assertThat(criado.get("veterinarioNome").asText()).isEqualTo("Camila Ferreira");
        // A Camila atende na VetCare. Registrar em outra clinica e recusado
        // desde a inversao do acesso: ela criaria um evento que nao conseguiria
        // ler em seguida.
        assertThat(criado.get("clinicaNome").asText()).isEqualTo("VetCare Prime");

        buscar("/api/v1/eventos-clinicos/" + id, vet).andExpect(status().isOk());

        JsonNode alterado = corpoDe(atualizar("/api/v1/eventos-clinicos/" + id, vet,
                EVENTO.formatted("09:00", "Retorno pos-consulta", "RETORNO",
                        SeedV2.VET_CAMILA, animalId, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isOk()));
        assertThat(alterado.get("id").asText()).isEqualTo(id);
        assertThat(alterado.get("hora").asText()).isEqualTo("09:00");
        assertThat(alterado.get("tipoEvento").asText()).isEqualTo("RETORNO");
        assertThat(alterado.get("clinicaNome").asText()).isEqualTo("VetCare Prime");

        remover("/api/v1/eventos-clinicos/" + id, vet).andExpect(status().isNoContent());
        buscar("/api/v1/eventos-clinicos/" + id, vet).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("evento clinico: animal inexistente responde 404")
    void eventoComAnimalInexistenteResponde404() throws Exception {
        criar("/api/v1/eventos-clinicos", tokenVeterinaria(),
                EVENTO.formatted("14:30", "Consulta", "CONSULTA",
                        SeedV2.VET_CAMILA, SeedV2.ID_INEXISTENTE, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------- pagamento

    @Test
    @DisplayName("pagamento: cria, le, altera, remove e some")
    void cicloDeVidaDoPagamento() throws Exception {
        String vet = tokenVeterinaria();
        String eventoId = eventoDeTeste(vet, animalDeTeste(tokenAdmin()));

        JsonNode criado = corpoDe(criar("/api/v1/pagamentos", vet,
                PAGAMENTO.formatted("PIX", "250.75", "PAGO", eventoId))
                .andExpect(status().isCreated()));
        String id = criado.get("id").asText();
        removerDepois("/api/v1/pagamentos/" + id);

        assertThat(criado.get("formaPagamento").asText()).isEqualTo("PIX");
        assertThat(criado.get("valor").decimalValue()).isEqualByComparingTo("250.75");
        assertThat(criado.get("statusPagamento").asText()).isEqualTo("PAGO");
        assertThat(criado.get("eventoClinicoId").asText()).isEqualTo(eventoId);

        buscar("/api/v1/pagamentos/" + id, vet).andExpect(status().isOk());

        JsonNode alterado = corpoDe(atualizar("/api/v1/pagamentos/" + id, vet,
                PAGAMENTO.formatted("BOLETO", "300.00", "PENDENTE", eventoId))
                .andExpect(status().isOk()));
        assertThat(alterado.get("id").asText()).isEqualTo(id);
        assertThat(alterado.get("formaPagamento").asText()).isEqualTo("BOLETO");
        assertThat(alterado.get("valor").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(alterado.get("statusPagamento").asText()).isEqualTo("PENDENTE");

        remover("/api/v1/pagamentos/" + id, vet).andExpect(status().isNoContent());
        buscar("/api/v1/pagamentos/" + id, vet).andExpect(status().isNotFound());
    }

    /**
     * O check do banco listava ESTORNADO enquanto o enum diz REEMBOLSADO, o que
     * tornava esse status impossivel de gravar. A migration V4 alinhou os dois —
     * este teste e o que impede a divergencia de voltar.
     */
    @Test
    @DisplayName("pagamento: aceita o status REEMBOLSADO do enum")
    void pagamentoAceitaStatusReembolsado() throws Exception {
        String vet = tokenVeterinaria();
        String eventoId = eventoDeTeste(vet, animalDeTeste(tokenAdmin()));

        String id = corpoDe(criar("/api/v1/pagamentos", vet,
                PAGAMENTO.formatted("CARTAO", "120.00", "REEMBOLSADO", eventoId))
                .andExpect(status().isCreated())).get("id").asText();
        removerDepois("/api/v1/pagamentos/" + id);

        assertThat(corpoDe(buscar("/api/v1/pagamentos/" + id, vet)).get("statusPagamento").asText())
                .isEqualTo("REEMBOLSADO");
    }

    @Test
    @DisplayName("pagamento: evento clinico inexistente responde 404")
    void pagamentoComEventoInexistenteResponde404() throws Exception {
        criar("/api/v1/pagamentos", tokenVeterinaria(),
                PAGAMENTO.formatted("PIX", "10.00", "PAGO", SeedV2.ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }
}
