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
        assertThat(nomesEm("/api/v1/veterinarios?nome=Camila", tokenAdmin())).containsExactly("Camila Ferreira");
    }

    @Test
    @DisplayName("veterinarios: a busca por nome ignora maiusculas e casa parcialmente")
    void buscaPorNomeEParcialEInsensivelACaixa() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/api/v1/veterinarios?nome=camila", admin)).containsExactly("Camila Ferreira");
        assertThat(nomesEm("/api/v1/veterinarios?nome=CAMILA", admin)).containsExactly("Camila Ferreira");
        assertThat(nomesEm("/api/v1/veterinarios?nome=Ferreira", admin)).containsExactly("Camila Ferreira");
    }

    @Test
    @DisplayName("veterinarios: filtro por especialidade")
    void filtraVeterinarioPorEspecialidade() throws Exception {
        assertThat(nomesEm("/api/v1/veterinarios?especialidade=Cardio", tokenAdmin()))
                .containsExactly("Rafael Matos");
    }

    @Test
    @DisplayName("veterinarios: os dois filtros se somam, nao se substituem")
    void filtrosSeSomam() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/api/v1/veterinarios?nome=Camila&especialidade=Clinica", admin))
                .containsExactly("Camila Ferreira");
        // Camila existe e Cardiologia existe, mas nao na mesma pessoa.
        assertThat(nomesEm("/api/v1/veterinarios?nome=Camila&especialidade=Cardiologia", admin)).isEmpty();
    }

    @Test
    @DisplayName("veterinarios: termo sem correspondencia devolve lista vazia")
    void termoSemCorrespondenciaDevolveVazio() throws Exception {
        assertThat(totalDe(buscar("/api/v1/veterinarios?nome=ZZZinexistente", tokenAdmin()))).isZero();
    }

    @Test
    @DisplayName("veterinarios: sem filtro, a listagem continua completa")
    void semFiltroTrazTudo() throws Exception {
        int total = totalDe(buscar("/api/v1/veterinarios", tokenAdmin()));

        assertThat(total).isGreaterThan(1);
        assertThat(totalDe(buscar("/api/v1/veterinarios?nome=Camila", tokenAdmin()))).isLessThan(total);
    }

    @Test
    @DisplayName("tutores: filtro por nome e por cidade")
    void filtraTutorPorNomeECidade() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/api/v1/tutores?nome=Lucas", admin)).containsExactly("Lucas M. Santos");
        assertThat(totalDe(buscar("/api/v1/tutores?cidade=Sao", admin))).isPositive();
        assertThat(totalDe(buscar("/api/v1/tutores?cidade=ZZZinexistente", admin))).isZero();
    }

    @Test
    @DisplayName("clinicas: filtro por nome e por cidade")
    void filtraClinicaPorNomeECidade() throws Exception {
        String admin = tokenAdmin();

        assertThat(nomesEm("/api/v1/clinicas?nome=PetMed", admin)).containsExactly("PetMed Centro");
        assertThat(totalDe(buscar("/api/v1/clinicas?cidade=Sao", admin))).isPositive();
    }

    /**
     * A busca livre da tela de atendimento do app.
     *
     * <p>O tutor procura clinica numa caixa so, e ele nao sabe (nem deve saber)
     * se "Bela Vista" e nome, bairro ou cidade. O OR da consulta e o que torna
     * essa ignorancia irrelevante -- e o que este teste protege: no dia em que
     * alguem trocar o OR por AND, a busca por bairro passa a devolver vazio sem
     * erro nenhum.</p>
     */
    @Test
    @DisplayName("clinicas: a busca livre acha por bairro, por nome e por estado")
    void buscaLivreDeClinica() throws Exception {
        String admin = tokenAdmin();

        // Bairro: "Consolacao" nao aparece no nome de nenhuma clinica do seed.
        //
        // Termo de uma palavra de proposito: o MockMvc monta a URI sem decodificar,
        // e um "%20" chegaria literal ao parametro. O caso com espaco ("Bela
        // Vista") foi verificado por curl contra a API no ar, onde a
        // decodificacao e a de verdade.
        assertThat(nomesEm("/api/v1/clinicas?busca=Consolacao", admin))
                .containsExactly("PetMed Centro");

        // Nome, pelo mesmo parametro.
        assertThat(nomesEm("/api/v1/clinicas?busca=VetCare", admin))
                .containsExactly("VetCare Prime");

        // Um termo que casa com o BAIRRO de uma clinica e com o NOME de outra
        // devolve as duas: e o OR atravessando registros diferentes.
        assertThat(nomesEm("/api/v1/clinicas?busca=Jardins", admin))
                .containsExactlyInAnyOrder("AnimalSaude SP", "CliniPet Jardins");

        // "Ipiranga" casa com o NOME e com o BAIRRO da mesma clinica. Um OR mal
        // escrito com JOIN devolveria a linha duas vezes.
        assertThat(nomesEm("/api/v1/clinicas?busca=Ipiranga", admin))
                .containsExactly("Hospital Vet Ipiranga");

        // Estado.
        assertThat(totalDe(buscar("/api/v1/clinicas?busca=SP", admin))).isPositive();

        assertThat(totalDe(buscar("/api/v1/clinicas?busca=ZZZinexistente", admin))).isZero();
    }

    /**
     * Caixa vazia nao e filtro.
     *
     * <p>O app manda {@code busca=} a cada tecla apagada. Nulo desliga o filtro;
     * string vazia casaria com tudo via {@code LIKE '%%'} -- hoje o mesmo
     * resultado por acidente, e deixaria de ser no dia em que a consulta ganhar
     * mais uma condicao.</p>
     */
    @Test
    @DisplayName("clinicas: busca vazia devolve a lista inteira")
    void buscaVaziaNaoFiltra() throws Exception {
        String admin = tokenAdmin();

        assertThat(totalDe(buscar("/api/v1/clinicas?busca=", admin)))
                .isEqualTo(totalDe(buscar("/api/v1/clinicas", admin)));
    }

    @Test
    @DisplayName("animais: filtro por nome e por especie")
    void filtraAnimalPorNomeEEspecie() throws Exception {
        String veterinaria = tokenVeterinaria();

        assertThat(nomesEm("/api/v1/animais?nome=Bolinha", veterinaria)).containsExactly("Bolinha");
        assertThat(totalDe(buscar("/api/v1/animais?especie=GATO", veterinaria))).isPositive();
        assertThat(totalDe(buscar("/api/v1/animais?especie=GATO", veterinaria)))
                .isLessThan(totalDe(buscar("/api/v1/animais", veterinaria)));
    }

    @Test
    @DisplayName("eventos clinicos: filtro pelo nome do animal e pelo tipo")
    void filtraEventoPorAnimalETipo() throws Exception {
        String veterinaria = tokenVeterinaria();
        int total = totalDe(buscar("/api/v1/eventos-clinicos", veterinaria));

        // Desde a inversao do acesso, a veterinaria ve so os atendimentos da
        // propria clinica -- e nesse recorte um filtro pode alcancar TODAS as
        // linhas visiveis. O que o filtro precisa provar e que ele recorta, e
        // nao que sobra alguma coisa de fora dele.
        int doBolinha = totalDe(buscar("/api/v1/eventos-clinicos?animalNome=Bolinha", veterinaria));
        assertThat(doBolinha).isPositive().isLessThanOrEqualTo(total);

        int consultas = totalDe(buscar("/api/v1/eventos-clinicos?tipoEvento=CONSULTA", veterinaria));
        assertThat(consultas).isPositive().isLessThanOrEqualTo(total);

        int inexistente = totalDe(buscar("/api/v1/eventos-clinicos?animalNome=NaoExiste", veterinaria));
        assertThat(inexistente).isZero();
    }

    @Test
    @DisplayName("pagamentos: filtro por status e por forma de pagamento")
    void filtraPagamentoPorStatusEForma() throws Exception {
        String veterinaria = tokenVeterinaria();
        JsonNode pagos = corpoDe(buscar("/api/v1/pagamentos?statusPagamento=PAGO", veterinaria)
                .andExpect(status().isOk()));

        assertThat(totalDe(pagos)).isPositive();
        assertThat(pagos.get("content").findValuesAsText("statusPagamento"))
                .isNotEmpty()
                .allMatch("PAGO"::equals);

        assertThat(totalDe(buscar("/api/v1/pagamentos?formaPagamento=PIX", veterinaria))).isPositive();
    }

    /**
     * A ordenacao entra na chave do cache. Se nao entrasse, a segunda chamada
     * receberia a pagina da primeira, na ordem errada.
     */
    @Test
    @DisplayName("a ordenacao pedida e respeitada nos dois sentidos")
    void ordenacaoNaoColideNoCache() throws Exception {
        String admin = tokenAdmin();

        List<String> crescente = nomesEm("/api/v1/veterinarios?sort=nome,asc", admin);
        List<String> decrescente = nomesEm("/api/v1/veterinarios?sort=nome,desc", admin);

        List<String> crescenteInvertida = new ArrayList<>(crescente);
        Collections.reverse(crescenteInvertida);

        assertThat(crescente).isSorted();
        assertThat(decrescente).isEqualTo(crescenteInvertida);
    }
}
