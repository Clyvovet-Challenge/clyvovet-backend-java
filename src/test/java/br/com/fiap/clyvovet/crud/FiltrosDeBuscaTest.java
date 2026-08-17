package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtros das listagens, sobre o seed da migration V2.
 *
 * Esta classe existe por causa de um defeito que passou despercebido justamente
 * por nao haver teste de filtro: o Hibernate gera {@code LIKE ... ESCAPE ''} e,
 * na semantica do Oracle — que o H2 imita com MODE=Oracle —, string vazia e
 * nulo. O predicado virava {@code ESCAPE NULL} e nunca casava, entao TODA busca
 * por texto devolvia lista vazia. Os testes de listagem que existiam passavam
 * porque so exercitavam o recorte por tutor, com o texto indo nulo.
 *
 * Por isso cada filtro aqui e conferido nos dois sentidos: o que ele traz e o
 * que ele deixa de fora.
 */
class FiltrosDeBuscaTest extends TesteDeApi {

    private List<String> nomesEm(String url, String token) throws Exception {
        JsonNode pagina = corpoDe(buscar(url, token).andExpect(status().isOk()));
        return pagina.get("content").findValuesAsText("nome");
    }

    @Test
    @DisplayName("veterinarios: filtro por nome encontra o registro do seed")
    void filtraVeterinarioPorNome() throws Exception {
        assertThat(nomesEm("/veterinarios?nome=Camila", tokenAdmin())).containsExactly("Camila Ferreira");
    }

    @Test
    @DisplayName("veterinarios: a busca por nome ignora maiusculas e casa parcialmente")
    void buscaPorNomeEParcialEInsensivelACaixa() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/veterinarios?nome=camila", admin)).containsExactly("Camila Ferreira");
        assertThat(nomesEm("/veterinarios?nome=CAMILA", admin)).containsExactly("Camila Ferreira");
        assertThat(nomesEm("/veterinarios?nome=Ferreira", admin)).containsExactly("Camila Ferreira");
    }

    @Test
    @DisplayName("veterinarios: filtro por especialidade")
    void filtraVeterinarioPorEspecialidade() throws Exception {
        assertThat(nomesEm("/veterinarios?especialidade=Cardio", tokenAdmin()))
                .containsExactly("Rafael Matos");
    }

    @Test
    @DisplayName("veterinarios: os dois filtros se somam, nao se substituem")
    void filtrosSeSomam() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/veterinarios?nome=Camila&especialidade=Clinica", admin))
                .containsExactly("Camila Ferreira");
        // Camila existe e Cardiologia existe, mas nao na mesma pessoa.
        assertThat(nomesEm("/veterinarios?nome=Camila&especialidade=Cardiologia", admin)).isEmpty();
    }

    @Test
    @DisplayName("veterinarios: termo sem correspondencia devolve lista vazia")
    void termoSemCorrespondenciaDevolveVazio() throws Exception {
        assertThat(totalDe(buscar("/veterinarios?nome=ZZZinexistente", tokenAdmin()))).isZero();
    }

    @Test
    @DisplayName("veterinarios: sem filtro, a listagem continua completa")
    void semFiltroTrazTudo() throws Exception {
        int total = totalDe(buscar("/veterinarios", tokenAdmin()));

        assertThat(total).isGreaterThan(1);
        assertThat(totalDe(buscar("/veterinarios?nome=Camila", tokenAdmin()))).isLessThan(total);
    }

    @Test
    @DisplayName("tutores: filtro por nome e por cidade")
    void filtraTutorPorNomeECidade() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/tutores?nome=Lucas", admin)).containsExactly("Lucas M. Santos");
        assertThat(totalDe(buscar("/tutores?cidade=Sao", admin))).isPositive();
        assertThat(totalDe(buscar("/tutores?cidade=ZZZinexistente", admin))).isZero();
    }

    @Test
    @DisplayName("clinicas: filtro por nome e por cidade")
    void filtraClinicaPorNomeECidade() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/clinicas?nome=PetMed", admin)).containsExactly("PetMed Centro");
        assertThat(totalDe(buscar("/clinicas?cidade=Sao", admin))).isPositive();
    }

    @Test
    @DisplayName("animais: filtro por nome e por especie")
    void filtraAnimalPorNomeEEspecie() throws Exception {
        String veterinaria = tokenVeterinaria();

        assertThat(nomesEm("/animais?nome=Bolinha", veterinaria)).containsExactly("Bolinha");
        assertThat(totalDe(buscar("/animais?especie=GATO", veterinaria))).isPositive();
        assertThat(totalDe(buscar("/animais?especie=GATO", veterinaria)))
                .isLessThan(totalDe(buscar("/animais", veterinaria)));
    }

    @Test
    @DisplayName("eventos clinicos: filtro pelo nome do animal e pelo tipo")
    void filtraEventoPorAnimalETipo() throws Exception {
        String veterinaria = tokenVeterinaria();
        int total = totalDe(buscar("/eventos-clinicos", veterinaria));

        int doBolinha = totalDe(buscar("/eventos-clinicos?animalNome=Bolinha", veterinaria));
        assertThat(doBolinha).isPositive().isLessThan(total);

        int consultas = totalDe(buscar("/eventos-clinicos?tipoEvento=CONSULTA", veterinaria));
        assertThat(consultas).isPositive().isLessThan(total);
    }

    @Test
    @DisplayName("pagamentos: filtro por status e por forma de pagamento")
    void filtraPagamentoPorStatusEForma() throws Exception {
        String veterinaria = tokenVeterinaria();
        JsonNode pagos = corpoDe(buscar("/pagamentos?statusPagamento=PAGO", veterinaria)
                .andExpect(status().isOk()));

        assertThat(pagos.get("totalElements").asInt()).isPositive();
        assertThat(pagos.get("content").findValuesAsText("statusPagamento"))
                .isNotEmpty()
                .allMatch("PAGO"::equals);

        assertThat(totalDe(buscar("/pagamentos?formaPagamento=PIX", veterinaria))).isPositive();
    }

    /**
     * A ordenacao entra na chave do cache. Se nao entrasse, a segunda chamada
     * receberia a pagina da primeira, na ordem errada.
     */
    @Test
    @DisplayName("a ordenacao pedida e respeitada nos dois sentidos")
    void ordenacaoNaoColideNoCache() throws Exception {
        String admin = tokenAdmin();

        List<String> crescente = nomesEm("/veterinarios?sort=nome,asc", admin);
        List<String> decrescente = nomesEm("/veterinarios?sort=nome,desc", admin);

        List<String> crescenteInvertida = new ArrayList<>(crescente);
        Collections.reverse(crescenteInvertida);

        assertThat(crescente).isSorted();
        assertThat(decrescente).isEqualTo(crescenteInvertida);
    }
}
