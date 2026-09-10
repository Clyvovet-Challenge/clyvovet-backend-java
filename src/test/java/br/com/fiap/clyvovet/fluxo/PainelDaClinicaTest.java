package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O painel da clinica — a pergunta que a API nao respondia.
 *
 * <p>Tudo aqui acontece numa janela de marco de 2019, e isso e deliberado: a suite
 * compartilha o banco e nao ha rollback entre classes, entao qualquer assercao sobre
 * "quantos atendimentos" precisa de um periodo que so este teste povoa. O seed vive
 * em 2024 e 2026; os demais testes, em 2023 e em torno de hoje.</p>
 */
class PainelDaClinicaTest extends TesteDeApi {

    private static final String DE = "2019-03-01";
    private static final String ATE = "2019-03-31";
    private static final String DA_VETCARE = "admin.vetcare@clinica.test";
    private static final String DA_PETMED = "admin.petmed@clinica.test";

    private String admin;
    private String vetcare;
    private String servicoId;

    /**
     * Quatro atendimentos na VetCare e um na PetMed, todos na mesma janela.
     *
     * <p>A composicao nao e arbitraria — cada linha existe para uma assercao:</p>
     * <ul>
     *   <li>dois realizados COM servico e desfecho MELHORA (agrupamento e receita);</li>
     *   <li>um realizado SEM servico, concluido sem desfecho (o rotulo "Não registrado"
     *       e a diferenca entre realizados e a soma por servico);</li>
     *   <li>um que fica AGENDADO (a caixa "marcados", e a prova de que servico so
     *       conta o que aconteceu);</li>
     *   <li>um na concorrente (o recorte).</li>
     * </ul>
     */
    @BeforeEach
    void montarOMovimentoDeMarcoDe2019() throws Exception {
        admin = tokenAdmin();
        vetcare = tokenAdminDaClinica(DA_VETCARE, SeedV2.CLINICA_VETCARE);

        // O servico entra PRIMEIRO na fila de remocao para sair por ULTIMO: os
        // eventos o referenciam, e a ordem inversa esbarraria na chave estrangeira.
        servicoId = idDe(criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta do painel %s","tipoEvento":"CONSULTA",
                 "preco":150.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_VETCARE, System.nanoTime()))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/servicos/" + servicoId);

        String comServico = concluido("2019-03-05", servicoId, SeedV2.ANIMAL_BOLINHA_DO_LUCAS, "MELHORA");
        String outroComServico = concluido("2019-03-10", servicoId, SeedV2.ANIMAL_BOLINHA_DO_LUCAS, "MELHORA");
        String semServico = concluido("2019-03-15", null, SeedV2.ANIMAL_MIMI_DA_MARIA, null);
        atendimento("2019-03-20", servicoId, SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.VET_CAMILA,
                SeedV2.CLINICA_VETCARE);
        atendimentoConcluidoNaPetMed();

        pagar(comServico, "PAGO", "150.00");
        pagar(outroComServico, "PENDENTE", "150.00");
        // Pagamento de atendimento SEM servico: entra no caixa e nao entra em nenhuma
        // linha da lista de servicos. E o caso que o LEFT JOIN da consulta protege.
        pagar(semServico, "PAGO", "80.00");
    }

    // ================================================================
    // Os numeros
    // ================================================================

    @Test
    @DisplayName("conta o movimento do periodo por situacao do atendimento")
    void contaOMovimento() throws Exception {
        JsonNode atendimentos = painel(vetcare).get("atendimentos");

        assertThat(atendimentos.get("marcados").asLong()).isEqualTo(1);
        assertThat(atendimentos.get("realizados").asLong()).isEqualTo(3);
        assertThat(atendimentos.get("faltas").asLong()).isZero();
        assertThat(atendimentos.get("cancelados").asLong()).isZero();
        assertThat(atendimentos.get("total").asLong()).isEqualTo(4);
        assertThat(atendimentos.get("taxaDeComparecimento").asDouble()).isEqualTo(100.0);
        assertThat(atendimentos.get("taxaDeFalta").asDouble()).isZero();
    }

    /**
     * O caixa soma os TRES pagamentos, inclusive o do atendimento sem servico.
     *
     * <p>E a regressao que o LEFT JOIN existe para impedir: com o join implicito
     * ({@code p.eventoClinico.servico.id}), o Hibernate gera INNER JOIN e o pagamento
     * de R$ 80,00 desaparece do faturamento sem erro nenhum — o total simplesmente
     * fica menor. A V2 tem pagamentos exatamente assim.</p>
     */
    @Test
    @DisplayName("soma o dinheiro por situacao, inclusive de atendimento sem servico")
    void somaODinheiro() throws Exception {
        JsonNode faturamento = painel(vetcare).get("faturamento");

        assertThat(faturamento.get("recebido").asDouble()).isEqualTo(230.00);
        assertThat(faturamento.get("aReceber").asDouble()).isEqualTo(150.00);
        assertThat(faturamento.get("estornado").asDouble()).isZero();
        assertThat(faturamento.get("cancelado").asDouble()).isZero();
        assertThat(faturamento.get("pagamentos").asLong()).isEqualTo(3);
        // 230 / 3 realizados, arredondado meio-para-cima na segunda casa.
        assertThat(faturamento.get("ticketMedio").asDouble()).isEqualTo(76.67);
    }

    @Test
    @DisplayName("agrupa os desfechos e mostra o atendimento concluido sem desfecho")
    void agrupaOsDesfechos() throws Exception {
        JsonNode desfechos = painel(vetcare).get("desfechos");

        assertThat(quantidadeDe(desfechos, "MELHORA")).isEqualTo(2);
        // Nao e INDEFINIDO: aquele e o veterinario dizendo que nao soube classificar.
        assertThat(quantidadeDe(desfechos, "Não registrado")).isEqualTo(1);
        assertThat(percentualDe(desfechos, "MELHORA")).isEqualTo(66.7);
        // A soma dos desfechos bate com os realizados -- e o motivo de o nulo entrar.
        assertThat(somaDe(desfechos)).isEqualTo(3);
    }

    @Test
    @DisplayName("ordena as racas mais atendidas")
    void ordenaAsRacas() throws Exception {
        JsonNode racas = painel(vetcare).get("racas");

        assertThat(quantidadeDe(racas, "Golden Retriever")).isEqualTo(2);
        // "Siames" com acento -- e nao como o seed digitou.
        //
        // A V14 reconcilia o texto legado contra o catalogo de racas e reescreve
        // `animal.raca` com o nome canonico. Este assert e o que prova, de fora,
        // que aquela uniformizacao aconteceu: antes da V14 o painel agrupava por
        // "Siames" e "Siames" acentuado como se fossem duas racas.
        assertThat(quantidadeDe(racas, "Siamês")).isEqualTo(1);
        assertThat(racas.get(0).get("rotulo").asText()).isEqualTo("Golden Retriever");
        assertThat(somaDe(racas)).isEqualTo(3);
    }

    /**
     * A lista de servicos conta 2, e os realizados sao 3.
     *
     * <p>A diferenca e o atendimento sem servico do catalogo, e ela e visivel de
     * proposito: e o quanto a clinica atende sem precificar. O agendado tambem fica
     * fora — servico prestado e o que aconteceu.</p>
     */
    @Test
    @DisplayName("mede cada servico do catalogo pelo que aconteceu e pelo que foi pago")
    void medeOsServicos() throws Exception {
        JsonNode servicos = painel(vetcare).get("servicos");

        assertThat(servicos.size()).isEqualTo(1);
        assertThat(servicos.get(0).get("servicoId").asText()).isEqualTo(servicoId);
        assertThat(servicos.get(0).get("realizados").asLong()).isEqualTo(2);
        // 150 do PAGO. O PENDENTE de mesmo valor nao e receita.
        assertThat(servicos.get(0).get("receita").asDouble()).isEqualTo(150.00);
    }

    @Test
    @DisplayName("o painel de uma clinica ignora o atendimento da concorrente")
    void naoMisturaClinicas() throws Exception {
        assertThat(painel(vetcare).get("atendimentos").get("realizados").asLong()).isEqualTo(3);

        String petmed = tokenAdminDaClinica(DA_PETMED, SeedV2.CLINICA_PETMED);
        assertThat(painel(petmed, SeedV2.CLINICA_PETMED).get("atendimentos").get("realizados").asLong())
                .isEqualTo(1);
    }

    // ================================================================
    // O periodo
    // ================================================================

    @Test
    @DisplayName("sem parametros, a janela sao os ultimos 30 dias -- e vem na resposta")
    void janelaPadrao() throws Exception {
        JsonNode painel = corpoDe(buscar("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE + "/painel", vetcare)
                .andExpect(status().isOk()));

        assertThat(painel.get("de").asText()).isNotBlank();
        assertThat(painel.get("ate").asText()).isNotBlank();
        assertThat(LocalDate.parse(painel.get("de").asText()))
                .isEqualTo(LocalDate.parse(painel.get("ate").asText()).minusDays(29));
    }

    /**
     * Periodo invertido e 409, e nao 200 com tudo zerado.
     *
     * <p>Zerado, a resposta seria indistinguivel de uma clinica que nao trabalhou no
     * mes — e quem esta olhando nao teria como saber que so inverteu as datas.</p>
     */
    @Test
    @DisplayName("periodo invertido e recusado")
    void periodoInvertido() throws Exception {
        buscar("/api/v1/clinicas/" + SeedV2.CLINICA_VETCARE + "/painel?de=2019-03-31&ate=2019-03-01", vetcare)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("de"));
    }

    @Test
    @DisplayName("clinica inexistente e 404")
    void clinicaInexistente() throws Exception {
        buscar("/api/v1/clinicas/" + SeedV2.ID_INEXISTENTE + "/painel", admin)
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // Quem abre
    // ================================================================

    @Test
    @DisplayName("o administrador da clinica abre o painel da propria casa")
    void oAdministradorAbreOProprio() throws Exception {
        buscar(url(SeedV2.CLINICA_VETCARE), vetcare).andExpect(status().isOk());
    }

    @Test
    @DisplayName("NAO abre o painel da concorrente")
    void naoAbreODaConcorrente() throws Exception {
        buscar(url(SeedV2.CLINICA_PETMED), vetcare).andExpect(status().isForbidden());
    }

    /**
     * O veterinario da casa abre, e o motivo esta em {@code podeVerPainelDe}: ele ja
     * alcanca item a item tudo que o painel soma — inadimplencia, extrato e
     * atendimentos da clinica. Negar so o total seria uma regra que nao segura nada.
     */
    @Test
    @DisplayName("o veterinario da casa abre o painel dela")
    void oVeterinarioDaCasaAbre() throws Exception {
        buscar(url(SeedV2.CLINICA_VETCARE), tokenVeterinaria()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("o veterinario NAO abre o painel de outra clinica")
    void oVeterinarioNaoAbreODeFora() throws Exception {
        buscar(url(SeedV2.CLINICA_PETMED), tokenVeterinaria()).andExpect(status().isForbidden());
    }

    /** Nao ha versao "so a minha parte" de faturamento e taxa de falta. */
    @Test
    @DisplayName("o tutor nao abre painel de clinica nenhuma")
    void oTutorNaoAbre() throws Exception {
        buscar(url(SeedV2.CLINICA_VETCARE), tokenTutor(LUCAS)).andExpect(status().isForbidden());
        buscar(url(SeedV2.CLINICA_PETMED), tokenTutor(LUCAS)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o ADMIN da plataforma abre o de qualquer uma")
    void oAdminAbreTodos() throws Exception {
        buscar(url(SeedV2.CLINICA_VETCARE), admin).andExpect(status().isOk());
        buscar(url(SeedV2.CLINICA_PETMED), admin).andExpect(status().isOk());
    }

    /**
     * O {@code /auth/me} passa a dizer de qual clinica o usuario e.
     *
     * <p>Sem isto o perfil existia e era inutil no cliente: o administrador logava e
     * nao tinha como descobrir o id da propria clinica, que e o primeiro segmento de
     * toda rota daqui.</p>
     */
    @Test
    @DisplayName("o /auth/me diz qual clinica o usuario administra")
    void oMeDizAClinica() throws Exception {
        JsonNode me = corpoDe(buscar("/api/v1/auth/me", vetcare).andExpect(status().isOk()));
        assertThat(me.get("clinicaId").asText()).isEqualTo(SeedV2.CLINICA_VETCARE);
        assertThat(me.get("clinicaNome").asText()).isNotBlank();

        // E para o veterinario responde a clinica onde ele ATENDE -- outro caminho,
        // mesma pergunta.
        JsonNode dela = corpoDe(buscar("/api/v1/auth/me", tokenVeterinaria()).andExpect(status().isOk()));
        assertThat(dela.get("clinicaId").asText()).isEqualTo(SeedV2.CLINICA_VETCARE);
    }

    // ================================================================
    // Apoio
    // ================================================================

    private static String url(String clinicaId) {
        return "/api/v1/clinicas/" + clinicaId + "/painel?de=" + DE + "&ate=" + ATE;
    }

    private JsonNode painel(String token) throws Exception {
        return painel(token, SeedV2.CLINICA_VETCARE);
    }

    private JsonNode painel(String token, String clinicaId) throws Exception {
        return corpoDe(buscar(url(clinicaId), token).andExpect(status().isOk()));
    }

    private String atendimento(String data, String servicoId, String animalId,
                               String veterinarioId, String clinicaId) throws Exception {
        String corpo = """
                {"data":"%s","hora":"09:00","descricao":"Atendimento do painel",
                 "tipoEvento":"CONSULTA","veterinarioId":"%s","animalId":"%s","clinicaId":"%s"%s}"""
                .formatted(data, veterinarioId, animalId, clinicaId,
                        servicoId == null ? "" : ",\"servicoId\":\"" + servicoId + "\"");
        String id = idDe(criar("/api/v1/eventos-clinicos", admin, corpo)
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    /** Nasce AGENDADO e e concluido pela acao propria — o unico caminho para REALIZADO. */
    private String concluido(String data, String servicoId, String animalId, String desfecho)
            throws Exception {
        String id = atendimento(data, servicoId, animalId, SeedV2.VET_CAMILA, SeedV2.CLINICA_VETCARE);
        criar("/api/v1/eventos-clinicos/" + id + "/concluir", admin,
                desfecho == null ? "{}" : "{\"desfecho\":\"%s\"}".formatted(desfecho))
                .andExpect(status().isOk());
        return id;
    }

    private void atendimentoConcluidoNaPetMed() throws Exception {
        String id = atendimento("2019-03-12", null, SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                SeedV2.VET_RAFAEL_DA_PETMED, SeedV2.CLINICA_PETMED);
        criar("/api/v1/eventos-clinicos/" + id + "/concluir", admin, "{\"desfecho\":\"PIORA\"}")
                .andExpect(status().isOk());
    }

    private void pagar(String eventoId, String status, String valor) throws Exception {
        String data = "PAGO".equals(status) ? ",\"dataPagamento\":\"2019-03-25\"" : "";
        String id = idDe(criar("/api/v1/pagamentos", admin, """
                {"formaPagamento":"PIX","valor":%s,"statusPagamento":"%s",
                 "eventoClinicoId":"%s","descricao":"Painel de marco"%s}"""
                .formatted(valor, status, eventoId, data))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/pagamentos/" + id);
    }

    private long quantidadeDe(JsonNode lista, String rotulo) {
        for (JsonNode linha : lista) {
            if (rotulo.equals(linha.get("rotulo").asText())) {
                return linha.get("quantidade").asLong();
            }
        }
        return 0L;
    }

    private double percentualDe(JsonNode lista, String rotulo) {
        for (JsonNode linha : lista) {
            if (rotulo.equals(linha.get("rotulo").asText())) {
                return linha.get("percentual").asDouble();
            }
        }
        return 0d;
    }

    private long somaDe(JsonNode lista) {
        long total = 0;
        for (JsonNode linha : lista) {
            total += linha.get("quantidade").asLong();
        }
        return total;
    }
}
