package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.Simultaneo;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O header Idempotency-Key em POST /pagamentos e POST /agendamentos.
 *
 * <p>O cenario e o do celular que desiste por timeout enquanto o servidor grava:
 * a pessoa toca de novo, com a mesma chave, e nada pode ser criado duas vezes.
 * Ver IdempotenciaFilter.</p>
 */
class IdempotenciaTest extends TesteDeApi {

    private String eventoId;

    @BeforeEach
    void atendimentoDeDuzentos() throws Exception {
        ResultActions servico = criar("/api/v1/servicos", tokenAdmin(), """
                {"clinicaId":"%s","nome":"Consulta idempotente %s","tipoEvento":"CONSULTA",
                 "preco":200.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()));
        servico.andExpect(status().isCreated());
        String servicoId = idDe(servico);
        removerDepois("/api/v1/servicos/" + servicoId);

        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenVeterinaria(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(5), SeedV2.VET_CAMILA,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE, servicoId));
        evento.andExpect(status().isCreated());
        eventoId = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + eventoId);
    }

    @Test
    @DisplayName("a mesma chave duas vezes em sequencia: uma cobranca so, e a segunda resposta e a primeira")
    void mesmaChaveEmSequencia() throws Exception {
        String vet = tokenVeterinaria();
        String chave = UUID.randomUUID().toString();

        MockHttpServletResponse primeira = cobrar(vet, chave, "200.00");
        MockHttpServletResponse segunda = cobrar(vet, chave, "200.00");
        removerOsPagamentos(List.of(primeira, segunda));

        assertThat(primeira.getStatus()).isEqualTo(201);
        assertThat(segunda.getStatus()).isEqualTo(201);
        assertThat(segunda.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(idDe(segunda)).isEqualTo(idDe(primeira));
        assertThat(pagamentosDoAtendimento()).isEqualTo(1);
    }

    @Test
    @DisplayName("a mesma chave varias vezes ao mesmo tempo: uma cobranca so")
    void mesmaChaveAoMesmoTempo() throws Exception {
        String vet = tokenVeterinaria();
        String chave = UUID.randomUUID().toString();

        List<MockHttpServletResponse> respostas = Simultaneo.disparar(4, () -> cobrar(vet, chave, "200.00"));
        removerOsPagamentos(respostas);

        // Quem chega com o primeiro ainda gravando recebe 409; quem chega depois,
        // a resposta guardada. Nenhum cria nada.
        Set<String> ids = new HashSet<>();
        for (MockHttpServletResponse r : respostas) {
            assertThat(r.getStatus()).isIn(201, 409);
            if (r.getStatus() == 201) {
                ids.add(idDe(r));
            }
        }
        assertThat(ids).hasSize(1);
        assertThat(pagamentosDoAtendimento()).isEqualTo(1);
    }

    @Test
    @DisplayName("a mesma chave com outro corpo e recusada com 422, e nada e criado")
    void mesmaChaveOutroCorpo() throws Exception {
        String vet = tokenVeterinaria();
        String chave = UUID.randomUUID().toString();

        MockHttpServletResponse primeira = cobrar(vet, chave, "100.00");
        MockHttpServletResponse outra = cobrar(vet, chave, "150.00");
        removerOsPagamentos(List.of(primeira));

        assertThat(outra.getStatus()).isEqualTo(422);
        assertThat(pagamentosDoAtendimento()).isEqualTo(1);
    }

    @Test
    @DisplayName("um pedido recusado nao prende a chave: a nova tentativa executa de novo")
    void falhaNaoFicaGuardada() throws Exception {
        String vet = tokenVeterinaria();
        String chave = UUID.randomUUID().toString();

        assertThat(cobrar(vet, chave, "-1.00").getStatus()).isEqualTo(400);

        MockHttpServletResponse corrigida = cobrar(vet, chave, "200.00");
        removerOsPagamentos(List.of(corrigida));
        assertThat(corrigida.getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("sem a chave, nada muda: dois pedidos criam duas cobrancas, como antes")
    void semChaveComoAntes() throws Exception {
        String vet = tokenVeterinaria();

        MockHttpServletResponse primeira = cobrar(vet, null, "100.00");
        MockHttpServletResponse segunda = cobrar(vet, null, "100.00");
        removerOsPagamentos(List.of(primeira, segunda));

        assertThat(pagamentosDoAtendimento()).isEqualTo(2);
    }

    @Test
    @DisplayName("chave que nao e UUID responde 400")
    void chaveInvalida() throws Exception {
        assertThat(cobrar(tokenVeterinaria(), "meu-email@exemplo.com", "200.00").getStatus()).isEqualTo(400);
        assertThat(pagamentosDoAtendimento()).isZero();
    }

    @Test
    @DisplayName("agendamento repetido com a mesma chave devolve o mesmo agendamento, e nao 'horario ocupado'")
    void agendamentoRepetido() throws Exception {
        AgendaDeTerca agenda = montarAgendaDeTerca();
        String lucas = tokenTutor(LUCAS);
        String chave = UUID.randomUUID().toString();
        String corpo = """
                {"animalId":"%s","servicoId":"%s","veterinarioId":"%s","data":"%s","hora":"10:30",
                 "consentimentoHistorico":true}"""
                .formatted(SeedV2.ANIMAL_BOLINHA_DO_LUCAS, agenda.servicoId(), SeedV2.VET_CAMILA, agenda.terca());

        MockHttpServletResponse primeira = enviar("/api/v1/agendamentos", lucas, chave, corpo);
        MockHttpServletResponse segunda = enviar("/api/v1/agendamentos", lucas, chave, corpo);
        removerDepois("/api/v1/eventos-clinicos/" + idDe(primeira));

        assertThat(primeira.getStatus()).isEqualTo(201);
        assertThat(segunda.getStatus()).isEqualTo(201);
        assertThat(idDe(segunda)).isEqualTo(idDe(primeira));
    }

    // ------------------------------------------------------------------

    private MockHttpServletResponse cobrar(String token, String chave, String valor) throws Exception {
        return enviar("/api/v1/pagamentos", token, chave, """
                {"formaPagamento":"PIX","valor":%s,"descricao":"Consulta",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}""".formatted(valor, eventoId));
    }

    private MockHttpServletResponse enviar(String url, String token, String chave, String corpo) throws Exception {
        var pedido = post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (chave != null) {
            pedido.header("Idempotency-Key", chave);
        }
        return mockMvc.perform(pedido).andReturn().getResponse();
    }

    private int pagamentosDoAtendimento() throws Exception {
        return totalDe(buscar("/api/v1/pagamentos?eventoClinicoId=" + eventoId, tokenVeterinaria()));
    }

    private String idDe(MockHttpServletResponse resposta) throws Exception {
        return objectMapper.readTree(resposta.getContentAsString()).get("id").asText();
    }

    /** Fora das threads: a fila do removerDepois nao e thread-safe. */
    private void removerOsPagamentos(List<MockHttpServletResponse> respostas) throws Exception {
        Set<String> ids = new HashSet<>();
        for (MockHttpServletResponse r : respostas) {
            if (r.getStatus() == 201) {
                ids.add(idDe(r));
            }
        }
        for (String id : ids) {
            removerDepois("/api/v1/pagamentos/" + id);
        }
    }
}
