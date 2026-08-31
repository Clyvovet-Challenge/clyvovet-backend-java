package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
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
 * O fluxo A da spec 08 — o tutor marca a propria consulta.
 *
 * E um dos dois fluxos nao-CRUD exigidos pela Sprint 3, e o que ele tem de
 * diferente dos testes de CRUD e o objeto do teste: nao se verifica que um
 * registro foi gravado, e sim que uma DECISAO foi tomada corretamente — o
 * servico e oferecido? o veterinario atende ali? o horario esta livre?
 *
 * O cenario e montado por API, e nao por SQL: e o que garante que o caminho
 * exercitado seja o mesmo que o frontend vai percorrer.
 */
class AgendamentoFluxoTest extends TesteDeApi {

    private String servicoId;
    private LocalDate proximaTerca;

    /**
     * A grade e montada numa terca futura de proposito.
     *
     * Datas relativas ao "hoje" da execucao evitam o teste que passa hoje e
     * quebra em janeiro — e a antecedencia minima de 2 horas da regra A10
     * tornaria qualquer horario fixo de hoje uma aposta contra o relogio.
     */
    @BeforeEach
    void montarCatalogoEGrade() throws Exception {
        String admin = tokenAdmin();
        proximaTerca = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);

        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta de rotina %s","tipoEvento":"CONSULTA",
                 "preco":180.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        servicoId = idDe(servico);
        removerDepois("/api/v1/servicos/" + servicoId);

        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"TERCA","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        removerDepois("/api/v1/disponibilidades/" + idDe(faixa));
    }

    @Test
    @DisplayName("o tutor marca a consulta e o evento nasce AGENDADO")
    void tutorAgendaDentroDaGrade() throws Exception {
        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(LUCAS), corpoDeAgendamento("09:00"));

        marcado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusEvento").value("AGENDADO"))
                .andExpect(jsonPath("$.tipoEvento").value("CONSULTA"))
                // O valor vem do catalogo: e a peca que faltava para o fluxo de
                // cobranca ter contra o que comparar o pagamento.
                .andExpect(jsonPath("$.valor").value(180.00))
                .andExpect(jsonPath("$.animalId").value(SeedV2.ANIMAL_BOLINHA_DO_LUCAS));

        removerDepois("/api/v1/eventos-clinicos/" + idDe(marcado));
    }

    @Test
    @DisplayName("recusa horário fora da grade do veterinário")
    void recusaForaDaGrade() throws Exception {
        // A grade vai ate as 12:00; as 14:00 a veterinaria nao atende.
        criar("/api/v1/agendamentos", tokenTutor(LUCAS), corpoDeAgendamento("14:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O veterinário não atende neste horário"));
    }

    @Test
    @DisplayName("recusa horário que já tem atendimento marcado")
    void recusaColisaoDeAgenda() throws Exception {
        String tutor = tokenTutor(LUCAS);
        ResultActions primeiro = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("10:00"));
        primeiro.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(primeiro));

        criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("10:00"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("uma consulta de 30 min não bloqueia o horário seguinte")
    void naoBloqueiaOSlotSeguinte() throws Exception {
        // O intervalo e fechado no inicio e aberto no fim: [09:00, 09:30) nao
        // colide com [09:30, 10:00). Sem isso, toda agenda cheia teria um furo
        // artificial entre atendimentos.
        String tutor = tokenTutor(LUCAS);
        ResultActions nove = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("09:00"));
        nove.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(nove));

        ResultActions noveEMeia = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("09:30"));
        noveEMeia.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(noveEMeia));
    }

    @Test
    @DisplayName("férias do veterinário bloqueiam o dia inteiro")
    void recusaDiaBloqueado() throws Exception {
        String admin = tokenAdmin();
        ResultActions ferias = criar("/api/v1/bloqueios", admin, """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Férias"}"""
                .formatted(SeedV2.VET_CAMILA, proximaTerca, proximaTerca.plusDays(5)));
        ferias.andExpect(status().isCreated());
        removerDepois("/api/v1/bloqueios/" + idDe(ferias));

        criar("/api/v1/agendamentos", tokenTutor(LUCAS), corpoDeAgendamento("09:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O veterinário está indisponível neste horário"));
    }

    @Test
    @DisplayName("recusa veterinário que não atende na clínica do serviço")
    void recusaVeterinarioDeOutraClinica() throws Exception {
        // Rafael Matos (seed da V2) atende na PetMed; o servico e da VetCare.
        // Agendar assim seria prometer ao tutor um atendimento que ninguem vai
        // prestar -- e o banco nao pega: nao ha FK ligando servico a veterinario.
        String vetDeOutraClinica = "33333333-3333-3333-3333-000000000002";

        criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, servicoId, vetDeOutraClinica, proximaTerca))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("O veterinário escolhido não atende na clínica deste serviço"));
    }

    @Test
    @DisplayName("tutor não agenda para o pet de outro tutor")
    void recusaAnimalDeOutroTutor() throws Exception {
        // O ownership e o mesmo que ja protege /animais/{id} — nao ha regra
        // nova, ha reuso da que existe.
        criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, proximaTerca))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("recusa agendamento sem a antecedência mínima")
    void recusaAgendamentoEmCimaDaHora() throws Exception {
        // O @Future do DTO nao alcanca este caso: a data e valida, e a hora que
        // ja passou. Data e hora precisam ser avaliadas como um instante so.
        criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"00:01",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, servicoId, SeedV2.VET_CAMILA,
                        LocalDate.now().plusDays(1)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a listagem de vagas devolve os horários livres da grade")
    void listaVagasLivres() throws Exception {
        // Grade de 08:00 as 12:00 com servico de 30 min = 8 vagas.
        ResultActions vagas = buscar(
                "/api/v1/agendamentos/vagas?servicoId=%s&de=%s&ate=%s"
                        .formatted(servicoId, proximaTerca, proximaTerca),
                tokenTutor(LUCAS));

        vagas.andExpect(status().isOk());
        assertThat(corpoDe(vagas).size()).isEqualTo(8);
        assertThat(corpoDe(vagas).get(0).get("horaInicio").asText()).isEqualTo("08:00");
    }

    @Test
    @DisplayName("marcar consulta remove a vaga da listagem")
    void agendarConsomeAVaga() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String url = "/api/v1/agendamentos/vagas?servicoId=%s&de=%s&ate=%s"
                .formatted(servicoId, proximaTerca, proximaTerca);

        int antes = corpoDe(buscar(url, tutor)).size();

        ResultActions marcado = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("11:00"));
        marcado.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(marcado));

        assertThat(corpoDe(buscar(url, tutor)).size()).isEqualTo(antes - 1);
    }

    @Test
    @DisplayName("cancelar exige motivo e preserva o registro")
    void cancelarRegistraOMotivo() throws Exception {
        String tutor = tokenTutor(LUCAS);
        ResultActions marcado = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("08:30"));
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/agendamentos/" + id + "/cancelar", tutor,
                """
                {"motivo":"Imprevisto de viagem"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusEvento").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Imprevisto de viagem"));

        // O evento continua existindo: sem ele a taxa de cancelamento nao seria
        // calculavel e o horario liberado nao teria rastro.
        buscar("/api/v1/eventos-clinicos/" + id, tutor).andExpect(status().isOk());
    }

    @Test
    @DisplayName("cancelar sem motivo é 400")
    void cancelarSemMotivo() throws Exception {
        String tutor = tokenTutor(LUCAS);
        ResultActions marcado = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("11:30"));
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/agendamentos/" + id + "/cancelar", tutor, "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancelar duas vezes é 409")
    void naoCancelaDuasVezes() throws Exception {
        String tutor = tokenTutor(LUCAS);
        ResultActions marcado = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("10:30"));
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        String motivo = """
                {"motivo":"Mudanca de planos"}""";
        criar("/api/v1/agendamentos/" + id + "/cancelar", tutor, motivo).andExpect(status().isOk());
        criar("/api/v1/agendamentos/" + id + "/cancelar", tutor, motivo).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o horário de um agendamento cancelado volta a ficar livre")
    void cancelarLiberaOHorario() throws Exception {
        String tutor = tokenTutor(LUCAS);
        ResultActions primeiro = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("08:00"));
        String id = idDe(primeiro);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        criar("/api/v1/agendamentos/" + id + "/cancelar", tutor,
                """
                {"motivo":"Desisti"}""").andExpect(status().isOk());

        // Se CANCELADO continuasse ocupando a agenda, um unico cancelamento
        // travaria o slot para sempre.
        ResultActions segundo = criar("/api/v1/agendamentos", tutor, corpoDeAgendamento("08:00"));
        segundo.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(segundo));
    }

    @Test
    @DisplayName("o tutor vê apenas os próprios agendamentos")
    void meusAgendamentos() throws Exception {
        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(LUCAS), corpoDeAgendamento("09:00"));
        marcado.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(marcado));

        ResultActions meus = buscar("/api/v1/agendamentos/meus", tokenTutor(LUCAS));
        meus.andExpect(status().isOk());
        assertThat(totalDe(meus)).isGreaterThan(0);

        // Todo item da lista do Lucas e de um pet do Lucas. Verificar a lista
        // inteira, e nao so o primeiro item, e o que faz o teste falhar se o
        // recorte por tutor vazar em qualquer posicao da pagina.
        corpoDe(meus).get("content").forEach(evento ->
                assertThat(evento.get("animalId").asText())
                        .isEqualTo(SeedV2.ANIMAL_BOLINHA_DO_LUCAS));

        // E a Maria nao enxerga nada do Lucas.
        corpoDe(buscar("/api/v1/agendamentos/meus", tokenTutor(MARIA))).get("content").forEach(evento ->
                assertThat(evento.get("animalId").asText())
                        .isNotEqualTo(SeedV2.ANIMAL_BOLINHA_DO_LUCAS));
    }

    @Test
    @DisplayName("recusa serviço desativado")
    void recusaServicoInativo() throws Exception {
        String admin = tokenAdmin();
        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Servico descontinuado %s","tipoEvento":"CONSULTA",
                 "preco":100.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        String id = idDe(servico);

        remover("/api/v1/servicos/" + id, admin).andExpect(status().isNoContent());

        criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, id, SeedV2.VET_CAMILA, proximaTerca))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("Este serviço não está sendo oferecido pela clínica"));
    }

    private String corpoDeAgendamento(String hora) {
        return """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"%s",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, servicoId, SeedV2.VET_CAMILA,
                        proximaTerca, hora);
    }
}
