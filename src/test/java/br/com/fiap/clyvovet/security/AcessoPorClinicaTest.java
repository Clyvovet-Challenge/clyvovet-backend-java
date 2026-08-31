package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A ruptura B1 — o veterinário deixou de enxergar a base inteira.
 *
 * Antes, `temVisaoAmpla()` devolvia `true` para todo VETERINARIO, e
 * `GET /eventos-clinicos` entregava o histórico de atendimento de todas as
 * clínicas da plataforma, inclusive concorrentes.
 *
 * A LINHA QUE SEPAROU AS DUAS COISAS: o **cadastro** do animal continua
 * acessível — é nível 0, e o profissional precisa dele para atender um paciente
 * que chega pela primeira vez. O **atendimento** é registro clínico, e alcança
 * só o da própria clínica (C0b) ou o que o tutor autorizou.
 */
class AcessoPorClinicaTest extends TesteDeApi {

    @Test
    @DisplayName("a listagem de atendimentos é recortada pela clínica do veterinário")
    void listagemRecortadaPorClinica() throws Exception {
        int daCamila = totalDe(buscar("/api/v1/eventos-clinicos", tokenVeterinaria()));
        int total = totalDe(buscar("/api/v1/eventos-clinicos", tokenAdmin()));

        assertThat(daCamila).isPositive().isLessThan(total);

        // Toda linha visível é da VetCare.
        JsonNode pagina = corpoDe(buscar("/api/v1/eventos-clinicos?size=50", tokenVeterinaria()));
        for (JsonNode evento : pagina.get("content")) {
            assertThat(evento.get("clinicaId").asText()).isEqualTo(SeedV2.CLINICA_VETCARE);
        }
    }

    @Test
    @DisplayName("o veterinário não lê o atendimento de outra clínica pelo id")
    void naoLeAtendimentoDeOutraClinica() throws Exception {
        // Animal NOVO, de propósito: os do seed acumulam consentimento de outras
        // classes de teste, e aí a Camila passaria a enxergar o atendimento de
        // fora — o que é o comportamento certo, mas não é o que este teste mede.
        String animalId = animalSemHistorico();

        ResultActions naPetMed = criar("/api/v1/eventos-clinicos", tokenAdmin(), """
                {"data":"%s","hora":"10:00","descricao":"Consulta na concorrente",
                 "tipoEvento":"CONSULTA","veterinarioId":"33333333-3333-3333-3333-000000000002",
                 "animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(2), animalId, SeedV2.CLINICA_PETMED));
        naPetMed.andExpect(status().isCreated());
        String id = idDe(naPetMed);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        buscar("/api/v1/eventos-clinicos/" + id, tokenVeterinaria())
                .andExpect(status().isForbidden());

        // E o ADMIN da plataforma continua alcançando tudo.
        buscar("/api/v1/eventos-clinicos/" + id, tokenAdmin()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("o cadastro do animal continua acessível: é o que o atendimento exige")
    void cadastroDoAnimalContinuaAcessivel() throws Exception {
        // Nível 0. Um paciente que chega pela primeira vez não tem histórico na
        // clínica, e o profissional precisa saber espécie, raça e porte para
        // atendê-lo.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA, tokenVeterinaria())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("o consentimento do tutor devolve o acesso ao atendimento de fora")
    void consentimentoDevolveOAcesso() throws Exception {
        String admin = tokenAdmin();

        // Um atendimento da Mimi na PetMed: fora do alcance da Camila.
        ResultActions naPetMed = criar("/api/v1/eventos-clinicos", admin, """
                {"data":"%s","hora":"11:00","descricao":"Consulta na PetMed",
                 "tipoEvento":"CONSULTA","veterinarioId":"33333333-3333-3333-3333-000000000002",
                 "animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(3), SeedV2.ANIMAL_MIMI_DA_MARIA,
                        SeedV2.CLINICA_PETMED));
        naPetMed.andExpect(status().isCreated());
        String eventoId = idDe(naPetMed);
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);

        buscar("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isForbidden());

        // A Maria agenda na VetCare consentindo — e isso libera o histórico
        // consolidado da Mimi para a clínica, inclusive o que veio de fora.
        String servicoId = criarServico(admin);
        String faixaId = criarGrade(admin);

        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(MARIA), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_MIMI_DA_MARIA, servicoId, SeedV2.VET_CAMILA, proximaSexta()));
        marcado.andExpect(status().isCreated());
        String agendamentoId = idDe(marcado);

        buscar("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isOk());

        // Revoga e o acesso cai na hora.
        JsonNode minhas = corpoDe(buscar("/api/v1/autorizacoes/minhas", tokenTutor(MARIA)));
        for (JsonNode a : minhas) {
            if (a.get("vigente").asBoolean()) {
                criar("/api/v1/autorizacoes/" + a.get("id").asText() + "/revogar",
                        tokenTutor(MARIA), "").andExpect(status().isOk());
            }
        }
        buscar("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isForbidden());

        remover("/api/v1/eventos-clinicos/" + agendamentoId, admin);
        remover("/api/v1/servicos/" + servicoId, admin);
        remover("/api/v1/disponibilidades/" + faixaId, admin);
    }

    @Test
    @DisplayName("o veterinário não registra atendimento em clínica onde não atende")
    void naoRegistraEmOutraClinica() throws Exception {
        // Espelha a regra A3 do agendamento. Sem ela, desde a inversão do
        // acesso o veterinário criaria um evento e ficaria sem conseguir lê-lo.
        criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"10:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(1), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_PETMED))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("evento inexistente é 404, e não 403")
    void inexistenteEh404() throws Exception {
        // A autorização não decide sobre o que não existe. Com 403 aqui, apagar
        // um evento e buscá-lo em seguida sugeriria que ele existe e não é seu.
        buscar("/api/v1/eventos-clinicos/" + SeedV2.ID_INEXISTENTE, tokenVeterinaria())
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------

    /** Um animal do Lucas recém-criado: nenhuma clínica consentiu sobre ele. */
    private String animalSemHistorico() throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                {"nome":"Paciente novo","raca":"SRD","especie":"Canina","porte":"MEDIO",
                 "cor":"Caramelo","sexo":"MACHO","dataNascimento":"2023-05-01",
                 "tutorId":"%s"}""".formatted(SeedV2.TUTOR_LUCAS));
        animal.andExpect(status().isCreated());
        String id = idDe(animal);
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    private String criarServico(String admin) throws Exception {
        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta B1 %s","tipoEvento":"CONSULTA",
                 "preco":150.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        return idDe(servico);
    }

    private String criarGrade(String admin) throws Exception {
        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"SEXTA","horaInicio":"08:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        return idDe(faixa);
    }

    private LocalDate proximaSexta() {
        return LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY))
                .plusWeeks(1);
    }
}
