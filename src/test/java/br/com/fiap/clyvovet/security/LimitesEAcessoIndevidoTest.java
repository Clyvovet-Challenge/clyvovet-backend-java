package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os limites e os acessos cruzados de dinheiro e agenda que a suite ainda nao
 * fixava. O cenario e um atendimento de R$ 200 do Bolinha (Lucas), na VetCare.
 *
 * <p>"Recusado" aqui e 403 ou 404: responder 404 a quem nao pode ver tambem
 * esconde que o recurso existe, e as duas respostas protegem o dado.</p>
 */
class LimitesEAcessoIndevidoTest extends TesteDeApi {

    private String eventoId;
    private String pagamentoId;

    @BeforeEach
    void atendimentoDoLucasComPagamento() throws Exception {
        ResultActions servico = criar("/api/v1/servicos", tokenAdmin(), """
                {"clinicaId":"%s","nome":"Consulta limites %s","tipoEvento":"CONSULTA",
                 "preco":200.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        String servicoId = idDe(servico);
        removerDepois("/api/v1/servicos/" + servicoId);

        String vet = tokenVeterinaria();
        ResultActions evento = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(10), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE, servicoId));
        evento.andExpect(status().isCreated());
        eventoId = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);

        ResultActions pagamento = criar("/api/v1/pagamentos", vet, """
                {"formaPagamento":"PIX","valor":200.00,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}""".formatted(eventoId));
        pagamento.andExpect(status().isCreated());
        pagamentoId = idDe(pagamento);
        removerDepois("/api/v1/pagamentos/" + pagamentoId);
    }

    @Test
    @DisplayName("pagamento de valor zero responde 400")
    void valorZero() throws Exception {
        criar("/api/v1/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":0.00,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}""".formatted(eventoId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o gestor da clinica concorrente nao le o pagamento de outra clinica")
    void gestorDeOutraClinicaNaoLePagamento() throws Exception {
        String petmed = tokenAdminDaClinica("admin.petmed@clinica.test", SeedV2.CLINICA_PETMED);

        int resposta = buscar("/api/v1/pagamentos/" + pagamentoId, petmed)
                .andReturn().getResponse().getStatus();

        assertThat(resposta).isIn(403, 404);
    }

    @Test
    @DisplayName("o tutor nao le o pagamento do atendimento de outro tutor")
    void tutorNaoLePagamentoAlheio() throws Exception {
        int resposta = buscar("/api/v1/pagamentos/" + pagamentoId, tokenTutor(MARIA))
                .andReturn().getResponse().getStatus();

        assertThat(resposta).isIn(403, 404);
    }

    @Test
    @DisplayName("o tutor nao ve o saldo do atendimento de outro tutor")
    void tutorNaoVeSaldoAlheio() throws Exception {
        int resposta = buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", tokenTutor(MARIA))
                .andReturn().getResponse().getStatus();

        assertThat(resposta).isIn(403, 404);
    }

    @Test
    @DisplayName("o tutor nao cancela o agendamento de outro tutor, e o agendamento continua de pe")
    void tutorNaoCancelaAgendamentoAlheio() throws Exception {
        AgendaDeTerca agenda = montarAgendaDeTerca();
        String lucas = tokenTutor(LUCAS);
        ResultActions marcado = criar("/api/v1/agendamentos", lucas, """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.servicoId(), SeedV2.VET_CAMILA, agenda.terca()));
        marcado.andExpect(status().isCreated());
        String id = idDe(marcado);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        int resposta = criar("/api/v1/agendamentos/" + id + "/cancelar", tokenTutor(MARIA), """
                {"motivo":"Cancelando o que nao e meu"}""")
                .andReturn().getResponse().getStatus();

        assertThat(resposta).isIn(403, 404);
        buscar("/api/v1/eventos-clinicos/" + id, lucas)
                .andExpect(jsonPath("$.statusEvento").value("AGENDADO"));
    }
}
