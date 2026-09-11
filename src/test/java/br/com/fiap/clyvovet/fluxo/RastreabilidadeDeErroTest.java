package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.config.CorrelacaoFilter;
import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import br.com.fiap.clyvovet.exception.GlobalExceptionHandler;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O erro precisa ser rastreavel: o usuario le uma referencia na tela e quem
 * investiga encontra a mesma referencia no log.
 *
 * <p>Estes testes existem porque a parte facil de acertar é a que aparece — o
 * texto na tela. O que quebra em silencio e o resto: o id que nao volta no
 * header, o id da requisicao anterior vazando para a proxima, o campo novo
 * mudando o contrato que o aplicativo ja consome.</p>
 */
class RastreabilidadeDeErroTest extends TesteDeApi {

    @Test
    @DisplayName("toda resposta volta com a referencia, mesmo quando da erro")
    void devolveReferenciaNoHeader() throws Exception {
        String referencia = mockMvc.perform(get("/api/v1/clinicas"))
                .andReturn()
                .getResponse()
                .getHeader(CorrelacaoFilter.HEADER);

        // Sem token isto e 401. A referencia tem que existir mesmo assim: e
        // justamente a requisicao que falha que precisa ser encontrada no log.
        assertThat(referencia)
                .as("o filtro roda antes da seguranca, entao ate o 401 e rastreavel")
                .isNotBlank();
    }

    @Test
    @DisplayName("duas requisicoes recebem referencias diferentes")
    void naoRepeteReferenciaEntreRequisicoes() throws Exception {
        String primeira = mockMvc.perform(get("/api/v1/clinicas"))
                .andReturn().getResponse().getHeader(CorrelacaoFilter.HEADER);
        String segunda = mockMvc.perform(get("/api/v1/clinicas"))
                .andReturn().getResponse().getHeader(CorrelacaoFilter.HEADER);

        // O container reaproveita a thread. Se o MDC nao fosse limpo no finally,
        // as duas viriam iguais -- e a investigacao apontaria para a requisicao
        // errada, que e pior do que nao ter referencia nenhuma.
        assertThat(primeira).isNotEqualTo(segunda);
    }

    @Test
    @DisplayName("a referencia enviada pelo cliente e mantida, para cruzar com a API .NET")
    void aceitaReferenciaDoCliente() throws Exception {
        String meuId = "teste-1234-abcd";

        String devolvido = mockMvc.perform(get("/api/v1/clinicas")
                        .header(CorrelacaoFilter.HEADER, meuId))
                .andReturn().getResponse().getHeader(CorrelacaoFilter.HEADER);

        assertThat(devolvido).isEqualTo(meuId);
    }

    @Test
    @DisplayName("referencia com quebra de linha e descartada, para nao forjar log")
    void recusaReferenciaQueInjetaLinha() throws Exception {
        String malicioso = "abc\nINFO  [x] Usuario admin promovido";

        String devolvido = mockMvc.perform(get("/api/v1/clinicas")
                        .header(CorrelacaoFilter.HEADER, malicioso))
                .andReturn().getResponse().getHeader(CorrelacaoFilter.HEADER);

        // Nao basta ser diferente: tem que ser um id gerado por nos, sem rastro
        // do que veio de fora. Log forjado engana quem investiga -- e pior do que
        // log nenhum.
        assertThat(devolvido).isNotBlank();
        assertThat(devolvido).doesNotContain("\n");
        assertThat(devolvido).doesNotContain("promovido");
    }

    @Test
    @DisplayName("falha inesperada responde no formato da API, com referencia")
    void falhaInesperadaMantemOContrato() throws Exception {
        ResponseEntity<ErroValidacao> resposta =
                new GlobalExceptionHandler().handleInesperado(new IllegalStateException("detalhe interno"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().campo()).isEqualTo("servidor");

        // A mensagem da excecao cita classe, tabela e as vezes o SQL. Nada disso
        // pode chegar a tela.
        assertThat(resposta.getBody().mensagem()).doesNotContain("detalhe interno");
        assertThat(resposta.getBody().mensagem()).doesNotContain("IllegalStateException");
    }

    @Test
    @DisplayName("negativa de acesso continua 403, e nao vira 500 pelo catch-all")
    void catchAllNaoEngoleNegativaDeAcesso() throws Exception {
        String tokenDoTutor = tokenTutor(LUCAS);

        // Um tutor alcancando a listagem de usuarios da plataforma. O @PreAuthorize
        // recusa, e quem traduz isso e a cadeia do Spring Security -- nao esta
        // classe. A primeira versao do catch-all capturava a AccessDeniedException
        // antes disso e devolvia 500: 38 testes de autorizacao caíram de uma vez.
        mockMvc.perform(get("/api/v1/auth/usuarios")
                        .header("Authorization", "Bearer " + tokenDoTutor))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rota inexistente continua 404, e nao vira 500 pelo catch-all")
    void catchAllNaoEngoleRotaInexistente() throws Exception {
        // Com token de proposito: sem ele a cadeia de seguranca responde 401 antes
        // de o Spring descobrir que a rota nao existe -- e o 401 ali esta certo,
        // rota desconhecida tambem e protegida.
        //
        // Autenticado, a excecao que chega e a NoResourceFoundException, que
        // implementa ErrorResponse e ja carrega o proprio 404. Trata-la como
        // "inesperada" transformaria um endereco errado em falha de servidor.
        mockMvc.perform(get("/api/v1/rota-que-nao-existe")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("o erro comum continua sem referencia, e o contrato antigo nao muda")
    void erroDeUsuarioNaoGanhaCampoNovo() throws Exception {
        String json = objectMapper.writeValueAsString(
                new ErroValidacao("nome", "não pode estar em branco"));

        // O aplicativo ja consome {campo, mensagem}. Um terceiro campo sempre
        // presente mudaria o contrato sem necessidade: no erro que o usuario
        // corrige sozinho nao ha nada a investigar.
        assertThat(json).doesNotContain("referencia");
        assertThat(json).contains("\"campo\":\"nome\"");
    }
}
