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
 * Regras da spec 08 que a API deixava passar.
 *
 * Cada teste aqui falhou primeiro contra o codigo como ele estava — foi assim
 * que cada furo desta lista foi encontrado, e nao por leitura. Estao juntos
 * porque tem a mesma natureza: nao sao caminhos felizes nem casos de borda, sao
 * as regras que dependiam de ninguem tentar.
 *
 * Duas causas explicavam quase tudo. O statusPagamento sobrevivia no corpo do
 * POST e do PUT, o que tornava P1-P13 decorativas; e a inversao de acesso do B1
 * nunca tinha chegado ao podeAcessarAnimal, que continuava liberando todo
 * VETERINARIO para escrever e para consentir.
 */
class RegrasDeNegocioTest extends TesteDeApi {

    private String eventoId;
    private String servicoId;
    private LocalDate proximaTerca;

    /** Um atendimento realizado na VetCare, com serviço de R$ 200. */
    @BeforeEach
    void atendimentoComPreco() throws Exception {
        String admin = tokenAdmin();
        proximaTerca = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);

        ResultActions servico = criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta regras %s","tipoEvento":"CONSULTA",
                 "preco":200.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servicoId = idDe(servico.andExpect(status().isCreated()));
        removerDepois("/api/v1/servicos/" + servicoId);

        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"TERCA","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}""".formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        removerDepois("/api/v1/disponibilidades/" + idDe(faixa.andExpect(status().isCreated())));

        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(40), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE, servicoId));
        eventoId = idDe(evento.andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);
    }

    // ------------------------------------------------------------------
    // P14 — o status do pagamento nao vem do corpo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("P14: o PUT não muda o status do pagamento")
    void putNaoMudaStatus() throws Exception {
        String id = criarPendente("200.00");

        atualizar("/api/v1/pagamentos/" + id, tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":200.00,"eventoClinicoId":"%s",
                 "statusPagamento":"PAGO"}""".formatted(eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"));
    }

    @Test
    @DisplayName("P7: o POST não cria um pagamento já quitado acima do preço do serviço")
    void postNaoCriaPagoAcimaDoPreco() throws Exception {
        // O caminho curto que anulava o teto: nascer PAGO pulava a soma.
        String id = criarPendente("99999.00");

        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"formaPagamento":"PIX","dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("P11: o PUT não ressuscita um pagamento estornado")
    void putNaoRessuscitaEstornado() throws Exception {
        String id = criarPago("200.00");
        criar("/api/v1/pagamentos/" + id + "/estornar", tokenVeterinaria(),
                "{\"motivo\":\"cliente desistiu do procedimento\"}").andExpect(status().isOk());

        // REEMBOLSADO é terminal. Com o status no corpo, um PUT o devolvia a PAGO.
        atualizar("/api/v1/pagamentos/" + id, tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":200.00,"eventoClinicoId":"%s",
                 "statusPagamento":"PAGO"}""".formatted(eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("REEMBOLSADO"));
    }

    // ------------------------------------------------------------------
    // P9, R19, P12 — dinheiro recebido nao some por DELETE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("P9: pagamento confirmado não é removido, é estornado")
    void pagoNaoERemovido() throws Exception {
        String id = criarPago("200.00");

        remover("/api/v1/pagamentos/" + id, tokenVeterinaria())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("R19: atendimento com pagamento confirmado não é removido")
    void eventoComPagoNaoERemovido() throws Exception {
        criarPago("200.00");

        remover("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("P12: cancelar exige estornar antes")
    void eventoComPagoNaoECancelado() throws Exception {
        String agendadoId = agendarNaProximaTerca("08:30", false);

        String pagamentoId = criarPendenteDe(agendadoId, "200.00");
        criar("/api/v1/pagamentos/" + pagamentoId + "/confirmar", tokenVeterinaria(), """
                {"formaPagamento":"PIX","dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());

        // Cancelar antes do estorno deixaria o valor preso a um atendimento que
        // nao vai acontecer: fora da inadimplencia e fora do painel.
        criar("/api/v1/agendamentos/" + agendadoId + "/cancelar", tokenTutor(LUCAS),
                "{\"motivo\":\"mudei de ideia\"}")
                .andExpect(status().isConflict());

        criar("/api/v1/pagamentos/" + pagamentoId + "/estornar", tokenVeterinaria(),
                "{\"motivo\":\"cancelamento do atendimento\"}").andExpect(status().isOk());
        criar("/api/v1/agendamentos/" + agendadoId + "/cancelar", tokenTutor(LUCAS),
                "{\"motivo\":\"mudei de ideia\"}")
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // R9 e R1 — o retorno tem origem, e a data decide o estado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("R9: RETORNO exige a consulta de origem, pelas três portas")
    void retornoExigeOrigem() throws Exception {
        String vet = tokenVeterinaria();
        String corpo = """
                {"data":"%s","hora":"10:00","descricao":"Retorno solto","tipoEvento":"RETORNO",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(5), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE);

        // Um retorno orfao contava nas metricas sem estar ligado a consulta
        // nenhuma, e a consulta que devia te-lo gerado seguia vencida em R17.
        criar("/api/v1/eventos-clinicos", vet, corpo).andExpect(status().isConflict());

        // A mesma regra pelas portas da edicao.
        atualizar("/api/v1/eventos-clinicos/" + eventoId, vet, corpo).andExpect(status().isConflict());
        atualizarParcialmente("/api/v1/eventos-clinicos/" + eventoId, vet,
                "{\"tipoEvento\":\"RETORNO\"}").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("R1: a data decide o estado inicial do atendimento")
    void dataDecideOEstadoInicial() throws Exception {
        // Passado: o vet esta registrando o que ja fez.
        buscar("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(jsonPath("$.statusEvento").value("REALIZADO"))
                // Mas o prontuario ainda esta aberto: nasceu REALIZADO, nao concluido.
                .andExpect(jsonPath("$.concluidoEm").doesNotExist());

        // E concluir continua possivel, que e o ponto da separacao da V8.
        criar("/api/v1/eventos-clinicos/" + eventoId + "/concluir", tokenVeterinaria(),
                "{\"pesoKg\":9.500,\"desfecho\":\"MELHORA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.concluidoEm").exists());

        // Uma vez fechado, nao se fecha de novo.
        criar("/api/v1/eventos-clinicos/" + eventoId + "/concluir", tokenVeterinaria(), "{}")
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // C10, C14, C17 — o consentimento e o cadastro sao do tutor
    // ------------------------------------------------------------------

    @Test
    @DisplayName("C10: o veterinário não consente pelo tutor")
    void vetNaoConsentePeloTutor() throws Exception {
        String animalId = animalNovoDoLucas("Regra C10");

        // A rota exige podeAcessarAnimal, que libera todo VETERINARIO para que
        // a clinica marque pelo balcao. Sem a regra, o mesmo caminho concedia a
        // ELA MESMA dois anos de acesso ao historico -- sem motivo, sem aviso e
        // sem a marca de quebra de vidro.
        criar("/api/v1/agendamentos", tokenVeterinaria(), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"08:00",
                 "consentimentoHistorico":true}"""
                .formatted(animalId, servicoId, SeedV2.VET_CAMILA, proximaTerca))
                .andExpect(status().isConflict());

        assertThat(temAutorizacaoVigente(animalId)).isFalse();

        // Sem reivindicar consentimento, a clinica marca normalmente.
        ResultActions semConsentir = criar("/api/v1/agendamentos", tokenVeterinaria(), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"08:00"}"""
                .formatted(animalId, servicoId, SeedV2.VET_CAMILA, proximaTerca));
        removerDepois("/api/v1/eventos-clinicos/" + idDe(semConsentir.andExpect(status().isCreated())));
    }

    @Test
    @DisplayName("C14: cancelar antes de qualquer atendimento revoga o consentimento")
    void cancelarSemAtendimentoRevoga() throws Exception {
        String animalId = animalNovoDoLucas("Regra C14");

        ResultActions ag = criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:30",
                 "consentimentoHistorico":true}"""
                .formatted(animalId, servicoId, SeedV2.VET_CAMILA, proximaTerca));
        String agendadoId = idDe(ag.andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + agendadoId);

        assertThat(temAutorizacaoVigente(animalId)).isTrue();

        criar("/api/v1/agendamentos/" + agendadoId + "/cancelar", tokenTutor(LUCAS),
                "{\"motivo\":\"nao vou mais\"}").andExpect(status().isOk());

        // Nunca houve atendimento ali: consentir e cancelar em seguida nao pode
        // comprar dois anos de prontuario.
        assertThat(temAutorizacaoVigente(animalId)).isFalse();
    }

    @Test
    @DisplayName("C17: o veterinário lê o cadastro do animal, mas não o escreve")
    void vetNaoMexeNoCadastroDoAnimal() throws Exception {
        String animalId = animalNovoDoLucas("Regra C17");
        String vet = tokenVeterinaria();

        // Ler e nivel 0: o profissional precisa disso para atender.
        buscar("/api/v1/animais/" + animalId, vet).andExpect(status().isOk());

        // Escrever nao. Pela spec, a correcao vinda do corpo clinico e uma
        // proposta que o tutor confirma -- e apagar nao e correcao nenhuma.
        atualizarParcialmente("/api/v1/animais/" + animalId, vet,
                "{\"nome\":\"Renomeado pela clinica\"}")
                .andExpect(status().isForbidden());
        remover("/api/v1/animais/" + animalId, vet)
                .andExpect(status().isForbidden());

        // O dono escreve.
        atualizarParcialmente("/api/v1/animais/" + animalId, tokenTutor(LUCAS),
                "{\"nome\":\"Renomeado pelo dono\"}")
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------

    private String criarPendente(String valor) throws Exception {
        return criarPendenteDe(eventoId, valor);
    }

    private String criarPendenteDe(String evento, String valor) throws Exception {
        ResultActions p = criar("/api/v1/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":%s,"descricao":"Regras",
                 "eventoClinicoId":"%s"}""".formatted(valor, evento));
        String id = idDe(p.andExpect(status().isCreated()));
        removerDepois("/api/v1/pagamentos/" + id);
        return id;
    }

    private String criarPago(String valor) throws Exception {
        String id = criarPendente(valor);
        criar("/api/v1/pagamentos/" + id + "/confirmar", tokenVeterinaria(), """
                {"formaPagamento":"PIX","dataPagamento":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());
        return id;
    }

    private String agendarNaProximaTerca(String hora, boolean consentindo) throws Exception {
        ResultActions ag = criar("/api/v1/agendamentos", tokenTutor(LUCAS), """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"%s",
                 "consentimentoHistorico":%s}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, servicoId, SeedV2.VET_CAMILA,
                        proximaTerca, hora, consentindo));
        String id = idDe(ag.andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    /**
     * Animal novo a cada teste: os do seed acumulam consentimento das outras
     * classes, e o teste passaria ou falharia conforme a ordem da suite.
     */
    private String animalNovoDoLucas(String nome) throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenAdmin(), """
                {"nome":"%s","especie":"CANINO","raca":"SRD","porte":"MEDIO","cor":"Caramelo",
                 "sexo":"MACHO","dataNascimento":"2021-05-10","tutorId":"%s"}"""
                .formatted(nome, SeedV2.TUTOR_LUCAS));
        String id = idDe(animal.andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    private boolean temAutorizacaoVigente(String animalId) throws Exception {
        for (JsonNode a : corpoDe(buscar("/api/v1/autorizacoes/minhas", tokenTutor(LUCAS)))) {
            if (animalId.equals(a.get("animalId").asText()) && a.get("vigente").asBoolean()) {
                return true;
            }
        }
        return false;
    }
}
