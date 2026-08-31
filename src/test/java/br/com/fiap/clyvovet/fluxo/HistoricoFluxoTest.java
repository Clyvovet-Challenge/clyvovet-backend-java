package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O fluxo C da spec 08 — acesso ao historico clinico em tres niveis.
 *
 * O que estes testes verificam nao e "o dado voltou", e sim QUANTO dado voltou
 * para cada solicitante. Um veterinario sem consentimento e um com consentimento
 * chamam a MESMA rota e recebem objetos de tamanhos diferentes; e essa diferenca
 * que e o produto.
 */
class HistoricoFluxoTest extends TesteDeApi {

    private static final String CHIP = "900000000012345";

    /** Cadastra um animal do Lucas com microchip, e devolve o id. */
    private String animalComChip(String chip) throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                {"nome":"Thor","raca":"Border Collie","especie":"Canina","porte":"MEDIO",
                 "cor":"Preto e branco","sexo":"MACHO","dataNascimento":"2021-04-10",
                 "tutorId":"%s","microchip":"%s","castrado":true}"""
                .formatted(SeedV2.TUTOR_LUCAS, chip));
        animal.andExpect(status().isCreated());
        String id = idDe(animal);
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    @Test
    @DisplayName("o veterinário alcança o resumo de segurança pelo microchip, sem consentimento")
    void resumoPeloMicrochip() throws Exception {
        String animalId = animalComChip(CHIP);

        criar("/api/v1/animais/" + animalId + "/alertas", tokenVeterinaria(), """
                {"tipo":"ALERGIA","descricao":"Anafilaxia a dipirona"}""")
                .andExpect(status().isCreated())
                // A origem e derivada do perfil, nunca do corpo: e ela que diz
                // ao proximo profissional o quanto confiar na informacao.
                .andExpect(jsonPath("$.origem").value("VETERINARIO"));

        ResultActions resumo = buscar("/api/v1/animais/resumo?microchip=" + CHIP, tokenVeterinaria());

        resumo.andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thor"))
                .andExpect(jsonPath("$.castrado").value(true))
                .andExpect(jsonPath("$.alertas[0].descricao").value("Anafilaxia a dipirona"))
                // O contato vem para que a emergencia consiga ligar.
                .andExpect(jsonPath("$.telefoneDeEmergencia").exists());
    }

    @Test
    @DisplayName("o resumo de segurança não expõe CPF, endereço nem linha do tempo")
    void resumoNaoVazaNivel2() throws Exception {
        animalComChip("900000000054321");

        JsonNode resumo = corpoDe(buscar("/api/v1/animais/resumo?microchip=900000000054321",
                tokenVeterinaria()).andExpect(status().isOk()));

        // O que NAO esta no resumo e tao deliberado quanto o que esta: para
        // atender uma emergencia basta conseguir ligar para o dono.
        assertThat(resumo.has("tutorNome")).isFalse();
        assertThat(resumo.has("tutorCpf")).isFalse();
        assertThat(resumo.has("linhaDoTempo")).isFalse();
        assertThat(resumo.has("documentos")).isFalse();
        assertThat(resumo.has("telefoneDeEmergencia")).isTrue();
    }

    @Test
    @DisplayName("o alerta registrado pelo tutor fica marcado como origem TUTOR")
    void alertaDoTutorTemOrigemPropria() throws Exception {
        String animalId = animalComChip("900000000011111");

        // "o tutor disse que tem alergia" e "o veterinario registrou
        // anafilaxia" pesam diferente na decisao clinica.
        criar("/api/v1/animais/" + animalId + "/alertas", tokenTutor(LUCAS), """
                {"tipo":"MEDICACAO_CONTINUA","descricao":"Toma remédio para o coração"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("TUTOR"));
    }

    @Test
    @DisplayName("microchip inexistente é 404, e a mensagem não fala em ID")
    void microchipInexistente() throws Exception {
        buscar("/api/v1/animais/resumo?microchip=900000000099999", tokenVeterinaria())
                .andExpect(status().isNotFound())
                // A mensagem padrao termina em "com ID: <uuid>", o que mandaria
                // quem le atras da chave errada.
                .andExpect(jsonPath("$.mensagem").value("Nenhum animal com o microchip informado"));
    }

    @Test
    @DisplayName("o tutor não usa a busca por microchip: ela é do corpo clínico")
    void tutorNaoUsaOResumo() throws Exception {
        animalComChip("900000000022222");

        buscar("/api/v1/animais/resumo?microchip=900000000022222", tokenTutor(LUCAS))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o tutor vê o histórico completo do próprio animal")
    void tutorVeOHistoricoCompleto() throws Exception {
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/historico", tokenTutor(LUCAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("COMPLETO"))
                .andExpect(jsonPath("$.tutorNome").exists());
    }

    @Test
    @DisplayName("o tutor não vê o histórico do pet de outro tutor")
    void tutorNaoVeHistoricoAlheio() throws Exception {
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenTutor(LUCAS))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("sem consentimento o veterinário fica no nível 1 e vê só a fatia da própria clínica")
    void semConsentimentoFicaNoNivel1() throws Exception {
        // Animal recem-criado, de proposito: os animais do seed acumulam
        // consentimento de outras classes de teste (o AgendamentoFluxoTest
        // agenda para o Bolinha consentindo), e o nivel resultante dependeria da
        // ordem de execucao. Um animal novo nunca teve consentimento concedido.
        String animalId = animalComChip("900000000066666");

        ResultActions historico = buscar("/api/v1/animais/" + animalId + "/historico", tokenVeterinaria());

        historico.andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"))
                // Nome do tutor e nivel 2. Sem consentimento, so o telefone.
                .andExpect(jsonPath("$.tutorNome").doesNotExist());

        // Toda linha visivel e da propria clinica — e a regra C0b: o
        // estabelecimento nunca se tranca contra o proprio registro.
        corpoDe(historico).get("linhaDoTempo").forEach(linha ->
                assertThat(linha.get("destaClinica").asBoolean()).isTrue());
    }

    @Test
    @DisplayName("agendar com consentimento eleva o veterinário ao nível 2")
    void consentimentoNoAgendamentoLiberaOHistorico() throws Exception {
        String admin = tokenAdmin();
        String tutor = tokenTutor(LUCAS);

        // O animal e da Maria: a VetCare nao tem consentimento sobre ele.
        ResultActions antes = buscar(
                "/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenVeterinaria());
        antes.andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"));

        // A Maria agenda na VetCare consentindo.
        String servicoId = criarServico(admin);
        String faixaId = criarGrade(admin);
        LocalDate data = proximaQuarta();

        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(MARIA), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, data));
        marcado.andExpect(status().isCreated());
        String eventoId = idDe(marcado);

        // Agora a mesma rota devolve o historico completo.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenVeterinaria())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("COMPLETO"))
                .andExpect(jsonPath("$.tutorNome").exists());

        // E a Maria enxerga quem ganhou acesso.
        ResultActions minhas = buscar("/api/v1/autorizacoes/minhas", tokenTutor(MARIA));
        minhas.andExpect(status().isOk());
        assertThat(corpoDe(minhas).size()).isGreaterThan(0);

        revogarTudoDaMaria();
        remover("/api/v1/eventos-clinicos/" + eventoId, admin);
        remover("/api/v1/servicos/" + servicoId, admin);
        remover("/api/v1/disponibilidades/" + faixaId, admin);
    }

    @Test
    @DisplayName("agendar recusando o consentimento não libera nada")
    void recusarConsentimentoMantemNivel1() throws Exception {
        String admin = tokenAdmin();
        String servicoId = criarServico(admin);
        String faixaId = criarGrade(admin);

        // Recusar e permitido e nao impede o atendimento: e o que faz o
        // consentimento ser real, e nao um pedagio na tela de agendamento.
        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(MARIA), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"10:00",
                 "consentimentoHistorico":false}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, proximaQuarta()));
        marcado.andExpect(status().isCreated());
        String eventoId = idDe(marcado);

        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenVeterinaria())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"));

        remover("/api/v1/eventos-clinicos/" + eventoId, admin);
        remover("/api/v1/servicos/" + servicoId, admin);
        remover("/api/v1/disponibilidades/" + faixaId, admin);
    }

    @Test
    @DisplayName("o consentimento ausente no corpo é tratado como recusa")
    void consentimentoAusenteERecusa() throws Exception {
        String admin = tokenAdmin();
        String servicoId = criarServico(admin);
        String faixaId = criarGrade(admin);

        // Consentimento pre-marcado nao e consentimento — e o campo ausente nao
        // pode virar "sim" por conveniencia do cliente.
        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(MARIA), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"11:00"}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, proximaQuarta()));
        marcado.andExpect(status().isCreated());
        String eventoId = idDe(marcado);

        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenVeterinaria())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"));

        remover("/api/v1/eventos-clinicos/" + eventoId, admin);
        remover("/api/v1/servicos/" + servicoId, admin);
        remover("/api/v1/disponibilidades/" + faixaId, admin);
    }

    @Test
    @DisplayName("a quebra de vidro exige motivo e fica marcada na auditoria")
    void quebraDeVidro() throws Exception {
        String animalId = animalComChip("900000000033333");

        criar("/api/v1/animais/" + animalId + "/acesso-emergencial", tokenVeterinaria(),
                """
                {"motivo":"Animal atropelado, tutor não localizado"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("COMPLETO"));

        ResultActions acessos = buscar("/api/v1/animais/" + animalId + "/acessos", tokenTutor(LUCAS));
        acessos.andExpect(status().isOk());

        JsonNode emergencial = corpoDe(acessos).get(0);
        assertThat(emergencial.get("emergencial").asBoolean()).isTrue();
        assertThat(emergencial.get("motivo").asText()).contains("atropelado");
    }

    @Test
    @DisplayName("a quebra de vidro sem motivo suficiente é recusada")
    void quebraDeVidroExigeMotivoDeVerdade() throws Exception {
        String animalId = animalComChip("900000000044444");

        // Um campo que aceitasse "x" transformaria a excecao no caminho mais
        // curto, e o consentimento viraria enfeite.
        criar("/api/v1/animais/" + animalId + "/acesso-emergencial", tokenVeterinaria(),
                """
                {"motivo":"urgente"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o tutor não aciona quebra de vidro")
    void tutorNaoQuebraVidro() throws Exception {
        criar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/acesso-emergencial",
                tokenTutor(LUCAS), """
                {"motivo":"Quero ver o histórico agora mesmo"}""")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a auditoria registra uma linha por dia, com contador")
    void auditoriaAgregaPorDia() throws Exception {
        String animalId = animalComChip("900000000055555");
        String vet = tokenVeterinaria();

        buscar("/api/v1/animais/resumo?microchip=900000000055555", vet).andExpect(status().isOk());
        buscar("/api/v1/animais/resumo?microchip=900000000055555", vet).andExpect(status().isOk());
        buscar("/api/v1/animais/resumo?microchip=900000000055555", vet).andExpect(status().isOk());

        JsonNode acessos = corpoDe(buscar("/api/v1/animais/" + animalId + "/acessos", tokenTutor(LUCAS)));

        // Uma linha por requisicao viraria dezenas por atendimento, e a
        // auditoria ficaria ilegivel justamente para quem ela existe.
        assertThat(acessos.size()).isEqualTo(1);
        assertThat(acessos.get(0).get("vezes").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("revogar tira o acesso, e só o tutor do animal pode revogar")
    void revogacao() throws Exception {
        String admin = tokenAdmin();
        String servicoId = criarServico(admin);
        String faixaId = criarGrade(admin);

        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(MARIA), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"14:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, proximaQuarta()));
        marcado.andExpect(status().isCreated());
        String eventoId = idDe(marcado);

        JsonNode minhas = corpoDe(buscar("/api/v1/autorizacoes/minhas", tokenTutor(MARIA)));
        String autorizacaoId = minhas.get(0).get("id").asText();

        // O Lucas nao revoga autorizacao sobre o pet da Maria.
        criar("/api/v1/autorizacoes/" + autorizacaoId + "/revogar", tokenTutor(LUCAS), "")
                .andExpect(status().isConflict());

        criar("/api/v1/autorizacoes/" + autorizacaoId + "/revogar", tokenTutor(MARIA), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOGADA"))
                .andExpect(jsonPath("$.vigente").value(false));

        // O acesso cai na hora.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/historico", tokenVeterinaria())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"));

        remover("/api/v1/eventos-clinicos/" + eventoId, admin);
        remover("/api/v1/servicos/" + servicoId, admin);
        remover("/api/v1/disponibilidades/" + faixaId, admin);
    }

    // ------------------------------------------------------------------

    private String criarServico(String admin) throws Exception {
        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta historico %s","tipoEvento":"CONSULTA",
                 "preco":150.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        return idDe(servico);
    }

    private String criarGrade(String admin) throws Exception {
        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"QUARTA","horaInicio":"08:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        return idDe(faixa);
    }

    private LocalDate proximaQuarta() {
        return LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.WEDNESDAY))
                .plusWeeks(1);
    }

    /** Limpa as autorizacoes da Maria para nao vazarem entre os testes desta classe. */
    private void revogarTudoDaMaria() throws Exception {
        JsonNode minhas = corpoDe(buscar("/api/v1/autorizacoes/minhas", tokenTutor(MARIA)));
        for (JsonNode autorizacao : minhas) {
            if (autorizacao.get("vigente").asBoolean()) {
                criar("/api/v1/autorizacoes/" + autorizacao.get("id").asText() + "/revogar",
                        tokenTutor(MARIA), "");
            }
        }
    }
}
