package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.StatusEvento;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O recorte por clinica nas duas rotas do fluxo R que nasceram sem ele.
 *
 * /retornos-vencidos e /marcar-faltas recebiam a clinica pela query string, e o
 * parametro era OPCIONAL. Um veterinario que simplesmente o omitisse recebia os
 * retornos vencidos de TODAS as clinicas da plataforma — nome do animal, nome
 * do tutor e telefone junto — e a varredura de faltas reescrevia o status dos
 * agendamentos de todo mundo.
 *
 * A montagem e sempre a mesma: o ADMIN planta um registro na PETMED, e a
 * veterinaria da VETCARE tenta alcanca-lo. Cada teste falha se o recorte for
 * removido do RetornoService.
 */
class RecorteDeClinicaNoFluxoRTest extends TesteDeApi {

    @Autowired
    private EventoClinicoRepository eventoClinicoRepository;

    // ------------------------------------------------------------------
    // Retornos vencidos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a veterinária não vê o retorno vencido de outra clínica")
    void naoVeVencidoDeOutraClinica() throws Exception {
        String daPetmed = retornoVencidoNaPetmed();

        JsonNode lista = corpoDe(buscar("/api/v1/eventos-clinicos/retornos-vencidos", tokenVeterinaria())
                .andExpect(status().isOk()));

        assertThat(contem(lista, daPetmed))
                .as("evento da PETMED na lista de quem atende na VETCARE")
                .isFalse();
    }

    @Test
    @DisplayName("passar clinicaId de outra clínica não amplia o que a veterinária vê")
    void clinicaIdNaQueryStringNaoAmplia() throws Exception {
        String daPetmed = retornoVencidoNaPetmed();

        // Este era o caminho direto: pedir explicitamente a clinica alheia.
        JsonNode lista = corpoDe(buscar(
                "/api/v1/eventos-clinicos/retornos-vencidos?clinicaId=" + SeedV2.CLINICA_PETMED,
                tokenVeterinaria()).andExpect(status().isOk()));

        assertThat(contem(lista, daPetmed)).isFalse();
    }

    @Test
    @DisplayName("o ADMIN da plataforma continua vendo todas as clínicas")
    void adminVeTodas() throws Exception {
        String daPetmed = retornoVencidoNaPetmed();

        // O recorte e um filtro, nao um bloqueio: sem esta assercao, zerar a
        // lista para todo mundo passaria como se fosse a correcao.
        JsonNode lista = corpoDe(buscar("/api/v1/eventos-clinicos/retornos-vencidos", tokenAdmin())
                .andExpect(status().isOk()));

        assertThat(contem(lista, daPetmed)).isTrue();
    }

    // ------------------------------------------------------------------
    // Varredura de faltas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a varredura de faltas não alcança os agendamentos de outra clínica")
    void varreduraNaoAlcancaOutraClinica() throws Exception {
        String daPetmed = agendamentoVencidoNaPetmed();

        criar("/api/v1/eventos-clinicos/marcar-faltas", tokenVeterinaria(), "")
                .andExpect(status().isOk());

        assertThat(statusDe(daPetmed))
                .as("agendamento da PETMED reescrito pela varredura da VETCARE")
                .isEqualTo(StatusEvento.AGENDADO);
    }

    // ------------------------------------------------------------------

    /**
     * Um atendimento da PETMED concluido com retorno previsto que ja venceu.
     *
     * A data de retorno e empurrada para o passado pelo repositorio, e nao pela
     * API: o {@code @Future} do ConclusaoRequest recusa data passada — como
     * deve — e esperar trinta dias nao e opcao.
     */
    private String retornoVencidoNaPetmed() throws Exception {
        String id = eventoNaPetmed(LocalDate.now().minusDays(60), "Consulta na PETMED");

        criar("/api/v1/eventos-clinicos/" + id + "/concluir", tokenAdmin(), """
                {"desfecho":"ESTAVEL","dataRetornoPrevisto":"%s"}"""
                .formatted(LocalDate.now().plusDays(1)))
                .andExpect(status().isOk());

        EventoClinico evento = eventoClinicoRepository.findById(UUID.fromString(id)).orElseThrow();
        evento.setDataRetornoPrevisto(LocalDate.now().minusDays(30));
        eventoClinicoRepository.save(evento);

        return id;
    }

    /** Um agendamento da PETMED com data vencida — o alvo da varredura de faltas. */
    private String agendamentoVencidoNaPetmed() throws Exception {
        return eventoNaPetmed(LocalDate.now().minusDays(3), "Agendamento vencido na PETMED");
    }

    private String eventoNaPetmed(LocalDate data, String descricao) throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenAdmin(), """
                {"data":"%s","hora":"09:00","descricao":"%s","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(data, descricao, SeedV2.VET_RAFAEL_DA_PETMED,
                        SeedV2.ANIMAL_MIMI_DA_MARIA, SeedV2.CLINICA_PETMED));
        evento.andExpect(status().isCreated());

        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    private StatusEvento statusDe(String eventoId) {
        return eventoClinicoRepository.findById(UUID.fromString(eventoId))
                .orElseThrow()
                .getStatusEvento();
    }

    private boolean contem(JsonNode lista, String eventoId) {
        return StreamSupport.stream(lista.spliterator(), false)
                .anyMatch(linha -> eventoId.equals(linha.path("eventoId").asText()));
    }
}
