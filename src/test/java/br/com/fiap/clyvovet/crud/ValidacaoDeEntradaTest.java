package br.com.fiap.clyvovet.crud;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validacao de entrada, com atencao ao que a COLUNA aceita.
 *
 * Quando o DTO e mais permissivo que o banco, o texto grande passa pela
 * validacao, chega ao INSERT e estoura la — o cliente recebe erro de servidor
 * por um dado que ele mesmo poderia corrigir. Aqui se verifica o contrario:
 * que o limite e recusado em 400, com o campo indicado na resposta.
 */
class ValidacaoDeEntradaTest extends TesteDeApi {

    private static final String ANIMAL = """
            {"nome":"Teste","raca":"Vira-lata","especie":"CAO","porte":"MEDIO","cor":"Caramelo",
             "sexo":"MACHO","dataNascimento":"2021-04-01","observacao":"%s","tutorId":"%s"}""";

    private static final String EVENTO = """
            {"data":"2026-03-10","hora":"%s","descricao":"Consulta","tipoEvento":"CONSULTA",
             "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}""";

    private static final String TUTOR_COM_NUMERO = """
            {"nome":"Teste Numero","cpf":"90000000090","email":"numero@email.com","telefone":"11970001122",
             "sexo":"OUTRO","dataNascimento":"1990-05-10",
             "endereco":{"logradouro":"Av. Brasil","numero":"%s","bairro":"Jardins",
                         "cidade":"Campinas","estado":"SP","cep":"13010000"}}""";

    private static String repetir(String texto, int vezes) {
        return texto.repeat(vezes);
    }

    private String campoComErro(String corpo) throws Exception {
        JsonNode erros = objectMapper.readTree(corpo);
        return erros.get(0).get("campo").asText();
    }

    @Test
    @DisplayName("observacao do animal maior que a coluna responde 400, e nao erro de servidor")
    void observacaoAcimaDoLimiteResponde400() throws Exception {
        String corpo = criar("/animais", tokenAdmin(),
                ANIMAL.formatted(repetir("a", 1001), SeedV2.TUTOR_LUCAS))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(campoComErro(corpo)).isEqualTo("observacao");
    }

    @Test
    @DisplayName("observacao no limite exato da coluna e aceita")
    void observacaoNoLimiteEAceita() throws Exception {
        String id = corpoDe(criar("/animais", tokenAdmin(),
                ANIMAL.formatted(repetir("a", 1000), SeedV2.TUTOR_LUCAS))
                .andExpect(status().isCreated())).get("id").asText();

        removerDepois("/animais/" + id);
    }

    /** A coluna hora_evento e VARCHAR2(5): so cabe HH:mm. */
    @Test
    @DisplayName("hora fora do formato HH:mm responde 400")
    void horaForaDoFormatoResponde400() throws Exception {
        String vet = tokenVeterinaria();

        for (String horaInvalida : new String[]{"14:30:00", "", "25:00", "9:00", "manha"}) {
            criar("/eventos-clinicos", vet,
                    EVENTO.formatted(horaInvalida, SeedV2.VET_CAMILA,
                            SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_PETMED))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("hora no formato HH:mm e aceita nos extremos do dia")
    void horaValidaEAceita() throws Exception {
        String vet = tokenVeterinaria();

        for (String horaValida : new String[]{"00:00", "23:59"}) {
            String id = corpoDe(criar("/eventos-clinicos", vet,
                    EVENTO.formatted(horaValida, SeedV2.VET_CAMILA,
                            SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_PETMED))
                    .andExpect(status().isCreated())).get("id").asText();
            removerDepois("/eventos-clinicos/" + id);
        }
    }

    @Test
    @DisplayName("numero do endereco maior que a coluna responde 400")
    void numeroDeEnderecoAcimaDoLimiteResponde400() throws Exception {
        String corpo = criar("/tutores", tokenAdmin(), TUTOR_COM_NUMERO.formatted(repetir("9", 11)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(campoComErro(corpo)).isEqualTo("endereco.numero");
    }

    @Test
    @DisplayName("valor de pagamento negativo responde 400")
    void valorNegativoResponde400() throws Exception {
        criar("/pagamentos", tokenVeterinaria(), """
                {"formaPagamento":"PIX","valor":-10.00,"dataPagamento":"2026-03-10",
                 "statusPagamento":"PAGO","eventoClinicoId":"%s"}""".formatted(SeedV2.ID_INEXISTENTE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("corpo vazio lista todos os campos obrigatorios")
    void corpoVazioListaCamposObrigatorios() throws Exception {
        String corpo = criar("/animais", tokenAdmin(), "{}")
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(corpo).findValuesAsText("campo"))
                .contains("nome", "raca", "especie", "porte", "cor", "sexo", "dataNascimento", "tutorId");
    }
}
