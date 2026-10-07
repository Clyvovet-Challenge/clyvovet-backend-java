package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeTela;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O fluxo A pela tela: o tutor marca e cancela a propria consulta no navegador.
 *
 * <p>As regras sao as mesmas do {@link AgendamentoFluxoTest} -- o service e um
 * so. O que este teste prova e a parte que so a tela tem: o formulario chega
 * ao service, e a recusa volta como mensagem ao lado do campo certo, e nao
 * como JSON.</p>
 */
class AgendamentoTelaTest extends TesteDeTela {

    private AgendaDeTerca agenda;

    @BeforeEach
    void montarAgenda() throws Exception {
        agenda = montarAgendaDeTerca();
    }

    @Test
    @DisplayName("a lista mostra os agendamentos do tutor com o botao de cancelar")
    void listaMeusAgendamentos() throws Exception {
        String id = agendarPelaApi("09:00");

        abrir("/tutor/agendamentos", LUCAS)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(agenda.servicoNome())))
                .andExpect(content().string(containsString("/tutor/agendamentos/" + id + "/cancelar")));
    }

    @Test
    @DisplayName("o novo agendamento oferece so os pets do proprio tutor")
    void novoOfereceSoOsPetsDoTutor() throws Exception {
        abrir("/tutor/agendamentos/novo", LUCAS)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(SeedV2.ANIMAL_BOLINHA_DO_LUCAS)))
                .andExpect(content().string(not(containsString(SeedV2.ANIMAL_MIMI_DA_MARIA))));
    }

    @Test
    @DisplayName("escolhidos pet e servico, a tela lista as vagas livres da semana")
    void listaAsVagasDoServico() throws Exception {
        abrir("/tutor/agendamentos/novo?animalId=%s&clinicaId=%s&servicoId=%s&de=%s"
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE,
                        agenda.servicoId(), agenda.terca()), LUCAS)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("08:00")))
                .andExpect(content().string(containsString("11:30")));
    }

    @Test
    @DisplayName("agendar numa vaga valida grava e volta a lista com a confirmacao")
    void agendaNumaVagaValida() throws Exception {
        enviarAgendamento(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.terca(), "09:00")
                .andExpect(redirectedUrl("/tutor/agendamentos"))
                .andExpect(flash().attributeExists("sucesso"));

        JsonNode marcado = agendamentoDoServicoPelaApi();
        removerDepois("/api/v1/eventos-clinicos/" + marcado.get("id").asText());
        assertThat(marcado.get("statusEvento").asText()).isEqualTo("AGENDADO");
        assertThat(marcado.get("hora").asText()).isEqualTo("09:00");
    }

    @Test
    @DisplayName("data passada volta ao formulario com o erro ao lado do campo data")
    void dataPassadaMostraErroNoCampo() throws Exception {
        enviarAgendamento(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, LocalDate.now().minusDays(1), "09:00")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("agendamento", "data"))
                .andExpect(content().string(containsString("Não é possível agendar para uma data passada")));
    }

    @Test
    @DisplayName("a recusa da regra de negocio aparece no campo que ela aponta")
    void regraDeNegocioViraErroNoCampo() throws Exception {
        // A grade vai ate as 12:00: as 14:00 a veterinaria nao atende. Quem
        // recusa e o service, e o campo vem da propria RegraDeNegocioException.
        enviarAgendamento(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.terca(), "14:00")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("agendamento", "hora"))
                .andExpect(content().string(containsString("O veterinário não atende neste horário")));
    }

    @Test
    @DisplayName("agendar para o pet de outro tutor e 403")
    void petDeOutroTutorRecebe403() throws Exception {
        // So o status: o 403 vem da cadeia de seguranca como sendError, e quem
        // desenha a error.html a partir dele e o Tomcat -- o MockMvc nao faz
        // esse despacho. A pagina se confere no navegador.
        enviarAgendamento(SeedV2.ANIMAL_MIMI_DA_MARIA, agenda.terca(), "09:00")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cancelar pela tela registra o motivo e volta a lista")
    void cancelaComMotivo() throws Exception {
        String id = agendarPelaApi("10:00");

        enviarFormulario("/tutor/agendamentos/" + id + "/cancelar", comoUsuario(LUCAS),
                "motivo", "Imprevisto de viagem")
                .andExpect(redirectedUrl("/tutor/agendamentos"))
                .andExpect(flash().attributeExists("sucesso"));

        JsonNode evento = corpoDe(buscar("/api/v1/eventos-clinicos/" + id, tokenTutor(LUCAS)));
        assertThat(evento.get("statusEvento").asText()).isEqualTo("CANCELADO");
        assertThat(evento.get("motivoCancelamento").asText()).isEqualTo("Imprevisto de viagem");
    }

    @Test
    @DisplayName("cancelar sem motivo volta ao formulario com o erro no campo")
    void cancelarSemMotivoMostraErro() throws Exception {
        String id = agendarPelaApi("10:30");

        enviarFormulario("/tutor/agendamentos/" + id + "/cancelar", comoUsuario(LUCAS), "motivo", "")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cancelamento", "motivo"));
    }

    @Test
    @DisplayName("agendamento inexistente e 404 na pagina de erro, e nao em JSON")
    void agendamentoInexistenteRecebe404EmHtml() throws Exception {
        // A autorizacao deixa passar o que nao existe (quem responde 404 e o
        // service). Sem restringir o GlobalExceptionHandler ao pacote da API,
        // a tela receberia o JSON de erro dele.
        abrir("/tutor/agendamentos/" + SeedV2.ID_INEXISTENTE + "/cancelar", LUCAS)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    private ResultActions enviarAgendamento(String animalId, LocalDate data, String hora) throws Exception {
        return enviarFormulario("/tutor/agendamentos", comoUsuario(LUCAS),
                "animalId", animalId,
                "clinicaId", SeedV2.CLINICA_VETCARE,
                "servicoId", agenda.servicoId(),
                "veterinarioId", SeedV2.VET_CAMILA,
                "data", data.toString(),
                "hora", hora,
                "consentimentoHistorico", "true");
    }

    private String agendarPelaApi(String hora) throws Exception {
        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"%s"}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.servicoId(), SeedV2.VET_CAMILA,
                        agenda.terca(), hora));
        marcado.andExpect(status().isCreated());
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    /** O servico e criado por teste, entao o unico agendamento dele e o que a tela gravou. */
    private JsonNode agendamentoDoServicoPelaApi() throws Exception {
        for (JsonNode evento : corpoDe(buscar("/api/v1/agendamentos/meus?size=100", tokenTutor(LUCAS))).get("content")) {
            if (agenda.servicoId().equals(evento.get("servicoId").asText())) {
                return evento;
            }
        }
        throw new AssertionError("A tela nao gravou o agendamento");
    }
}
