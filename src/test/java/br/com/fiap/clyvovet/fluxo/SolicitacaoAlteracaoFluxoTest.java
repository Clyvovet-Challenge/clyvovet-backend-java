package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O fluxo "o veterinário pede, o tutor aprova".
 *
 * <p>É o caminho de volta para o veterinário poder alterar o cadastro do animal.
 * A escrita direta foi fechada quando se descobriu que qualquer veterinário
 * autenticado editava e excluía o pet de qualquer tutor — sem nenhuma autorização.
 * A regra do produto sempre foi "desde que tenha confirmação e autorização do
 * dono"; estes testes são essa regra.</p>
 *
 * <p>Dados da migration V2: Lucas é dono do Bolinha; Camila é a veterinária.</p>
 */
class SolicitacaoAlteracaoFluxoTest extends TesteDeApi {

    private static final String PEDIDO = """
            {"justificativa":"Confirmei o microchip e a raça na consulta de hoje.",
             "raca":"Poodle Toy"}""";

    /**
     * Esvazia a caixa do tutor antes de cada teste.
     *
     * <p>Necessário porque a suíte compartilha o banco e o serviço impede o mesmo
     * veterinário de empilhar pedidos no mesmo animal — a guarda que evita o duplo
     * clique também faria um teste derrubar o seguinte. Recusar é o caminho: usa a
     * própria API, então também exercita o fluxo em vez de mexer na tabela.</p>
     */
    @BeforeEach
    void esvaziarCaixaDoTutor() throws Exception {
        String lucas = tokenTutor(LUCAS);
        for (JsonNode pendente : corpoDe(buscar("/api/v1/solicitacoes-alteracao/minhas", lucas))) {
            criar("/api/v1/solicitacoes-alteracao/" + pendente.get("id").asText() + "/recusar",
                    lucas, """
                    {"motivo":"Limpeza entre testes."}""")
                    .andExpect(status().isOk());
        }
    }

    private String urlPedir() {
        return "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/solicitacoes-alteracao";
    }

    private String idDoPedido(String vet) throws Exception {
        JsonNode criado = corpoDe(criar(urlPedir(), vet, PEDIDO).andExpect(status().isCreated()));
        return criado.get("id").asText();
    }

    // ================================================================
    // O caminho feliz
    // ================================================================

    @Test
    @DisplayName("o veterinario pede, o tutor aprova, e o cadastro muda")
    void pedidoAprovadoAlteraOCadastro() throws Exception {
        String vet = tokenVeterinaria();
        String lucas = tokenTutor(LUCAS);
        String animal = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        String racaOriginal = corpoDe(buscar(animal, lucas)).get("raca").asText();

        // 1. O veterinário pede. O pedido nasce PENDENTE e nada mudou ainda.
        String pedido = idDoPedido(vet);
        assertThat(corpoDe(buscar(animal, lucas)).get("raca").asText()).isEqualTo(racaOriginal);

        // 2. O pedido aparece na caixa do tutor, com o antes e o depois.
        JsonNode caixa = corpoDe(buscar("/api/v1/solicitacoes-alteracao/minhas", lucas));
        assertThat(caixa).hasSize(1);
        JsonNode alteracao = caixa.get(0).get("alteracoes").get(0);
        assertThat(alteracao.get("campo").asText()).isEqualTo("raça");
        assertThat(alteracao.get("atual").asText()).isEqualTo(racaOriginal);
        assertThat(alteracao.get("proposto").asText()).isEqualTo("Poodle Toy");

        // 3. O tutor aprova, e só então o animal muda.
        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/aprovar", lucas, "{}")
                .andExpect(status().isOk());
        assertThat(corpoDe(buscar(animal, lucas)).get("raca").asText()).isEqualTo("Poodle Toy");

        // 4. A caixa esvazia — o pedido saiu de pendente.
        assertThat(corpoDe(buscar("/api/v1/solicitacoes-alteracao/minhas", lucas))).isEmpty();

        // Devolve ao estado do seed, que os demais testes assumem.
        atualizarParcialmente(animal, lucas, "{\"raca\":\"" + racaOriginal + "\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("o tutor recusa e o cadastro NAO muda")
    void pedidoRecusadoNaoAlteraNada() throws Exception {
        String vet = tokenVeterinaria();
        String lucas = tokenTutor(LUCAS);
        String animal = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;

        String racaOriginal = corpoDe(buscar(animal, lucas)).get("raca").asText();
        String pedido = idDoPedido(vet);

        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/recusar", lucas, """
                {"motivo":"A raça está correta na carteirinha."}""")
                .andExpect(status().isOk());

        assertThat(corpoDe(buscar(animal, lucas)).get("raca").asText()).isEqualTo(racaOriginal);
    }

    // ================================================================
    // Quem pode o quê
    // ================================================================

    @Test
    @DisplayName("o tutor NAO responde pedido de animal de outro tutor")
    void tutorNaoRespondePedidoAlheio() throws Exception {
        String pedido = idDoPedido(tokenVeterinaria());

        // Maria não é dona do Bolinha. Sem esta regra, qualquer tutor aprovaria
        // alteração no pet de qualquer outro — o furo trocaria de porta.
        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/aprovar", tokenTutor(MARIA), "{}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o proprio veterinario NAO aprova o que ele mesmo pediu")
    void veterinarioNaoAprovaOProprioPedido() throws Exception {
        String vet = tokenVeterinaria();
        String pedido = idDoPedido(vet);

        // O ponto inteiro do fluxo. Se o autor pudesse aprovar, o pedido seria
        // uma formalidade e a escrita continuaria aberta, só com mais passos.
        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/aprovar", vet, "{}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o tutor NAO cria pedido -- ele edita o proprio pet direto")
    void tutorNaoCriaPedido() throws Exception {
        criar(urlPedir(), tokenTutor(LUCAS), PEDIDO).andExpect(status().isForbidden());
    }

    // ================================================================
    // Regras do pedido
    // ================================================================

    @Test
    @DisplayName("pedido sem justificativa e recusado")
    void pedidoExigeJustificativa() throws Exception {
        criar(urlPedir(), tokenVeterinaria(), """
                {"raca":"Poodle Toy"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("pedido sem nenhum campo a alterar e recusado")
    void pedidoExigeAoMenosUmCampo() throws Exception {
        // 409 e nao 400: neste projeto RegraDeNegocioException e CONFLICT. O 400
        // fica para violacao de formato, que o Bean Validation pega antes.
        criar(urlPedir(), tokenVeterinaria(), """
                {"justificativa":"Quero mexer no cadastro deste paciente."}""")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("pedido com valores iguais aos atuais e recusado")
    void pedidoQueNaoMudaNadaERecusado() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String racaAtual = corpoDe(
                buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, lucas)).get("raca").asText();

        // Gasta a atenção do tutor para nada: aprovar não mudaria uma linha.
        criar(urlPedir(), tokenVeterinaria(),
                "{\"justificativa\":\"Confirmando o que ja esta la.\",\"raca\":\"" + racaAtual + "\"}")
                .andExpect(status().isConflict());
    }

    /**
     * O porte em outra caixa NAO e uma alteracao.
     *
     * <p>Era, e o tutor via {@code porte: PEQUENO -> Pequeno} no pedido para
     * aprovar — o mesmo porte apresentado como mudanca. Verificado contra o MySQL
     * do docker-compose antes da correcao.</p>
     */
    @Test
    @DisplayName("porte na mesma caixa que o atual nao vira alteracao")
    void porteEmOutraCaixaNaoEUmaAlteracao() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String atual = corpoDe(buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS, lucas))
                .get("porte").asText();

        // "PEQUENO" -> "Pequeno": e o que o formulario do app manda.
        String comoOAppManda = atual.charAt(0) + atual.substring(1).toLowerCase();

        criar(urlPedir(), tokenVeterinaria(),
                "{\"justificativa\":\"Reavaliacao de porte na consulta.\",\"porte\":\""
                        + comoOAppManda + "\"}")
                .andExpect(status().isConflict());
    }

    /**
     * O porte aprovado entra em CAIXA ALTA no cadastro.
     *
     * <p>A normalizacao vivia no {@code AnimalMapper} e cobria o POST e o PATCH.
     * Este caminho — solicitacao aprovada — chama {@code aplicarEm}, que escreve
     * direto na entidade, e escapava: um pedido com {@code "porte":"grande"}
     * gravava {@code 'grande'}.</p>
     *
     * <p>No MySQL o dano fica escondido, porque a colacao padrao e
     * {@code _ai_ci} e o CHECK aceita. <b>No Oracle a aprovacao falharia</b> com
     * violacao de {@code chk_animal_porte} — um erro de banco na cara do tutor,
     * so no banco que a suite nao usa.</p>
     */
    @Test
    @DisplayName("porte de pedido aprovado e gravado em caixa alta")
    void porteAprovadoEGravadoEmCaixaAlta() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String vet = tokenVeterinaria();
        String url = "/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS;
        String original = corpoDe(buscar(url, lucas)).get("porte").asText();

        // Um porte DIFERENTE do atual, para o pedido nao cair no "nada muda", e
        // em minuscula, que e o formato que escapava.
        String proposto = original.equalsIgnoreCase("GRANDE") ? "medio" : "grande";

        String pedidoId = corpoDe(criar(urlPedir(), vet,
                "{\"justificativa\":\"Reavaliacao de porte na consulta.\",\"porte\":\""
                        + proposto + "\"}")
                .andExpect(status().isCreated())).get("id").asText();

        criar("/api/v1/solicitacoes-alteracao/" + pedidoId + "/aprovar", lucas, "")
                .andExpect(status().isOk());

        try {
            assertThat(corpoDe(buscar(url, lucas)).get("porte").asText())
                    .as("o CHECK do Oracle e sensivel a caixa; gravar 'grande' quebraria a aprovacao la")
                    .isEqualTo(proposto.toUpperCase());
        } finally {
            // A suite compartilha o banco: devolver o porte evita que este teste
            // decida o resultado do proximo.
            atualizarParcialmente(url, lucas, "{\"porte\":\"" + original + "\"}")
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("o mesmo veterinario nao empilha dois pedidos no mesmo animal")
    void naoEmpilhaPedidosDuplicados() throws Exception {
        String vet = tokenVeterinaria();
        idDoPedido(vet);

        // Um duplo clique encheria a caixa do tutor com pedidos idênticos.
        criar(urlPedir(), vet, PEDIDO).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("um pedido so e respondido uma vez")
    void pedidoRespondidoNaoAceitaSegundaResposta() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String pedido = idDoPedido(tokenVeterinaria());

        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/recusar", lucas, """
                {"motivo":"Nao concordo com a mudanca."}""")
                .andExpect(status().isOk());

        criar("/api/v1/solicitacoes-alteracao/" + pedido + "/aprovar", lucas, "{}")
                .andExpect(status().isConflict());
    }
}
