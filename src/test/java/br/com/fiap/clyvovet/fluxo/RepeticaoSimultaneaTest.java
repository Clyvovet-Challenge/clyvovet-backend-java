package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.Simultaneo;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O mesmo pedido chegando mais de uma vez AO MESMO TEMPO.
 *
 * <p>Os testes "duas vezes" que ja existem (CobrancaFluxoTest,
 * AgendamentoFluxoTest) mandam um pedido depois do outro: o segundo le o que o
 * primeiro gravou e e recusado. Aqui os pedidos saem juntos -- o duplo toque no
 * botao, o app que reenvia por timeout -- e cada um le o estado ANTES de o outro
 * gravar. E a janela que so uma trava no banco (lock, versao ou UNIQUE) fecha.</p>
 *
 * <p>Cada cenario dispara {@link #PEDIDOS} chamadas presas numa largada comum.
 * Corrida e probabilistica: um teste verde aqui nao prova que a janela nao
 * existe, mas um vermelho prova que existe.</p>
 *
 * <p>Roda no H2 do perfil dev. O MySQL da Render tem outro isolamento e outras
 * travas, entao o resultado la pode ser diferente -- para pior ou para melhor.</p>
 */
class RepeticaoSimultaneaTest extends TesteDeApi {

    private static final int PEDIDOS = 4;

    @Nested
    @DisplayName("pagamento")
    class Pagamento {

        @Test
        @DisplayName("confirmacoes simultaneas do mesmo pagamento: so uma vira PAGO, as outras sao 409")
        void confirmarAoMesmoTempo() throws Exception {
            String eventoId = atendimentoDeDuzentos();
            String id = criarPendente(eventoId, "200.00");
            String vet = tokenVeterinaria();
            String corpo = """
                    {"dataPagamento":"%s"}""".formatted(LocalDate.now());

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    enviar("/api/v1/pagamentos/" + id + "/confirmar", vet, corpo)));

            assertThat(respostas).containsOnly(200, 409);
            assertThat(respostas).filteredOn(s -> s == 200).hasSize(1);
        }

        @Test
        @DisplayName("confirmacoes simultaneas que juntas passam do valor do servico: so cabe o que o servico vale")
        void confirmarPagamentosQueJuntosExcedem() throws Exception {
            // R$ 150 + R$ 100 sobre um servico de R$ 200. Em sequencia, o segundo e
            // 409 (CobrancaFluxoTest.naoExcedeOValorDoAtendimento).
            String eventoId = atendimentoDeDuzentos();
            String primeiro = criarPendente(eventoId, "150.00");
            String segundo = criarPendente(eventoId, "100.00");
            String vet = tokenVeterinaria();
            String corpo = """
                    {"dataPagamento":"%s"}""".formatted(LocalDate.now());

            List<Integer> respostas = statusDe(aoMesmoTempo(List.of(
                    () -> enviar("/api/v1/pagamentos/" + primeiro + "/confirmar", vet, corpo),
                    () -> enviar("/api/v1/pagamentos/" + segundo + "/confirmar", vet, corpo))));

            assertThat(respostas).containsExactlyInAnyOrder(200, 409);
            JsonNode saldo = corpoDe(buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", vet));
            assertThat(saldo.get("totalPago").decimalValue())
                    .isLessThanOrEqualTo(new java.math.BigDecimal("200.00"));
        }

        @Test
        @DisplayName("estornos simultaneos do mesmo pagamento: so um e aplicado, os outros sao 409")
        void estornarAoMesmoTempo() throws Exception {
            String eventoId = atendimentoDeDuzentos();
            String id = criarPendente(eventoId, "200.00");
            String vet = tokenVeterinaria();
            criar("/api/v1/pagamentos/" + id + "/confirmar", vet, """
                    {"dataPagamento":"%s"}""".formatted(LocalDate.now()))
                    .andExpect(status().isOk());

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    enviar("/api/v1/pagamentos/" + id + "/estornar", vet, """
                            {"motivo":"Cobranca em duplicidade"}""")));

            assertThat(respostas).containsOnly(200, 409);
            assertThat(respostas).filteredOn(s -> s == 200).hasSize(1);
            JsonNode saldo = corpoDe(buscar("/api/v1/eventos-clinicos/" + eventoId + "/saldo", vet));
            assertThat(saldo.get("totalPago").decimalValue()).isZero();
        }
    }

    @Nested
    @DisplayName("agendamento")
    class Agendamento {

        @Test
        @DisplayName("dois tutores marcam o mesmo horario ao mesmo tempo: so um agendamento e gravado, o outro e 409")
        void doisTutoresNoMesmoHorario() throws Exception {
            AgendaDeTerca agenda = montarAgendaDeTerca();
            String lucas = tokenTutor(LUCAS);
            // O segundo tutor e uma conta nova, e nao a Maria: agendar com
            // consentimento abre o historico do pet para a clinica, e outros
            // testes contam com a Mimi nunca ter passado pela VetCare.
            TutorNovo outro = tutorNovoComPet();

            List<MockHttpServletResponse> respostas = aoMesmoTempo(List.of(
                    () -> enviar("/api/v1/agendamentos", lucas,
                            agendamento(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda, "09:00")),
                    () -> enviar("/api/v1/agendamentos", outro.token(),
                            agendamento(outro.animalId(), agenda, "09:00"))));
            removerOsCriados(respostas);

            assertThat(statusDe(respostas)).containsExactlyInAnyOrder(201, 409);
        }

        @Test
        @DisplayName("o mesmo tutor envia o agendamento varias vezes ao mesmo tempo: so um agendamento existe")
        void duploToque() throws Exception {
            AgendaDeTerca agenda = montarAgendaDeTerca();
            String lucas = tokenTutor(LUCAS);
            String corpo = agendamento(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda, "10:00");

            List<MockHttpServletResponse> respostas = aoMesmoTempo(() ->
                    enviar("/api/v1/agendamentos", lucas, corpo));
            removerOsCriados(respostas);

            List<Integer> status = statusDe(respostas);
            assertThat(status).containsOnly(201, 409);
            assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        }

        private String agendamento(String animalId, AgendaDeTerca agenda, String hora) {
            return """
                    {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"%s",
                     "consentimentoHistorico":true}"""
                    .formatted(animalId, agenda.servicoId(), SeedV2.VET_CAMILA, agenda.terca(), hora);
        }
    }

    @Nested
    @DisplayName("cadastro")
    class Cadastro {

        @Test
        @DisplayName("cadastros simultaneos com o mesmo e-mail: uma conta so, os outros sao 409 e nunca 500")
        void mesmoEmailAoMesmoTempo() throws Exception {
            String corpo = """
                    {"email":"corrida.%d@teste.com","senha":"senha12345","nome":"Tutor Apressado"}"""
                    .formatted(System.nanoTime());

            List<Integer> respostas = statusDe(aoMesmoTempo(() -> mockMvc.perform(post("/api/v1/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andReturn().getResponse()));

            assertThat(respostas).containsOnly(201, 409);
            assertThat(respostas).filteredOn(s -> s == 201).hasSize(1);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("cinco senhas erradas ao mesmo tempo contam como cinco: a conta bloqueia")
        void falhasSimultaneasBloqueiam() throws Exception {
            // Com o contador lido e somado em Java, as cinco liam o mesmo valor e
            // gravavam +1: a conta saia com uma falha e o atacante seguia tentando.
            String email = "alvo.%d@teste.com".formatted(System.nanoTime());
            mockMvc.perform(post("/api/v1/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","senha":"senha12345","nome":"Alvo"}""".formatted(email)))
                    .andExpect(status().isCreated());

            List<Integer> respostas = statusDe(aoMesmoTempo(5, () -> login(email, "senhaErrada999")));
            assertThat(respostas).containsOnly(401);

            assertThat(login(email, "senha12345").getStatus()).isEqualTo(401);
        }

        private MockHttpServletResponse login(String email, String senha) throws Exception {
            return mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","senha":"%s"}""".formatted(email, senha)))
                    .andReturn().getResponse();
        }
    }

    @Nested
    @DisplayName("pedido de alteracao de cadastro")
    class PedidoDeAlteracao {

        private static final String PEDIDO = """
                {"justificativa":"Confirmei o microchip e a raça na consulta de hoje.",
                 "raca":"Poodle Toy"}""";
        private final String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/solicitacoes-alteracao";

        @Test
        @DisplayName("o veterinario envia o mesmo pedido varias vezes ao mesmo tempo: so um fica pendente")
        void duploCliqueDoVeterinario() throws Exception {
            String vet = tokenVeterinaria();

            List<MockHttpServletResponse> respostas = aoMesmoTempo(() -> enviar(url, vet, PEDIDO));
            recusarOsPendentesDoLucas();

            List<Integer> status = statusDe(respostas);
            assertThat(status).containsOnly(201, 409);
            assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        }

        @Test
        @DisplayName("o tutor responde o mesmo pedido varias vezes ao mesmo tempo: so uma resposta vale")
        void tutorRespondeAoMesmoTempo() throws Exception {
            recusarOsPendentesDoLucas();
            String pedido = idDe(criar(url, tokenVeterinaria(), PEDIDO).andExpect(status().isCreated()));
            String lucas = tokenTutor(LUCAS);

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    enviar("/api/v1/solicitacoes-alteracao/" + pedido + "/recusar", lucas, """
                            {"motivo":"Nao confere com o meu cadastro."}""")));

            assertThat(respostas).containsOnly(200, 409);
            assertThat(respostas).filteredOn(s -> s == 200).hasSize(1);
        }

        /** Os testes de pedido contam com a caixa do Lucas vazia (SolicitacaoAlteracaoFluxoTest). */
        private void recusarOsPendentesDoLucas() throws Exception {
            String lucas = tokenTutor(LUCAS);
            for (JsonNode pendente : corpoDe(buscar("/api/v1/solicitacoes-alteracao/minhas", lucas))) {
                criar("/api/v1/solicitacoes-alteracao/" + pendente.get("id").asText() + "/recusar", lucas, """
                        {"motivo":"Limpeza entre testes."}""");
            }
        }
    }

    @Nested
    @DisplayName("atendimento")
    class Atendimento {

        @Test
        @DisplayName("varios retornos da mesma consulta pedidos ao mesmo tempo: so um e marcado")
        void retornoAoMesmoTempo() throws Exception {
            String vet = tokenVeterinaria();
            String origem = consultaDeOntem(vet);
            criar("/api/v1/eventos-clinicos/" + origem + "/concluir", vet, "{}").andExpect(status().isOk());
            LocalDate segunda = gradeDeSegunda();

            List<MockHttpServletResponse> respostas = aoMesmoTempo(() ->
                    enviar("/api/v1/eventos-clinicos/" + origem + "/retorno", vet, """
                            {"data":"%s","hora":"08:00"}""".formatted(segunda)));
            removerOsCriados(respostas);

            List<Integer> status = statusDe(respostas);
            assertThat(status).containsOnly(201, 409);
            assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        }

        @Test
        @DisplayName("varias conclusoes do mesmo atendimento ao mesmo tempo: so uma vale")
        void concluirAoMesmoTempo() throws Exception {
            String vet = tokenVeterinaria();
            String evento = consultaDeOntem(vet);

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    enviar("/api/v1/eventos-clinicos/" + evento + "/concluir", vet, "{}")));

            assertThat(respostas).containsOnly(200, 409);
            assertThat(respostas).filteredOn(s -> s == 200).hasSize(1);
        }

        @Test
        @DisplayName("varios cancelamentos do mesmo agendamento ao mesmo tempo: so um vale")
        void cancelarAoMesmoTempo() throws Exception {
            AgendaDeTerca agenda = montarAgendaDeTerca();
            String lucas = tokenTutor(LUCAS);
            ResultActions marcado = criar("/api/v1/agendamentos", lucas, """
                    {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"11:30",
                     "consentimentoHistorico":true}"""
                    .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.servicoId(), SeedV2.VET_CAMILA, agenda.terca()));
            marcado.andExpect(status().isCreated());
            String id = idDe(marcado);
            removerDepois("/api/v1/eventos-clinicos/" + id);

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    enviar("/api/v1/agendamentos/" + id + "/cancelar", lucas, """
                            {"motivo":"Imprevisto"}""")));

            assertThat(respostas).containsOnly(200, 409);
            assertThat(respostas).filteredOn(s -> s == 200).hasSize(1);
        }

        @Test
        @DisplayName("o mesmo pet agendado com dois veterinarios da clinica ao mesmo tempo: os dois agendamentos entram")
        void consentimentoSimultaneoNaMesmaClinica() throws Exception {
            // Os dois agendamentos criavam a autorizacao do pet na clinica; o UNIQUE
            // derrubava um, e o agendamento ia junto. Nao e conflito de agenda --
            // sao veterinarios diferentes --, entao os dois devem entrar.
            AgendaDeTerca agenda = montarAgendaDeTerca();
            ResultActions faixa = criar("/api/v1/disponibilidades", tokenAdmin(), """
                    {"veterinarioId":"%s","diaSemana":"TERCA","horaInicio":"08:00","horaFim":"12:00",
                     "vigenciaInicio":"%s"}"""
                    .formatted(SeedV2.VET_TOMAS_DA_VETCARE, LocalDate.now().minusDays(1)));
            faixa.andExpect(status().isCreated());
            removerDepois("/api/v1/disponibilidades/" + idDe(faixa));
            TutorNovo tutor = tutorNovoComPet();

            List<MockHttpServletResponse> respostas = aoMesmoTempo(List.of(
                    () -> enviar("/api/v1/agendamentos", tutor.token(),
                            agendamento(tutor.animalId(), agenda, SeedV2.VET_CAMILA)),
                    () -> enviar("/api/v1/agendamentos", tutor.token(),
                            agendamento(tutor.animalId(), agenda, SeedV2.VET_TOMAS_DA_VETCARE))));
            removerOsCriados(respostas);

            assertThat(statusDe(respostas)).containsOnly(201);
        }

        private String agendamento(String animalId, AgendaDeTerca agenda, String veterinarioId) {
            return """
                    {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"09:00",
                     "consentimentoHistorico":true}"""
                    .formatted(animalId, agenda.servicoId(), veterinarioId, agenda.terca());
        }

        private String consultaDeOntem(String vet) throws Exception {
            ResultActions evento = criar("/api/v1/eventos-clinicos", vet, """
                    {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                     "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                    .formatted(LocalDate.now().minusDays(1), SeedV2.VET_CAMILA,
                            SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE));
            evento.andExpect(status().isCreated());
            String id = idDe(evento);
            removerDepois("/api/v1/eventos-clinicos/" + id);
            return id;
        }

        /** Grade da Camila numa segunda futura, como no RetornoFluxoTest. */
        private LocalDate gradeDeSegunda() throws Exception {
            ResultActions faixa = criar("/api/v1/disponibilidades", tokenAdmin(), """
                    {"veterinarioId":"%s","diaSemana":"SEGUNDA","horaInicio":"08:00","horaFim":"18:00",
                     "vigenciaInicio":"%s"}"""
                    .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
            faixa.andExpect(status().isCreated());
            removerDepois("/api/v1/disponibilidades/" + idDe(faixa));
            return LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                    .plusWeeks(3);
        }
    }

    @Nested
    @DisplayName("historico e documentos")
    class HistoricoEDocumentos {

        @Test
        @DisplayName("varias leituras do resumo de um animal novo ao mesmo tempo: todas respondem, numa linha de auditoria")
        void primeiroAcessoAoMesmoTempo() throws Exception {
            // As primeiras leituras do dia inseriam todas a linha de auditoria, e o
            // UNIQUE uk_acesso_dia derrubava as outras com 500.
            String chip = "9%014d".formatted(System.nanoTime() % 100_000_000_000_000L);
            ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                    {"nome":"Paciente %s","raca":"SRD","especie":"Canina","porte":"MEDIO",
                     "cor":"Caramelo","sexo":"MACHO","dataNascimento":"2022-01-10",
                     "tutorId":"%s","microchip":"%s"}"""
                    .formatted(chip, SeedV2.TUTOR_LUCAS, chip));
            animal.andExpect(status().isCreated());
            String animalId = idDe(animal);
            removerDepois("/api/v1/animais/" + animalId);
            String vet = tokenVeterinaria();

            List<Integer> respostas = statusDe(aoMesmoTempo(() ->
                    buscar("/api/v1/animais/resumo?microchip=" + chip, vet).andReturn().getResponse()));

            assertThat(respostas).containsOnly(200);
            JsonNode acessos = corpoDe(buscar("/api/v1/animais/" + animalId + "/acessos", tokenTutor(LUCAS)));
            assertThat(acessos.size()).isEqualTo(1);
            assertThat(acessos.get(0).get("vezes").asInt()).isEqualTo(PEDIDOS);
        }

        @Test
        @DisplayName("o mesmo arquivo anexado varias vezes ao mesmo tempo: entra uma vez so")
        void mesmoArquivoAoMesmoTempo() throws Exception {
            String lucas = tokenTutor(LUCAS);
            ResultActions animal = criar("/api/v1/animais", lucas, """
                    {"nome":"Anexos","raca":"SRD","especie":"Canina","porte":"MEDIO",
                     "cor":"Preto","sexo":"MACHO","dataNascimento":"2021-04-10","tutorId":"%s"}"""
                    .formatted(SeedV2.TUTOR_LUCAS));
            animal.andExpect(status().isCreated());
            String animalId = idDe(animal);
            removerDepois("/api/v1/animais/" + animalId);
            byte[] pdf = ("%PDF-1.4\nlaudo " + System.nanoTime() + "\n%%EOF\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);

            List<MockHttpServletResponse> respostas = aoMesmoTempo(() -> mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .multipart("/api/v1/animais/" + animalId + "/documentos")
                                    .file(new org.springframework.mock.web.MockMultipartFile(
                                            "arquivo", "laudo.pdf", "application/pdf", pdf))
                                    .param("titulo", "Laudo")
                                    .header("Authorization", "Bearer " + lucas))
                    .andReturn().getResponse());
            for (MockHttpServletResponse r : respostas) {
                if (r.getStatus() == 201) {
                    String id = objectMapper.readTree(r.getContentAsString()).get("id").asText();
                    removerDepois("/api/v1/animais/" + animalId + "/documentos/" + id);
                }
            }

            List<Integer> status = statusDe(respostas);
            assertThat(status).containsOnly(201, 409);
            assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        }
    }

    // ================================================================
    // Cenario e disparo
    // ================================================================

    /** Um atendimento realizado da Camila, com servico de R$ 200 -- o mesmo do CobrancaFluxoTest. */
    private String atendimentoDeDuzentos() throws Exception {
        ResultActions servico = criar("/api/v1/servicos", tokenAdmin(), """
                {"clinicaId":"%s","nome":"Consulta corrida %s","tipoEvento":"CONSULTA",
                 "preco":200.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        String servicoId = idDe(servico);
        removerDepois("/api/v1/servicos/" + servicoId);

        String vet = tokenVeterinaria();
        ResultActions evento = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(40), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE, servicoId));
        evento.andExpect(status().isCreated());
        String eventoId = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);

        criar("/api/v1/eventos-clinicos/" + eventoId + "/concluir", vet, "{}")
                .andExpect(status().isOk());
        return eventoId;
    }

    private String criarPendente(String eventoId, String valor) throws Exception {
        ResultActions pagamento = criar("/api/v1/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":%s,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}"""
                .formatted(valor, eventoId));
        pagamento.andExpect(status().isCreated());
        String id = idDe(pagamento);
        removerDepois("/api/v1/pagamentos/" + id);
        return id;
    }

    private MockHttpServletResponse enviar(String url, String token, String corpo) throws Exception {
        return criar(url, token, corpo).andReturn().getResponse();
    }

    /** O mesmo pedido, {@link #PEDIDOS} vezes, todos soltos na mesma largada. */
    private List<MockHttpServletResponse> aoMesmoTempo(Callable<MockHttpServletResponse> pedido) throws Exception {
        return Simultaneo.disparar(PEDIDOS, pedido);
    }

    private List<MockHttpServletResponse> aoMesmoTempo(int vezes, Callable<MockHttpServletResponse> pedido)
            throws Exception {
        return Simultaneo.disparar(vezes, pedido);
    }

    private List<MockHttpServletResponse> aoMesmoTempo(List<Callable<MockHttpServletResponse>> pedidos)
            throws Exception {
        return Simultaneo.disparar(pedidos);
    }

    /** Uma conta nova com um pet: o que ela consente nao contamina os pets do seed. */
    private TutorNovo tutorNovoComPet() throws Exception {
        String email = "segundo.tutor.%d@teste.com".formatted(System.nanoTime());
        JsonNode conta = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"senha12345","nome":"Segundo Tutor"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String token = token(email, "senha12345");

        ResultActions pet = criar("/api/v1/animais", token, """
                {"nome":"Pipoca","raca":"Vira-lata","especie":"CAO","porte":"PEQUENO","cor":"Caramelo",
                 "sexo":"MACHO","dataNascimento":"2021-04-01","tutorId":"%s"}"""
                .formatted(conta.get("tutorId").asText()));
        pet.andExpect(status().isCreated());
        String animalId = idDe(pet);
        removerDepois("/api/v1/animais/" + animalId);
        return new TutorNovo(token, animalId);
    }

    private record TutorNovo(String token, String animalId) {
    }

    private List<Integer> statusDe(List<MockHttpServletResponse> respostas) {
        return respostas.stream().map(MockHttpServletResponse::getStatus).toList();
    }

    /** Fora das threads: a fila do removerDepois nao e thread-safe. */
    private void removerOsCriados(List<MockHttpServletResponse> respostas) throws Exception {
        for (MockHttpServletResponse r : respostas) {
            if (r.getStatus() == 201) {
                String id = objectMapper.readTree(r.getContentAsString()).get("id").asText();
                removerDepois("/api/v1/eventos-clinicos/" + id);
            }
        }
    }
}
