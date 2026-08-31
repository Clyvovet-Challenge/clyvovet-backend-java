package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Nivel 3 de Richardson — a resposta diz o que se pode fazer com o recurso.
 *
 * O QUE ESTES TESTES DEFENDEM
 * Nao e a presenca de um link "self", que seria decoracao. E a propriedade que
 * torna hipermidia util: os links MUDAM COM O ESTADO. Um evento cancelado nao
 * traz "cancelar", e o frontend nao precisa conhecer a regra para desabilitar o
 * botao — se o link nao veio, a acao nao existe.
 *
 * Sem isso, quem consome a API carrega por fora uma copia da maquina de estados,
 * e essa copia envelhece em silencio no dia em que a regra do servidor muda.
 */
class HateoasTest extends TesteDeApi {

    @Test
    @DisplayName("o contrato antigo continua valendo: os campos seguem na raiz")
    void naoQuebraOContratoExistente() throws Exception {
        // EntityModel serializa o conteudo inline e apenas ACRESCENTA _links.
        // Quem lia "$.id" continua lendo "$.id" — a mudanca e aditiva.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenTutor(LUCAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SeedV2.ANIMAL_BOLINHA_DO_LUCAS))
                .andExpect(jsonPath("$.nome").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("o animal aponta para o histórico clínico e para o tutor")
    void animalLevaAoHistorico() throws Exception {
        // O historico e o objeto que de fato importa clinicamente, e ate aqui so
        // era alcancavel por quem ja soubesse que a rota existia.
        ResultActions animal = buscar(
                "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenTutor(LUCAS));

        animal.andExpect(status().isOk())
                .andExpect(jsonPath("$._links.historico.href").exists())
                .andExpect(jsonPath("$._links.tutor.href").exists());

        assertThat(corpoDe(animal).at("/_links/historico/href").asText())
                .endsWith("/historico");
    }

    @Test
    @DisplayName("o link de acessos só aparece para quem pode segui-lo")
    void linkDeAcessosRespeitaAAutorizacao() throws Exception {
        // Um link que devolveria 403 e pior que link nenhum: desenha um botao
        // que so falha depois do clique.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenTutor(LUCAS))
                .andExpect(jsonPath("$._links.acessos.href").exists());

        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, tokenVeterinaria())
                .andExpect(jsonPath("$._links.acessos").doesNotExist());
    }

    @Test
    @DisplayName("evento AGENDADO oferece cancelar e concluir")
    void agendadoOfereceAsDuasTransicoes() throws Exception {
        String id = agendar();

        buscar("/api/v1/eventos-clinicos/" + id, tokenVeterinaria())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusEvento").value("AGENDADO"))
                .andExpect(jsonPath("$._links.cancelar.href").exists())
                .andExpect(jsonPath("$._links.concluir.href").exists());
    }

    @Test
    @DisplayName("evento CANCELADO não oferece mais nenhuma ação")
    void canceladoEhTerminal() throws Exception {
        String id = agendar();
        criar("/api/v1/agendamentos/" + id + "/cancelar", tokenTutor(LUCAS), """
                {"motivo":"Imprevisto de viagem"}""")
                .andExpect(status().isOk());

        ResultActions evento = buscar("/api/v1/eventos-clinicos/" + id, tokenVeterinaria());

        evento.andExpect(jsonPath("$.statusEvento").value("CANCELADO"))
                // Estado terminal: o cliente nao consegue inventar uma transicao
                // que o servidor recusaria — ele nao ve o caminho.
                .andExpect(jsonPath("$._links.cancelar").doesNotExist())
                .andExpect(jsonPath("$._links.concluir").doesNotExist());

        // Mas a navegacao continua: cancelado nao e invisivel.
        JsonNode links = corpoDe(evento).get("_links");
        assertThat(links.has("self")).isTrue();
        assertThat(links.has("animal")).isTrue();
    }

    @Test
    @DisplayName("os links acompanham a transição do evento")
    void osLinksMudamComOEstado() throws Exception {
        // A propriedade central, verificada de ponta a ponta: o mesmo recurso,
        // duas leituras, conjuntos de acoes diferentes.
        String id = registrarAtendimentoDeOntem();

        JsonNode antes = corpoDe(buscar("/api/v1/eventos-clinicos/" + id, tokenVeterinaria()));
        assertThat(antes.at("/_links/concluir").isMissingNode()).isFalse();

        criar("/api/v1/eventos-clinicos/" + id + "/concluir", tokenVeterinaria(), """
                {"desfecho":"MELHORA","dataRetornoPrevisto":"%s"}"""
                .formatted(LocalDate.now().plusDays(30)))
                .andExpect(status().isOk());

        JsonNode depois = corpoDe(buscar("/api/v1/eventos-clinicos/" + id, tokenVeterinaria()));
        assertThat(depois.at("/_links/concluir").isMissingNode()).isTrue();
        // REALIZADO com retorno previsto abre a proxima transicao do ciclo.
        assertThat(depois.at("/_links/marcar-retorno").isMissingNode()).isFalse();
    }

    // ------------------------------------------------------------------

    private String agendar() throws Exception {
        String admin = tokenAdmin();
        LocalDate quinta = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.THURSDAY)).plusWeeks(1);

        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta hateoas %s","tipoEvento":"CONSULTA",
                 "preco":150.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        removerDepois("/api/v1/servicos/" + idDe(servico));

        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"QUINTA","horaInicio":"08:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}""".formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        removerDepois("/api/v1/disponibilidades/" + idDe(faixa));

        ResultActions marcado = criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":false}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, idDe(servico), SeedV2.VET_CAMILA, quinta));
        marcado.andExpect(status().isCreated());
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    /** Um atendimento com data passada — o caminho do vet que registra o que fez. */
    private String registrarAtendimentoDeOntem() throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(1), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
        evento.andExpect(status().isCreated());
        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }
}
