package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entradas de borda que a API precisa recusar sem quebrar.
 *
 * <p>Os dois casos abaixo foram encontrados varrendo a API no ar, e não por
 * leitura de código — é o tipo de defeito que só aparece quando alguém manda algo
 * que ninguém imaginou mandar.</p>
 */
class EntradaDeBordaTest extends TesteDeApi {

    // ================================================================
    // Ordenação por campo inexistente
    // ================================================================

    /**
     * O Spring Data resolve o {@code sort} por reflexão na hora de montar a
     * consulta. Uma propriedade que não existe na entidade sobe como
     * {@code PropertyReferenceException} — que ninguém tratava, e virava 500.
     *
     * <p>Um cliente derrubava o endpoint com uma query string. Verificado no ar:
     * {@code ?sort=naoExiste,asc} respondia 500 com token perfeitamente válido.</p>
     */
    @ParameterizedTest(name = "?sort={0} responde 400, e nao 500")
    @ValueSource(strings = {"naoExiste,asc", "senha,desc", "tutor.senha,asc", "'; DROP TABLE,asc"})
    @DisplayName("ordenar por campo inexistente devolve 400")
    void ordenacaoInvalidaNaoQuebra(String sort) throws Exception {
        buscar("/api/v1/animais?sort=" + sort, tokenAdmin())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ordenar por campo que EXISTE continua funcionando")
    void ordenacaoValidaFunciona() throws Exception {
        buscar("/api/v1/animais?sort=nome,asc", tokenAdmin()).andExpect(status().isOk());
    }

    // ================================================================
    // Data de nascimento no futuro
    // ================================================================

    private static final String ANIMAL_NASCIDO_EM = """
            {"nome":"Viajante do Tempo","raca":"Vira-lata","especie":"CACHORRO",
             "porte":"MEDIO","cor":"Caramelo","sexo":"MACHO",
             "dataNascimento":"%s","tutorId":"%s"}""";

    /**
     * Nenhum dos sete DTOs com data de nascimento validava o futuro. Verificado no
     * ar: um animal com {@code dataNascimento} em 2999 entrava com 201.
     *
     * <p>O projeto já usava {@code @PastOrPresent} nos DTOs de pagamento — a
     * anotação estava apenas faltando aqui.</p>
     */
    @Test
    @DisplayName("animal nascido no futuro e recusado")
    void animalNascidoNoFuturoERecusado() throws Exception {
        criar("/api/v1/animais", tokenTutor(LUCAS),
                ANIMAL_NASCIDO_EM.formatted("2999-01-01", SeedV2.TUTOR_LUCAS))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("animal nascido hoje e aceito -- o limite e futuro, nao passado")
    void animalNascidoHojeEAceito() throws Exception {
        String lucas = tokenTutor(LUCAS);

        JsonNode criado = corpoDe(criar("/api/v1/animais", lucas,
                ANIMAL_NASCIDO_EM.formatted(LocalDate.now().toString(), SeedV2.TUTOR_LUCAS))
                .andExpect(status().isCreated()));

        // Apaga o que criou. A suite compartilha o banco, e o OwnershipTest afirma
        // contagens exatas -- Lucas tem UM pet, e a base tem seis. Um animal
        // deixado para tras faz tres testes de outra classe falharem, por um
        // motivo que nao tem nada a ver com o que eles verificam.
        remover("/api/v1/animais/" + criado.get("id").asText(), lucas)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH tambem recusa data de nascimento futura")
    void patchComDataFuturaERecusado() throws Exception {
        atualizarParcialmente("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS,
                tokenTutor(LUCAS), """
                        {"dataNascimento":"2999-01-01"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tutor nascido no futuro e recusado")
    void tutorNascidoNoFuturoERecusado() throws Exception {
        // A mesma lacuna existia em tutor e veterinário — sete DTOs no total.
        atualizarParcialmente("/api/v1/tutores/" + SeedV2.TUTOR_LUCAS, tokenTutor(LUCAS), """
                {"dataNascimento":"2999-01-01"}""")
                .andExpect(status().isBadRequest());
    }
}
