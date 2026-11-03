package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacao do que o historico e o resumo de seguranca MONTAM: vacinas,
 * serie de peso, ultimo peso, idade, alertas, linha do tempo e contato.
 *
 * <p>Os testes de acesso ja fixam QUEM ve o que; nenhum fixava o conteudo. Escrito
 * antes de a montagem sair do HistoricoService, para que a mudanca de lugar nao
 * mude um campo.</p>
 */
class MontagemDoHistoricoTest extends TesteDeApi {

    private String animalId;
    private String chip;

    /**
     * Um pet de 27 meses com: vacina realizada ha 60 dias, consulta com 12,5 kg e
     * desfecho MELHORA ha 30, consulta com 13 kg ha 10, e um alerta de alergia.
     */
    @BeforeEach
    void petComHistorico() throws Exception {
        String lucas = tokenTutor(LUCAS);
        chip = "8%014d".formatted(System.nanoTime() % 100_000_000_000_000L);
        ResultActions animal = criar("/api/v1/animais", lucas, """
                {"nome":"Historico","raca":"SRD","especie":"Canina","porte":"MEDIO",
                 "cor":"Preto","sexo":"MACHO","dataNascimento":"%s","tutorId":"%s","microchip":"%s"}"""
                .formatted(LocalDate.now().minusMonths(27), SeedV2.TUTOR_LUCAS, chip));
        animal.andExpect(status().isCreated());
        animalId = idDe(animal);
        removerDepois("/api/v1/animais/" + animalId);

        String vet = tokenVeterinaria();
        concluir(vet, atendimento(vet, "VACINA", 60, "V10"), "{}");
        concluir(vet, atendimento(vet, "CONSULTA", 30, "Consulta"), """
                {"pesoKg":12.5,"desfecho":"MELHORA"}""");
        concluir(vet, atendimento(vet, "CONSULTA", 10, "Retorno"), """
                {"pesoKg":13.0}""");

        criar("/api/v1/animais/" + animalId + "/alertas", vet, """
                {"tipo":"ALERGIA","descricao":"Dipirona"}""").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("o historico completo traz idade, vacinas, serie de peso, alertas, linha do tempo e tutor")
    void historicoCompleto() throws Exception {
        JsonNode h = corpoDe(buscar("/api/v1/animais/" + animalId + "/historico", tokenTutor(LUCAS))
                .andExpect(status().isOk()));

        assertThat(h.get("nivelDeAcesso").asText()).isEqualTo("COMPLETO");
        assertThat(h.get("idadeEmMeses").asInt()).isEqualTo(27);
        assertThat(h.get("microchip").asText()).isEqualTo(chip);

        assertThat(textos(h.get("vacinas"), "descricao")).containsExactly("V10");
        assertThat(numeros(h.get("serieDePeso"), "pesoKg")).containsExactly(12.5, 13.0);
        assertThat(textos(h.get("alertas"), "descricao")).containsExactly("Dipirona");

        JsonNode linha = h.get("linhaDoTempo");
        assertThat(textos(linha, "descricao")).containsExactly("V10", "Consulta", "Retorno");
        assertThat(linha.get(1).get("desfecho").asText()).isEqualTo("MELHORA");

        assertThat(h.get("tutorNome").isNull()).isFalse();
        assertThat(h.has("tutorTelefone")).isTrue();
    }

    @Test
    @DisplayName("o resumo de seguranca traz o ultimo peso, vacinas, alertas e o telefone, e nada do resto")
    void resumoDeSeguranca() throws Exception {
        JsonNode r = corpoDe(buscar("/api/v1/animais/resumo?microchip=" + chip, tokenVeterinaria())
                .andExpect(status().isOk()));

        assertThat(r.get("idadeEmMeses").asInt()).isEqualTo(27);
        assertThat(r.get("ultimoPesoKg").asDouble()).isEqualTo(13.0);
        assertThat(textos(r.get("vacinas"), "descricao")).containsExactly("V10");
        assertThat(textos(r.get("alertas"), "descricao")).containsExactly("Dipirona");
        assertThat(r.has("telefoneDeEmergencia")).isTrue();
        assertThat(r.has("linhaDoTempo")).isFalse();
        assertThat(r.has("tutorNome")).isFalse();
    }

    // ------------------------------------------------------------------

    private String atendimento(String vet, String tipo, int diasAtras, String descricao) throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", vet, """
                {"data":"%s","hora":"09:00","descricao":"%s","tipoEvento":"%s",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(diasAtras), descricao, tipo,
                        SeedV2.VET_CAMILA, animalId, SeedV2.CLINICA_VETCARE));
        evento.andExpect(status().isCreated());
        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    private void concluir(String vet, String eventoId, String corpo) throws Exception {
        criar("/api/v1/eventos-clinicos/" + eventoId + "/concluir", vet, corpo).andExpect(status().isOk());
    }

    private static List<String> textos(JsonNode lista, String campo) {
        List<String> valores = new ArrayList<>();
        lista.forEach(n -> valores.add(n.get(campo).asText()));
        return valores;
    }

    private static List<Double> numeros(JsonNode lista, String campo) {
        List<Double> valores = new ArrayList<>();
        lista.forEach(n -> valores.add(n.get(campo).asDouble()));
        return valores;
    }
}
