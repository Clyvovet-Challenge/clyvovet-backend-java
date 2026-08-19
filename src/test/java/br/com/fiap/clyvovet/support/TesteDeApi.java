package br.com.fiap.clyvovet.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes que exercitam a API pela cadeia real de filtros.
 *
 * Existe para que cada classe de teste nao precise repetir o login, o header
 * Authorization e a leitura do JSON — era o que acontecia: o mesmo helper de
 * token copiado em cada arquivo, cada um com uma regra propria para descobrir
 * a senha do usuario.
 *
 * Os usuarios sao os do DevDataSeeder sobre o seed da migration V2.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class TesteDeApi {

    protected static final String ADMIN = "admin@clyvovet.com";
    protected static final String VETERINARIA = "camila.ferreira@vetcare.com.br";
    protected static final String LUCAS = "lucas.santos@email.com";
    protected static final String MARIA = "maria.oliveira@email.com";

    private static final String SENHA_ADMIN = "admin12345";
    private static final String SENHA_VETERINARIA = "vet12345";
    private static final String SENHA_TUTOR = "tutor12345";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    private final Deque<String> aRemover = new ArrayDeque<>();

    /**
     * Marca um recurso para ser removido no fim do teste.
     *
     * Os testes gravam de verdade — nao ha transacao de teste com rollback,
     * porque ela adiaria os INSERTs para um commit que nunca acontece e
     * esconderia justamente o que se quer verificar: constraint de unicidade,
     * chave estrangeira, limite de coluna.
     *
     * A remocao acontece em ordem inversa a do cadastro, que e a ordem em que
     * as dependencias permitem: o pagamento sai antes do evento, o evento antes
     * do animal.
     */
    protected void removerDepois(String url) {
        aRemover.push(url);
    }

    @AfterEach
    void limparRecursosCriados() throws Exception {
        if (aRemover.isEmpty()) {
            return;
        }
        String admin = tokenAdmin();
        while (!aRemover.isEmpty()) {
            // Sem verificar o status: o proprio teste pode ja ter removido.
            remover(aRemover.pop(), admin);
        }
    }

    /**
     * As listagens sao cacheadas por 10 minutos. Sem limpar entre os testes, a
     * pagina montada por um deles seria servida a outro que caisse na mesma
     * chave, e o segundo passaria (ou falharia) por um motivo que nao e o dele.
     */
    @AfterEach
    void limparCaches() {
        for (String nome : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(nome);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    protected String tokenAdmin() throws Exception {
        return token(ADMIN, SENHA_ADMIN);
    }

    protected String tokenVeterinaria() throws Exception {
        return token(VETERINARIA, SENHA_VETERINARIA);
    }

    protected String tokenTutor(String email) throws Exception {
        return token(email, SENHA_TUTOR);
    }

    protected String token(String email, String senha) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("accessToken").asText();
    }

    protected ResultActions buscar(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions criar(String url, String token, String corpo) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    protected ResultActions atualizar(String url, String token, String corpo) throws Exception {
        return mockMvc.perform(put(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    protected ResultActions atualizarParcialmente(String url, String token, String corpo) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    protected ResultActions remover(String url, String token) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }

    protected JsonNode corpoDe(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    /** Id do recurso recem-criado, para encadear a proxima chamada. */
    protected String idDe(ResultActions resultado) throws Exception {
        return corpoDe(resultado).get("id").asText();
    }

    protected int totalDe(ResultActions listagem) throws Exception {
        return totalDe(corpoDe(listagem));
    }

    /**
     * Total de elementos de uma listagem paginada.
     *
     * O caminho e "page.totalElements", e nao "totalElements" na raiz, desde
     * que o WebConfig passou a serializar as paginas via PagedModel. Concentrar
     * a leitura aqui e o que faz uma mudanca dessas custar uma linha em vez de
     * uma varredura pela suite.
     */
    protected int totalDe(JsonNode corpo) {
        return corpo.get("page").get("totalElements").asInt();
    }
}
