package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Os meus atendimentos, de hoje" — a pergunta que o veterinário não conseguia fazer.
 *
 * <p>A API tinha {@code /agendamentos/meus} para o tutor e nada equivalente para quem
 * atende. O profissional pedia {@code GET /eventos-clinicos} e recebia a clínica
 * inteira — todos os dias, todos os colegas —, dez por página. Sem isso não existe
 * agenda do dia, e sem agenda do dia não existe fluxo do veterinário no app.</p>
 *
 * <p>A janela é maio de 2018 pelo mesmo motivo da classe do painel: a suíte
 * compartilha o banco, e contagem exata precisa de um período que só este teste
 * povoa.</p>
 */
class AgendaDoProfissionalTest extends TesteDeApi {

    private static final String DIA = "2018-05-10";
    private static final String OUTRO_DIA = "2018-05-11";
    private static final String JANELA = "de=2018-05-01&ate=2018-05-31";

    private String admin;

    /** Duas consultas da Camila e uma do colega Tomás — os dois da MESMA clínica. */
    @BeforeEach
    void montarADuplaAgenda() throws Exception {
        admin = tokenAdmin();
        atendimento(DIA, "09:00", SeedV2.VET_CAMILA);
        atendimento(OUTRO_DIA, "10:00", SeedV2.VET_CAMILA);
        atendimento(DIA, "11:00", SeedV2.VET_TOMAS_DA_VETCARE);
    }

    @Test
    @DisplayName("filtra pela agenda de um profissional dentro da propria clinica")
    void agendaDeUmProfissional() throws Exception {
        assertThat(totalDe(buscar(agenda(SeedV2.VET_CAMILA, JANELA), admin))).isEqualTo(2);
    }

    /**
     * O teste do CACHE, e não do filtro.
     *
     * <p>As duas consultas rodam em sequência, dentro do mesmo teste — e é assim que
     * o cache fica quente entre elas (o {@code @AfterEach} da base só limpa no fim).
     * Se {@code veterinarioId} não estivesse na chave de {@code @Cacheable}, a
     * segunda receberia a página da primeira e o Tomás veria dois atendimentos que
     * não são dele. É a ruptura B1 outra vez, agora vazando entre colegas em vez de
     * entre clínicas.</p>
     */
    @Test
    @DisplayName("a agenda de um colega NAO e servida do cache do outro")
    void filtroEntraNaChaveDoCache() throws Exception {
        assertThat(totalDe(buscar(agenda(SeedV2.VET_CAMILA, JANELA), admin))).isEqualTo(2);
        assertThat(totalDe(buscar(agenda(SeedV2.VET_TOMAS_DA_VETCARE, JANELA), admin))).isEqualTo(1);
    }

    @Test
    @DisplayName("o intervalo de datas tambem entra na chave")
    void intervaloEntraNaChaveDoCache() throws Exception {
        // O dia inteiro da clínica: as duas agendas somadas.
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos?de=" + DIA + "&ate=" + DIA, admin)))
                .isEqualTo(2);
        // A mesma rota, outro dia: se a data não estivesse na chave, viria 2.
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos?de=" + OUTRO_DIA + "&ate=" + OUTRO_DIA, admin)))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("filtra pela situacao do atendimento")
    void filtraPorSituacao() throws Exception {
        String url = agenda(SeedV2.VET_CAMILA, JANELA);
        assertThat(totalDe(buscar(url + "&statusEvento=AGENDADO", admin))).isEqualTo(2);
        assertThat(totalDe(buscar(url + "&statusEvento=REALIZADO", admin))).isZero();
    }

    /**
     * O próprio veterinário pede a agenda dele, com o token dele.
     *
     * <p>O filtro é conveniência; o recorte continua sendo o de sempre. A Camila vê
     * estes atendimentos porque são da clínica dela — e não porque pediu o próprio
     * id.</p>
     */
    @Test
    @DisplayName("o veterinario pede a propria agenda")
    void oProfissionalPedeAPropria() throws Exception {
        assertThat(totalDe(buscar(agenda(SeedV2.VET_CAMILA, JANELA), tokenVeterinaria()))).isEqualTo(2);
    }

    /**
     * Pedir a agenda de quem é de outra clínica devolve vazio, e não 403.
     *
     * <p>Não é descuido: o recorte por clínica age antes do filtro, então a consulta
     * simplesmente não alcança aquelas linhas. Responder 403 aqui contaria o que a
     * pessoa não pode saber — que existe agenda naquele id.</p>
     */
    @Test
    @DisplayName("a agenda de veterinario de outra clinica vem vazia")
    void agendaDeForaVemVazia() throws Exception {
        assertThat(totalDe(buscar(agenda(SeedV2.VET_RAFAEL_DA_PETMED, JANELA), tokenVeterinaria())))
                .isZero();
    }

    @Test
    @DisplayName("o tutor continua vendo so os proprios pets, filtro ou nao")
    void oTutorContinuaRecortado() throws Exception {
        // O filtro é do profissional; para o tutor ele não abre nada — os
        // atendimentos acima são de um pet que não é dele.
        assertThat(totalDe(buscar(agenda(SeedV2.VET_CAMILA, JANELA), tokenTutor(MARIA)))).isZero();
    }

    // ================================================================
    // O que a agenda precisava e a API nao expunha
    // ================================================================

    /**
     * Os bloqueios de um profissional, que antes não se listavam.
     *
     * <p>A API sabia CRIAR e APAGAR bloqueio, e não sabia mostrar: o id aparecia uma
     * única vez, na resposta do POST. Quem marcasse as férias fechava a tela e não
     * tinha mais como conferir nem desfazer — e a agenda ficava fechada por um motivo
     * invisível.</p>
     */
    @Test
    @DisplayName("lista os bloqueios de um profissional")
    void listaOsBloqueios() throws Exception {
        String id = idDe(criar("/api/v1/bloqueios", admin, """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Congresso"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12)))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/bloqueios/" + id);

        JsonNode bloqueios = corpoDe(
                buscar("/api/v1/veterinarios/" + SeedV2.VET_CAMILA + "/bloqueios", admin)
                        .andExpect(status().isOk()));

        boolean achou = false;
        for (JsonNode bloqueio : bloqueios) {
            if (id.equals(bloqueio.get("id").asText())) {
                achou = true;
                assertThat(bloqueio.get("motivo").asText()).isEqualTo("Congresso");
            }
        }
        assertThat(achou).isTrue();
    }

    /**
     * O bloqueio que JÁ TERMINOU sai da lista; o que ainda alcança hoje, fica.
     *
     * <p>O corte é por {@code dataFim}, e não por {@code dataInicio}: férias que
     * começaram semana passada e terminam amanhã continuam fechando a agenda, e
     * sumir com elas esconderia a causa dos horários que não aparecem.</p>
     */
    @Test
    @DisplayName("bloqueio vencido nao aparece; o que ainda alcanca hoje, sim")
    void bloqueioVencidoSai() throws Exception {
        String vencido = idDe(criar("/api/v1/bloqueios", admin, """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Ferias antigas"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(30), LocalDate.now().minusDays(20)))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/bloqueios/" + vencido);

        String emCurso = idDe(criar("/api/v1/bloqueios", admin, """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Ferias em curso"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/bloqueios/" + emCurso);

        JsonNode lista = corpoDe(
                buscar("/api/v1/veterinarios/" + SeedV2.VET_CAMILA + "/bloqueios", admin));

        assertThat(contem(lista, vencido)).isFalse();
        assertThat(contem(lista, emCurso)).isTrue();
    }

    /**
     * O motivo do bloqueio não é de quem estiver autenticado.
     *
     * <p>Quando esta rota nasceu, ela caiu em {@code anyRequest().authenticated()} e
     * ficou legível para qualquer conta — inclusive a de um tutor, que podia listar os
     * veterinários da plataforma e ler o motivo da ausência de cada um. E {@code motivo}
     * é texto livre da clínica: cabe "congresso" e cabe "licença médica".</p>
     *
     * <p>O tutor não perde nada com a restrição — ele nunca precisou da rota. A busca
     * por vagas já desconta os bloqueios do lado do servidor.</p>
     */
    @Test
    @DisplayName("o motivo do bloqueio e da casa, e nao de quem estiver autenticado")
    void bloqueioNaoEDeQualquerUm() throws Exception {
        String id = idDe(criar("/api/v1/bloqueios", admin, """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Licenca medica"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now(), LocalDate.now().plusDays(3)))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/bloqueios/" + id);

        String url = "/api/v1/veterinarios/" + SeedV2.VET_CAMILA + "/bloqueios";

        // A casa lê: o próprio profissional e o ADMIN da plataforma.
        buscar(url, tokenVeterinaria()).andExpect(status().isOk());
        buscar(url, admin).andExpect(status().isOk());

        // O tutor, não.
        buscar(url, tokenTutor(LUCAS)).andExpect(status().isForbidden());
    }

    /**
     * A cobrança de UM atendimento.
     *
     * <p>Sem este filtro, a tela de cobrança teria de varrer a listagem inteira,
     * página a página, para descobrir quais lançamentos são daquele atendimento — e
     * nenhuma página traz garantia de conter todos.</p>
     */
    @Test
    @DisplayName("filtra os pagamentos por atendimento")
    void filtraPagamentosPorAtendimento() throws Exception {
        String atendimento = atendimentoComId("2018-05-14", "15:00", SeedV2.VET_CAMILA);
        String outro = atendimentoComId("2018-05-15", "16:00", SeedV2.VET_CAMILA);

        pagar(atendimento, "80.00");
        pagar(atendimento, "40.00");
        pagar(outro, "25.00");

        assertThat(totalDe(buscar("/api/v1/pagamentos?eventoClinicoId=" + atendimento, admin)))
                .isEqualTo(2);
        // A segunda consulta em sequência: se o filtro não estivesse na chave do
        // cache, ela receberia a página da primeira.
        assertThat(totalDe(buscar("/api/v1/pagamentos?eventoClinicoId=" + outro, admin)))
                .isEqualTo(1);
    }

    // ================================================================
    // Apoio
    // ================================================================

    private boolean contem(JsonNode lista, String id) {
        for (JsonNode item : lista) {
            if (id.equals(item.get("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private String atendimentoComId(String data, String hora, String veterinarioId) throws Exception {
        String id = idDe(criar("/api/v1/eventos-clinicos", admin, """
                {"data":"%s","hora":"%s","descricao":"Cobranca do teste",
                 "tipoEvento":"CONSULTA","veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(data, hora, veterinarioId,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    private void pagar(String eventoId, String valor) throws Exception {
        String id = idDe(criar("/api/v1/pagamentos", admin, """
                {"formaPagamento":"PIX","valor":%s,"statusPagamento":"PENDENTE",
                 "eventoClinicoId":"%s","descricao":"Teste"}"""
                .formatted(valor, eventoId))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/pagamentos/" + id);
    }

    private static String agenda(String veterinarioId, String janela) {
        return "/api/v1/eventos-clinicos?veterinarioId=" + veterinarioId + "&" + janela;
    }

    private void atendimento(String data, String hora, String veterinarioId) throws Exception {
        String id = idDe(criar("/api/v1/eventos-clinicos", admin, """
                {"data":"%s","hora":"%s","descricao":"Consulta da agenda",
                 "tipoEvento":"CONSULTA","veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(data, hora, veterinarioId,
                        SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + id);
    }
}
