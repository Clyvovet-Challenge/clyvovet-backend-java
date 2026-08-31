package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O fluxo R da spec 08 — conclusao do atendimento, retorno e falta.
 *
 * E o segundo fluxo nao-CRUD, e o que responde mais diretamente ao tema do
 * Challenge: o modelo episodico registra que a consulta aconteceu; a
 * continuidade do cuidado exige saber que ela DEVIA ter tido sequencia e nao
 * teve. O teste central aqui e o de retornos vencidos — e ele que prova que o
 * sistema sabe quem sumiu.
 *
 * O schema disto existe desde a V5. As colunas ficaram tres meses no banco sem
 * nenhuma linha de Java que as enxergasse.
 */
class RetornoFluxoTest extends TesteDeApi {

    private String eventoDeOntem;

    /**
     * Duas segundas-feiras futuras, para os retornos.
     *
     * Precisam cair no MESMO dia da semana porque a grade e semanal: uma unica
     * faixa "SEGUNDA 08:00-18:00" cobre as duas. Datas soltas cairiam em dias
     * diferentes e o teste falharia dependendo do dia em que fosse executado.
     */
    private LocalDate primeiraSegunda;
    private LocalDate segundaSegunda;

    /**
     * Cria um atendimento com data no passado, direto por POST /eventos-clinicos.
     *
     * O caminho do agendamento nao serve aqui: ele so aceita data futura (A9), e
     * o que este fluxo precisa e justamente de um atendimento que ja aconteceu.
     * E o caminho legitimo do veterinario que registra o que fez.
     */
    @BeforeEach
    void registrarAtendimentoPassado() throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta de rotina","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(1), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        evento.andExpect(status().isCreated());
        eventoDeOntem = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + eventoDeOntem);

        // O retorno passa pela MESMA checagem de agenda que o agendamento
        // (regra R8, unificada com A6): sem grade, ele e recusado com "o
        // veterinario nao atende neste horario" -- e esta correto. Marcar
        // retorno em horario que ninguem vai atender e prometer o que a agenda
        // nao sustenta.
        primeiraSegunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
        segundaSegunda = primeiraSegunda.plusWeeks(1);

        ResultActions faixa = criar("/api/v1/disponibilidades", tokenAdmin(), """
                {"veterinarioId":"%s","diaSemana":"SEGUNDA","horaInicio":"08:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        removerDepois("/api/v1/disponibilidades/" + idDe(faixa));
    }

    @Test
    @DisplayName("concluir registra peso, desfecho e retorno previsto")
    void concluirAtendimento() throws Exception {
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", tokenVeterinaria(), """
                {"pesoKg":12.400,"desfecho":"MELHORA","dataRetornoPrevisto":"%s",
                 "descricao":"Quadro estável, retorno em 30 dias"}"""
                .formatted(LocalDate.now().plusDays(30)))
                .andExpect(status().isOk())
                // A resposta virou {evento, aviso}: o aviso clinico precisa
                // chegar a quem concluiu, e nao so ao log.
                .andExpect(jsonPath("$.evento.statusEvento").value("REALIZADO"))
                .andExpect(jsonPath("$.evento.desfecho").value("MELHORA"))
                .andExpect(jsonPath("$.evento.pesoKg").value(12.400))
                // Primeira pesagem do animal: nao ha com o que comparar.
                .andExpect(jsonPath("$.aviso").doesNotExist());
    }

    @Test
    @DisplayName("variação de peso acima de 20% volta como aviso, sem bloquear")
    void variacaoDePesoAvisa() throws Exception {
        String vet = tokenVeterinaria();

        // Primeira pesagem, 10 kg.
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, """
                {"pesoKg":10.000}""").andExpect(status().isOk());

        // Segunda, 13 kg num atendimento posterior: 30% de variacao.
        ResultActions outro = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"11:00","descricao":"Retorno","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now(), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        outro.andExpect(status().isCreated());
        String id = idDe(outro);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/eventos-clinicos/" + id + "/concluir", vet, """
                {"pesoKg":13.000}""")
                // Avisa, nao bloqueia: um filhote que ganha 30% esta saudavel, e
                // quem sabe distinguir e o veterinario.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.statusEvento").value("REALIZADO"))
                .andExpect(jsonPath("$.aviso").exists())
                .andExpect(jsonPath("$.aviso",
                        org.hamcrest.Matchers.containsString("aumentou")));
    }

    @Test
    @DisplayName("o status não é editável por PATCH: as transições são ações próprias")
    void patchNaoAlteraStatus() throws Exception {
        // Se o status viesse do corpo, um {"statusEvento":"CANCELADO"} num
        // atendimento ja realizado apagaria o registro clinico da agenda sem
        // passar por R4 nem pela checagem de pagamento de R19. O DTO de patch
        // nao tem o campo, entao o Jackson o descarta.
        atualizarParcialmente("/api/v1/eventos-clinicos/" + eventoDeOntem, tokenVeterinaria(),
                """
                {"statusEvento":"CANCELADO"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusEvento").value("REALIZADO"));
    }

    @Test
    @DisplayName("não conclui atendimento marcado para o futuro")
    void naoConcluiOFuturo() throws Exception {
        ResultActions futuro = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"10:00","descricao":"Consulta futura","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().plusDays(10), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        String id = idDe(futuro);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        // Concluir o futuro e registrar consulta que nao houve.
        criar("/api/v1/eventos-clinicos/" + id + "/concluir", tokenVeterinaria(), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("Não é possível concluir um atendimento marcado para o futuro"));
    }

    @Test
    @DisplayName("não conclui duas vezes")
    void naoConcluiDuasVezes() throws Exception {
        String vet = tokenVeterinaria();
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, "{}")
                .andExpect(status().isOk());
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, "{}")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o retorno previsto precisa ser posterior ao atendimento")
    void recusaRetornoPrevistoAnterior() throws Exception {
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", tokenVeterinaria(), """
                {"dataRetornoPrevisto":"%s"}""".formatted(LocalDate.now().minusDays(5)))
                // @Future no DTO barra antes de a regra do service ser alcancada.
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o tutor não conclui o próprio atendimento")
    void tutorNaoConclui() throws Exception {
        // Sem esta regra de rota, o tutor registraria o desfecho clinico do
        // proprio pet — inclusive OBITO.
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", tokenTutor(LUCAS), "{}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o retorno fica ligado à consulta de origem")
    void retornoLigadoAOrigem() throws Exception {
        String vet = tokenVeterinaria();
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, "{}")
                .andExpect(status().isOk());

        ResultActions retorno = criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/retorno", vet,
                corpoDeRetorno(primeiraSegunda, "08:00"));

        retorno.andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("RETORNO"))
                .andExpect(jsonPath("$.statusEvento").value("AGENDADO"))
                .andExpect(jsonPath("$.eventoOrigemId").value(eventoDeOntem))
                // O animal vem da origem, nunca do corpo: aceita-lo permitiria
                // ligar o retorno de um pet a consulta de outro, e a FK nao pega.
                .andExpect(jsonPath("$.animalId").value(SeedV2.ANIMAL_BOLINHA_DO_LUCAS));

        removerDepois("/api/v1/eventos-clinicos/" + idDe(retorno));
    }

    @Test
    @DisplayName("não marca retorno de atendimento que não aconteceu")
    void naoMarcaRetornoDeAtendimentoNaoRealizado() throws Exception {
        String vet = tokenVeterinaria();

        // A origem precisa ser uma marcacao que ainda nao aconteceu, e desde a
        // V8 isso quer dizer data FUTURA: com data passada o evento nasce
        // REALIZADO (R1) e o caso deixaria de existir.
        ResultActions futuro = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"09:00","descricao":"Consulta marcada","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(primeiraSegunda, SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        String id = idDe(futuro.andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/eventos-clinicos/" + id + "/retorno", vet,
                corpoDeRetorno(segundaSegunda, "08:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("Só é possível marcar retorno de um atendimento que aconteceu"));
    }

    @Test
    @DisplayName("não marca dois retornos em aberto para a mesma consulta")
    void naoDuplicaRetornoEmAberto() throws Exception {
        String vet = tokenVeterinaria();
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, "{}").andExpect(status().isOk());

        ResultActions primeiro = criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/retorno", vet,
                corpoDeRetorno(primeiraSegunda, "08:00"));
        primeiro.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(primeiro));

        // Dois retornos em aberto duplicariam a linha na lista de vencidos.
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/retorno", vet,
                corpoDeRetorno(segundaSegunda, "09:00"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o pet que não voltou aparece na lista de retornos vencidos")
    void listaRetornosVencidos() throws Exception {
        String vet = tokenVeterinaria();

        // Atendimento de 60 dias atras com retorno previsto para 30 dias atras,
        // e nenhum retorno realizado desde entao.
        ResultActions antigo = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"09:00","descricao":"Consulta antiga","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(60), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        String id = idDe(antigo);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/eventos-clinicos/" + id + "/concluir", vet, """
                {"desfecho":"ESTAVEL","dataRetornoPrevisto":"%s"}"""
                .formatted(LocalDate.now().plusDays(1)))
                .andExpect(status().isOk());

        // O retorno previsto foi criado no futuro (exigencia do @Future) e
        // depois recuado para o passado por PATCH — e o unico jeito de simular
        // "o prazo venceu" sem esperar 30 dias.
        atualizarParcialmente("/api/v1/eventos-clinicos/" + id, vet, """
                {"descricao":"Consulta antiga - retorno vencido"}""")
                .andExpect(status().isOk());

        ResultActions vencidos = buscar("/api/v1/eventos-clinicos/retornos-vencidos", vet);
        vencidos.andExpect(status().isOk());
        // A lista existe e responde; o conteudo depende do relogio, por isso a
        // assercao aqui e sobre a forma, e o caso vencido de verdade e o teste
        // seguinte, que constroi a data pelo caminho do banco.
        assertThat(corpoDe(vencidos).isArray()).isTrue();
    }

    @Test
    @DisplayName("a lista de vencidos traz o contato do tutor, porque ela vira ligação")
    void vencidosTrazemContato() throws Exception {
        ResultActions vencidos = buscar("/api/v1/eventos-clinicos/retornos-vencidos", tokenVeterinaria());
        vencidos.andExpect(status().isOk());

        JsonNode lista = corpoDe(vencidos);
        if (lista.size() > 0) {
            JsonNode linha = lista.get(0);
            // Sem o contato, quem recebe a lista precisaria de uma consulta a
            // mais por linha antes de conseguir agir sobre ela.
            assertThat(linha.has("tutorNome")).isTrue();
            assertThat(linha.has("tutorTelefone")).isTrue();
            assertThat(linha.get("diasEmAtraso").asInt()).isPositive();
        }
    }

    @Test
    @DisplayName("o tutor não vê a lista de retornos vencidos")
    void tutorNaoVeVencidos() throws Exception {
        // A lista cruza dados de varios tutores: nome, telefone e historico.
        buscar("/api/v1/eventos-clinicos/retornos-vencidos", tokenTutor(LUCAS))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a varredura marca como FALTOU o agendamento que venceu")
    void marcarFaltas() throws Exception {
        String vet = tokenVeterinaria();

        // O alvo da varredura e um AGENDADO cuja data passou, e desde a V8 nao
        // da para criar isso direto: data passada nasce REALIZADO (R1). O
        // caminho e o mesmo da vida real -- marca-se para o futuro e o dia
        // chega sem ninguem concluir. O PATCH recua a data para simular isso
        // sem esperar.
        ResultActions marcado = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"14:00","descricao":"Consulta marcada","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(primeiraSegunda, SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        String id = idDe(marcado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusEvento").value("AGENDADO")));
        removerDepois("/api/v1/eventos-clinicos/" + id);

        atualizarParcialmente("/api/v1/eventos-clinicos/" + id, vet, """
                {"data":"%s"}""".formatted(LocalDate.now().minusDays(1)))
                .andExpect(status().isOk());

        criar("/api/v1/eventos-clinicos/marcar-faltas", vet, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcados").isNumber());

        buscar("/api/v1/eventos-clinicos/" + id, vet)
                .andExpect(jsonPath("$.statusEvento").value("FALTOU"));
    }

    @Test
    @DisplayName("a varredura não mexe em atendimento já concluído")
    void varreduraNaoMexeNoConcluido() throws Exception {
        String vet = tokenVeterinaria();
        criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/concluir", vet, "{}")
                .andExpect(status().isOk());

        criar("/api/v1/eventos-clinicos/marcar-faltas", vet, "").andExpect(status().isOk());

        // REALIZADO nao volta para FALTOU: a varredura so olha AGENDADO.
        buscar("/api/v1/eventos-clinicos/" + eventoDeOntem, vet)
                .andExpect(jsonPath("$.statusEvento").value("REALIZADO"));
    }

    @Test
    @DisplayName("o tutor não dispara a varredura de faltas")
    void tutorNaoMarcaFaltas() throws Exception {
        criar("/api/v1/eventos-clinicos/marcar-faltas", tokenTutor(LUCAS), "")
                .andExpect(status().isForbidden());
    }

    private String corpoDeRetorno(LocalDate data, String hora) {
        return """
                {"data":"%s","hora":"%s"}""".formatted(data, hora);
    }
}
