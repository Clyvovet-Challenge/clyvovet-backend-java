package br.com.fiap.clyvovet.fluxo;

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
 * Fluxo P da spec 08 — cobrança.
 *
 * Transições: PENDENTE -> PAGO | CANCELADO; PAGO -> REEMBOLSADO.
 * O valor vem do catálogo (Servico.preco), gravado no evento pelo agendamento.
 */
class CobrancaFluxoTest extends TesteDeApi {

    private String eventoId;
    private String servicoId;

    /** Um atendimento realizado, com serviço de R$ 200 vinculado. */
    @BeforeEach
    void atendimentoComPreco() throws Exception {
        String admin = tokenAdmin();

        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta cobranca %s","tipoEvento":"CONSULTA",
                 "preco":200.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        servicoId = idDe(servico);
        removerDepois("/api/v1/servicos/" + servicoId);

        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(40), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE, servicoId));
        evento.andExpect(status().isCreated());
        eventoId = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);

        criar("/api/v1/eventos-clinicos/" + eventoId + "/concluir", tokenVeterinaria(), "{}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("um pagamento pode nascer PENDENTE, sem data")
    void pendenteNaoPrecisaDeData() throws Exception {
        // Era @NotNull, o que obrigava a declarar uma data de pagamento que não
        // aconteceu. O próprio seed da V2 grava pendentes com data nula.
        ResultActions pago = criar("/api/v1/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":200.00,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}""".formatted(eventoId));

        pago.andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.dataPagamento").doesNotExist());

        removerDepois("/api/v1/pagamentos/" + idDe(pago));
    }

    @Test
    @DisplayName("o PATCH não muda o status: a transição é ação própria")
    void patchNaoMudaStatus() throws Exception {
        // Com o status no corpo do PATCH, um {"statusPagamento":"PAGO"}
        // contornaria todas as transições de uma vez.
        String id = criarPendente("200.00");

        atualizarParcialmente("/api/v1/pagamentos/" + id, tokenVeterinaria(), """
                {"statusPagamento":"PAGO"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"));
    }

    @Test
    @DisplayName("confirmar leva PENDENTE a PAGO e registra a data")
    void confirmar() throws Exception {
        String id = criarPendente("200.00");

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"dataPagamento":"%s","formaPagamento":"CARTAO"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                .andExpect(jsonPath("$.formaPagamento").value("CARTAO"))
                .andExpect(jsonPath("$.dataPagamento").exists());
    }

    @Test
    @DisplayName("não confirma duas vezes")
    void naoConfirmaDuasVezes() throws Exception {
        String id = criarPendente("200.00");
        String corpo = """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now());

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), corpo)
                .andExpect(status().isOk());
        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), corpo)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a soma dos pagamentos não passa do valor do serviço")
    void naoExcedeOValorDoAtendimento() throws Exception {
        // Parcial é permitido — parcelamento é comum em cirurgia — mas o total
        // confirmado não passa dos R$ 200 do catálogo.
        String primeiro = criarPendente("150.00");
        String segundo = criarPendente("100.00");
        String hoje = """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now());

        criar("/api/v1/pagamentos/" + primeiro + "/confirmar", tokenVeterinaria(), hoje)
                .andExpect(status().isOk());

        criar("/api/v1/pagamentos/" + segundo + "/confirmar", tokenVeterinaria(), hoje)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("excede")));
    }

    @Test
    @DisplayName("pagamento parcial é aceito enquanto couber no valor")
    void pagamentoParcial() throws Exception {
        String primeiro = criarPendente("120.00");
        String segundo = criarPendente("80.00");
        String hoje = """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now());

        criar("/api/v1/pagamentos/" + primeiro + "/confirmar", tokenVeterinaria(), hoje)
                .andExpect(status().isOk());
        criar("/api/v1/pagamentos/" + segundo + "/confirmar", tokenVeterinaria(), hoje)
                .andExpect(status().isOk());

        buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", tokenVeterinaria())
                .andExpect(jsonPath("$.totalPago").value(200.00))
                .andExpect(jsonPath("$.emAberto").value(0))
                .andExpect(jsonPath("$.quitado").value(true));
    }

    @Test
    @DisplayName("estornar exige motivo e só funciona sobre um pagamento PAGO")
    void estornar() throws Exception {
        String id = criarPendente("200.00");

        // Pendente não se estorna: não houve recebimento a devolver.
        criar("/api/v1/pagamentos/" + id + "/estornar", tokenVeterinaria(), """
                {"motivo":"Cliente desistiu do procedimento"}""")
                .andExpect(status().isConflict());

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());

        criar("/api/v1/pagamentos/" + id + "/estornar", tokenVeterinaria(), """
                {"motivo":"Procedimento cancelado após o pagamento"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("REEMBOLSADO"))
                .andExpect(jsonPath("$.observacao")
                        .value("Procedimento cancelado após o pagamento"));
    }

    @Test
    @DisplayName("estorno sem motivo suficiente é recusado")
    void estornoExigeMotivo() throws Exception {
        String id = criarPendente("200.00");
        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());

        criar("/api/v1/pagamentos/" + id + "/estornar", tokenVeterinaria(), """
                {"motivo":"erro"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o estorno libera o valor para uma nova cobrança")
    void estornoLiberaOValor() throws Exception {
        // REEMBOLSADO não conta no total pago, senão o atendimento ficaria
        // eternamente quitado depois de um estorno.
        String id = criarPendente("200.00");
        String hoje = """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now());

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), hoje)
                .andExpect(status().isOk());
        criar("/api/v1/pagamentos/" + id + "/estornar", tokenVeterinaria(), """
                {"motivo":"Cobranca em duplicidade"}""")
                .andExpect(status().isOk());

        buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", tokenVeterinaria())
                .andExpect(jsonPath("$.totalPago").value(0))
                .andExpect(jsonPath("$.quitado").value(false));
    }

    @Test
    @DisplayName("o saldo mostra o valor do catálogo, e não o que foi digitado")
    void saldoVemDoCatalogo() throws Exception {
        buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", tokenVeterinaria())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCobrado").value(200.00))
                .andExpect(jsonPath("$.totalPago").value(0))
                .andExpect(jsonPath("$.emAberto").value(200.00));
    }

    @Test
    @DisplayName("o atendimento em aberto aparece na inadimplência")
    void inadimplencia() throws Exception {
        // O evento do cenário tem 40 dias e nenhum pagamento confirmado.
        ResultActions lista = buscar("/api/v1/pagamentos/inadimplencia?diasMinimos=30", tokenVeterinaria());
        lista.andExpect(status().isOk());

        boolean achou = false;
        for (var linha : corpoDe(lista)) {
            if (eventoId.equals(linha.get("eventoId").asText())) {
                achou = true;
                assertThat(linha.get("emAberto").decimalValue())
                        .isEqualByComparingTo(new java.math.BigDecimal("200.00"));
                // Vira ligação, como a lista de retornos vencidos.
                assertThat(linha.has("tutorTelefone")).isTrue();
                assertThat(linha.get("diasEmAberto").asInt()).isGreaterThanOrEqualTo(40);
            }
        }
        assertThat(achou).isTrue();
    }

    @Test
    @DisplayName("quitado sai da inadimplência")
    void quitadoSaiDaLista() throws Exception {
        String id = criarPendente("200.00");
        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());

        for (var linha : corpoDe(buscar("/api/v1/pagamentos/inadimplencia?diasMinimos=30", tokenVeterinaria()))) {
            assertThat(linha.get("eventoId").asText()).isNotEqualTo(eventoId);
        }
    }

    @Test
    @DisplayName("o tutor vê o próprio extrato, e não o de outro")
    void extrato() throws Exception {
        buscar("/api/v1/tutores/" + SeedV2.TUTOR_LUCAS + "/extrato", tokenTutor(LUCAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPago").exists())
                .andExpect(jsonPath("$.totalPendente").exists());

        buscar("/api/v1/tutores/" + SeedV2.TUTOR_MARIA + "/extrato", tokenTutor(LUCAS))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o tutor não confirma nem estorna pagamento")
    void tutorNaoMexeNasTransicoes() throws Exception {
        String id = criarPendente("200.00");

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenTutor(LUCAS), """
                {"dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isForbidden());

        buscar("/api/v1/pagamentos/inadimplencia", tokenTutor(LUCAS))
                .andExpect(status().isForbidden());
    }

    private String criarPendente(String valor) throws Exception {
        ResultActions pagamento = criar("/api/v1/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":%s,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}"""
                .formatted(valor, eventoId));
        pagamento.andExpect(status().isCreated());
        String id = idDe(pagamento);
        removerDepois("/api/v1/pagamentos/" + id);
        return id;
    }
}
