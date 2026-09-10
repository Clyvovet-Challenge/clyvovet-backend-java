package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os anexos do prontuario (V17): quem anexa, quem baixa, e quem nao.
 *
 * <p>O que estes testes protegem nao e "o upload funcionou". E a promessa de que
 * o ARQUIVO nao abriu uma porta mais larga que o TEXTO do prontuario — porque
 * seria facil: uma rota de download que confiasse no id do documento entregaria
 * laudo a qualquer autenticado, e nenhum teste de historico notaria.</p>
 */
class DocumentoDoProntuarioTest extends TesteDeApi {

    /**
     * Um PDF minimo de verdade: o que importa e a assinatura {@code %PDF}, que e
     * o que o service le para decidir o tipo. O resto do corpo e irrelevante
     * para a regra e nao deve fingir ser relevante.
     */
    private static byte[] pdf(String miolo) {
        return ("%PDF-1.4\n" + miolo + "\n%%EOF\n").getBytes(StandardCharsets.UTF_8);
    }

    private MockMultipartFile arquivoPdf(String nome, String miolo) {
        return new MockMultipartFile("arquivo", nome, "application/pdf", pdf(miolo));
    }

    private ResultActions anexar(String animalId, String token, String titulo, MockMultipartFile arquivo)
            throws Exception {
        return mockMvc.perform(multipart("/api/v1/animais/" + animalId + "/documentos")
                .file(arquivo)
                .param("titulo", titulo)
                .header("Authorization", "Bearer " + token));
    }

    /** Um animal novo do Lucas, ja marcado para remocao no fim do teste. */
    private String animalDoLucas(String nome) throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                {"nome":"%s","raca":"Border Collie","especie":"Canina","porte":"MEDIO",
                 "cor":"Preto e branco","sexo":"MACHO","dataNascimento":"2021-04-10",
                 "tutorId":"%s","castrado":true}"""
                .formatted(nome, SeedV2.TUTOR_LUCAS));
        animal.andExpect(status().isCreated());
        String id = idDe(animal);
        // Registrado ANTES dos documentos de proposito: a limpeza e LIFO, e a
        // chave estrangeira exige que o documento saia primeiro.
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    @Test
    @DisplayName("o tutor anexa um PDF ao pet dele e baixa de volta os mesmos bytes")
    void idaEVoltaDoArquivo() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Thor");
        byte[] enviado = pdf("hemograma completo, 12/03");

        ResultActions criado = anexar(animalId, tutor, "Hemograma completo",
                new MockMultipartFile("arquivo", "hemograma.pdf", "application/pdf", enviado));

        criado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Hemograma completo"))
                .andExpect(jsonPath("$.tipoConteudo").value("application/pdf"))
                .andExpect(jsonPath("$.tamanhoBytes").value(enviado.length))
                // Anexo do tutor nao tem clinica, e a tela precisa da diferenca.
                .andExpect(jsonPath("$.clinicaNome").doesNotExist())
                .andExpect(jsonPath("$.origem").value("TUTOR"));

        String documentoId = idDe(criado);
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + documentoId);

        // A LISTAGEM e onde uma juncao INNER esconderia justamente este
        // documento -- o que o dono do prontuario acabou de anexar.
        ResultActions lista = buscar("/api/v1/animais/" + animalId + "/documentos", tutor);
        lista.andExpect(status().isOk());
        JsonNode itens = corpoDe(lista);
        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).get("id").asText()).isEqualTo(documentoId);

        // E o byte volta identico: a ida e a volta pelo BLOB nao mexem no PDF.
        buscar("/api/v1/animais/" + animalId + "/documentos/" + documentoId + "/arquivo", tutor)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(content().bytes(enviado));
    }

    @Test
    @DisplayName("outro tutor não lista nem baixa o documento de um pet que não é dele")
    void oArquivoRespeitaODono() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Nina");

        String documentoId = idDe(anexar(animalId, lucas, "Vacina antirrábica",
                arquivoPdf("carteira.pdf", "antirrabica")).andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + documentoId);

        String maria = tokenTutor(MARIA);

        // 409, e nao 403: e o mesmo status que /historico devolve para o mesmo
        // caso. Divergir aqui daria ao cliente duas formas de ler a mesma recusa.
        buscar("/api/v1/animais/" + animalId + "/documentos", maria)
                .andExpect(status().isConflict());

        buscar("/api/v1/animais/" + animalId + "/documentos/" + documentoId + "/arquivo", maria)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o resumo de segurança não alcança o arquivo, mas a quebra de vidro alcança")
    void oNivel1NaoBaixaLaudoEaQuebraDeVidroBaixa() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Fumaca");

        String documentoId = idDe(anexar(animalId, lucas, "Raio-X do torax",
                arquivoPdf("raiox.pdf", "torax")).andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + documentoId);

        String vet = tokenVeterinaria();

        // Sem consentimento, a veterinaria chega ao nivel 1 no historico...
        buscar("/api/v1/animais/" + animalId + "/historico", vet)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelDeAcesso").value("RESUMO_DE_SEGURANCA"));

        // ...e o nivel 1 NAO alcanca o arquivo. Um PDF nao tem versao curta.
        buscar("/api/v1/animais/" + animalId + "/documentos", vet)
                .andExpect(status().isConflict());

        // A quebra de vidro e o caminho que ja existia: pede motivo e fica
        // registrada. Por isso ela libera o arquivo, sem um segundo mecanismo.
        criar("/api/v1/animais/" + animalId + "/acesso-emergencial", vet, """
                {"motivo":"Animal em parada respiratória, tutor não localizado"}""")
                .andExpect(status().isOk());

        buscar("/api/v1/animais/" + animalId + "/documentos", vet)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentoId));

        buscar("/api/v1/animais/" + animalId + "/documentos/" + documentoId + "/arquivo", vet)
                .andExpect(status().isOk());

        // E quem nao enviou nao remove -- nem tendo aberto a quebra de vidro.
        remover("/api/v1/animais/" + animalId + "/documentos/" + documentoId, vet)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("baixar o arquivo aparece na auditoria que o tutor consulta")
    void oDownloadDeixaRastro() throws Exception {
        String lucas = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Pipoca");

        String documentoId = idDe(anexar(animalId, lucas, "Exame de urina",
                arquivoPdf("urina.pdf", "urina")).andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + documentoId);

        String vet = tokenVeterinaria();
        criar("/api/v1/animais/" + animalId + "/acesso-emergencial", vet, """
                {"motivo":"Encaixe de urgência, sem agendamento"}""")
                .andExpect(status().isOk());
        buscar("/api/v1/animais/" + animalId + "/documentos/" + documentoId + "/arquivo", vet)
                .andExpect(status().isOk());

        // O tutor ve a leitura. Um download que nao registrasse acesso seria a
        // unica porta do sistema por onde se le o prontuario sem deixar rastro.
        JsonNode acessos = corpoDe(buscar("/api/v1/animais/" + animalId + "/acessos", lucas)
                .andExpect(status().isOk()));

        assertThat(acessos).isNotEmpty();
        boolean aVeterinariaAparece = false;
        for (JsonNode acesso : acessos) {
            if (VETERINARIA.equals(acesso.get("usuarioEmail").asText(null))) {
                aVeterinariaAparece = true;
            }
        }
        assertThat(aVeterinariaAparece)
                .as("o download do laudo precisa aparecer em /acessos, como aparece a leitura do histórico")
                .isTrue();
    }

    @Test
    @DisplayName("o tipo vem dos bytes do arquivo, não do que o cliente declara")
    void oContentTypeDoClienteNaoDecide() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Mel");

        // Um executavel dizendo-se PDF. O cabecalho e uma afirmacao de quem
        // envia; a assinatura do formato nao.
        MockMultipartFile disfarcado = new MockMultipartFile(
                "arquivo", "laudo.pdf", "application/pdf",
                new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03});

        anexar(animalId, tutor, "Laudo", disfarcado)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Formato nao aceito")));

        // Nada foi gravado.
        assertThat(corpoDe(buscar("/api/v1/animais/" + animalId + "/documentos", tutor))).isEmpty();
    }

    @Test
    @DisplayName("o mesmo arquivo não entra duas vezes no prontuário do mesmo pet")
    void arquivoDuplicadoERecusado() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Bidu");
        byte[] mesmoConteudo = pdf("hemograma identico");

        String documentoId = idDe(anexar(animalId, tutor, "Hemograma",
                new MockMultipartFile("arquivo", "a.pdf", "application/pdf", mesmoConteudo))
                .andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + documentoId);

        // Nome diferente, conteudo igual: e o toque duplo no botao de enviar, e
        // o "(1)" que o sistema de arquivos inventa.
        anexar(animalId, tutor, "Hemograma",
                new MockMultipartFile("arquivo", "a (1).pdf", "application/pdf", mesmoConteudo))
                .andExpect(status().isConflict());

        assertThat(corpoDe(buscar("/api/v1/animais/" + animalId + "/documentos", tutor))).hasSize(1);
    }

    @Test
    @DisplayName("o documento de um pet não sai pelo caminho de outro")
    void oCaminhoPrecisaCasarComODocumento() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String umAnimal = animalDoLucas("Estrela");
        String outroAnimal = animalDoLucas("Cometa");

        String documentoId = idDe(anexar(umAnimal, tutor, "Ultrassom",
                arquivoPdf("us.pdf", "abdomen")).andExpect(status().isCreated()));
        removerDepois("/api/v1/animais/" + umAnimal + "/documentos/" + documentoId);

        // Os dois pets sao do MESMO tutor, entao a autorizacao do caminho passa.
        // O que barra e a conferencia de que o documento e daquele animal -- sem
        // ela, o id do arquivo bastaria para tirar o laudo de qualquer paciente
        // cujo dono se conhece.
        buscar("/api/v1/animais/" + outroAnimal + "/documentos/" + documentoId + "/arquivo", tutor)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o documento sem nome é recusado, e não gravado com o título vazio")
    void tituloEObrigatorio() throws Exception {
        String tutor = tokenTutor(LUCAS);
        String animalId = animalDoLucas("Amora");

        anexar(animalId, tutor, "   ", arquivoPdf("sem-nome.pdf", "conteudo"))
                .andExpect(status().isConflict());

        assertThat(corpoDe(buscar("/api/v1/animais/" + animalId + "/documentos", tutor))).isEmpty();
    }
}
