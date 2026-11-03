package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacao da validacao do arquivo anexado ao prontuario: o que a API
 * responde hoje para cada caso que o DocumentoDoProntuarioTest nao cobre.
 *
 * <p>Escrito antes de a validacao sair do DocumentoClinicoService para uma
 * classe propria. A mudanca de lugar nao pode mudar uma mensagem.</p>
 */
class ValidacaoDeArquivoTest extends TesteDeApi {

    private String animalId;
    private String lucas;

    @BeforeEach
    void animalDoLucas() throws Exception {
        lucas = tokenTutor(LUCAS);
        ResultActions animal = criar("/api/v1/animais", lucas, """
                {"nome":"Arquivos","raca":"SRD","especie":"Canina","porte":"MEDIO",
                 "cor":"Preto","sexo":"MACHO","dataNascimento":"2021-04-10","tutorId":"%s"}"""
                .formatted(SeedV2.TUTOR_LUCAS));
        animal.andExpect(status().isCreated());
        animalId = idDe(animal);
        removerDepois("/api/v1/animais/" + animalId);
    }

    @Test
    @DisplayName("titulo com mais de 150 caracteres e recusado com a mensagem de hoje")
    void tituloLongo() throws Exception {
        anexar("x".repeat(151), arquivo("laudo.pdf", pdf("a")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("titulo"))
                .andExpect(jsonPath("$.mensagem").value("O nome do documento passa de 150 caracteres."));
    }

    @Test
    @DisplayName("titulo e aparado nas pontas antes de gravar")
    void tituloAparado() throws Exception {
        ResultActions r = anexar("  Hemograma  ", arquivo("h.pdf", pdf("b")));
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.titulo").value("Hemograma"));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + idDe(r));
    }

    @Test
    @DisplayName("arquivo acima de 8 MB e recusado com a mensagem de hoje")
    void arquivoGrande() throws Exception {
        byte[] grande = new byte[8 * 1024 * 1024 + 1];
        byte[] cabeca = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(cabeca, 0, grande, 0, cabeca.length);
        Arrays.fill(grande, cabeca.length, grande.length, (byte) 'a');

        anexar("Grande", arquivo("grande.pdf", grande))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O arquivo passa de 8 MB."));
    }

    @Test
    @DisplayName("PNG e aceito pelo tipo lido dos bytes")
    void png() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};
        ResultActions r = anexar("Raio X", arquivo("raio.png", png));
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.tipoConteudo").value("image/png"));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + idDe(r));
    }

    @Test
    @DisplayName("o nome do arquivo perde barras, aspas, quebras de linha e '..'")
    void nomeLimpo() throws Exception {
        ResultActions r = anexar("Laudo", arquivo("../la\"udo\r\n.pdf", pdf("c")));
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.nomeArquivo").value("laudo.pdf"));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + idDe(r));
    }

    @Test
    @DisplayName("sem nome de arquivo, o nome vira 'documento' com a extensao do tipo real")
    void nomePadrao() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 4, 5, 6};
        ResultActions r = anexar("Foto", arquivo("", jpeg));
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.nomeArquivo").value("documento.jpg"));
        removerDepois("/api/v1/animais/" + animalId + "/documentos/" + idDe(r));
    }

    // ------------------------------------------------------------------

    private static byte[] pdf(String miolo) {
        return ("%PDF-1.4\n" + miolo + System.nanoTime() + "\n%%EOF\n").getBytes(StandardCharsets.UTF_8);
    }

    private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo);
    }

    private ResultActions anexar(String titulo, MockMultipartFile arquivo) throws Exception {
        return mockMvc.perform(multipart("/api/v1/animais/" + animalId + "/documentos")
                .file(arquivo)
                .param("titulo", titulo)
                .header("Authorization", "Bearer " + lucas));
    }
}
