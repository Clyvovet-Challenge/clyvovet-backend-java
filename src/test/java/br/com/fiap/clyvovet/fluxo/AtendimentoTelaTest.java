package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeTela;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O fluxo R pela tela: a veterinaria conclui o atendimento, marca o retorno e
 * ve quem nao voltou.
 *
 * <p>As regras sao as do {@link RetornoFluxoTest} -- o service e um so. Aqui se
 * prova o que so a tela tem: o formulario chega ao service, e a recusa volta ao
 * lado do campo certo.</p>
 */
class AtendimentoTelaTest extends TesteDeTela {

    private static final String ATENDIMENTOS = "/veterinario/atendimentos";

    @Autowired
    private EventoClinicoRepository eventoClinicoRepository;

    private String eventoDeOntem;

    /** Uma segunda futura, coberta pela faixa de segunda criada abaixo. */
    private LocalDate segundaFutura;

    /**
     * O mesmo cenario do {@link RetornoFluxoTest}: um atendimento de ontem, que
     * so o POST /eventos-clinicos consegue criar (o agendamento recusa o
     * passado), e uma faixa de segunda para o retorno ter onde cair.
     */
    @BeforeEach
    void registrarAtendimentoDeOntem() throws Exception {
        eventoDeOntem = eventoNaVetcare(LocalDate.now().minusDays(1));

        segundaFutura = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
        ResultActions faixa = criar("/api/v1/disponibilidades", tokenAdmin(), """
                {"veterinarioId":"%s","diaSemana":"SEGUNDA","horaInicio":"08:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}"""
                .formatted(SeedV2.VET_CAMILA, LocalDate.now().minusDays(1)));
        faixa.andExpect(status().isCreated());
        removerDepois("/api/v1/disponibilidades/" + idDe(faixa));
    }

    @Test
    @DisplayName("a lista mostra o atendimento de ontem com o botao de concluir")
    void listaOsAtendimentos() throws Exception {
        abrir(ATENDIMENTOS, VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir")));
    }

    @Test
    @DisplayName("concluir com peso valido grava o atendimento e volta a lista")
    void concluiComPesoValido() throws Exception {
        enviarFormulario(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir", comoUsuario(VETERINARIA),
                "pesoKg", "12.4",
                "desfecho", "MELHORA",
                "descricao", "Quadro estavel")
                .andExpect(redirectedUrl(ATENDIMENTOS))
                .andExpect(flash().attributeExists("sucesso"));

        JsonNode evento = corpoDe(buscar("/api/v1/eventos-clinicos/" + eventoDeOntem, tokenVeterinaria()));
        assertThat(evento.get("statusEvento").asText()).isEqualTo("REALIZADO");
        assertThat(evento.get("desfecho").asText()).isEqualTo("MELHORA");
        assertThat(evento.get("pesoKg").decimalValue()).isEqualByComparingTo("12.4");
    }

    @Test
    @DisplayName("os desfechos aparecem com acento, e o valor enviado continua sendo o da API")
    void desfechosComAcento() throws Exception {
        abrir(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir", VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Estável<")))
                .andExpect(content().string(containsString(">Óbito<")))
                .andExpect(content().string(containsString("value=\"OBITO\"")));
    }

    @Test
    @DisplayName("a descricao atual ja vem no campo: quem conclui ve o texto que esta trocando")
    void descricaoAtualVemPreenchida() throws Exception {
        abrir(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir", VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Descrição do atendimento")))
                .andExpect(content().string(containsString(">Consulta de rotina</textarea>")));
    }

    @Test
    @DisplayName("peso negativo volta ao formulario com o erro ao lado do campo peso")
    void pesoNegativoMostraErroNoCampo() throws Exception {
        enviarFormulario(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir", comoUsuario(VETERINARIA),
                "pesoKg", "-3")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("conclusao", "pesoKg"))
                .andExpect(content().string(containsString("Peso deve ser positivo")));
    }

    @Test
    @DisplayName("concluir duas vezes mostra a recusa do service no topo do formulario")
    void concluirDuasVezesMostraErroGeral() throws Exception {
        concluirPelaApi(eventoDeOntem);

        // O campo da recusa e "statusEvento", que nao existe no formulario:
        // a mensagem vira erro geral.
        enviarFormulario(ATENDIMENTOS + "/" + eventoDeOntem + "/concluir", comoUsuario(VETERINARIA))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("conclusao"))
                .andExpect(content().string(containsString("aviso-erro")));
    }

    @Test
    @DisplayName("marcar retorno valido cria o retorno ligado a consulta de origem")
    void marcaRetorno() throws Exception {
        concluirPelaApi(eventoDeOntem);

        enviarFormulario(ATENDIMENTOS + "/" + eventoDeOntem + "/retorno", comoUsuario(VETERINARIA),
                "data", segundaFutura.toString(),
                "hora", "08:00")
                .andExpect(redirectedUrl(ATENDIMENTOS))
                .andExpect(flash().attributeExists("sucesso"));

        EventoClinico retorno = eventoClinicoRepository.findAll().stream()
                .filter(e -> e.getEventoOrigem() != null
                        && e.getEventoOrigem().getId().toString().equals(eventoDeOntem))
                .findFirst()
                .orElseThrow(() -> new AssertionError("A tela nao gravou o retorno"));
        removerDepois("/api/v1/eventos-clinicos/" + retorno.getId());
        assertThat(retorno.getData()).isEqualTo(segundaFutura);
    }

    @Test
    @DisplayName("atendimento concluido sem retorno mostra o botao de marcar retorno")
    void semRetornoMostraOBotao() throws Exception {
        concluirPelaApi(eventoDeOntem);

        abrir(ATENDIMENTOS, VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ATENDIMENTOS + "/" + eventoDeOntem + "/retorno")));
    }

    @Test
    @DisplayName("com retorno em aberto o botao some: a R14 recusaria o clique")
    void retornoEmAbertoEscondeOBotao() throws Exception {
        concluirPelaApi(eventoDeOntem);
        ResultActions retorno = criar("/api/v1/eventos-clinicos/" + eventoDeOntem + "/retorno", tokenVeterinaria(),
                """
                {"data":"%s","hora":"08:00"}""".formatted(segundaFutura));
        retorno.andExpect(status().isCreated());
        removerDepois("/api/v1/eventos-clinicos/" + idDe(retorno));

        abrir(ATENDIMENTOS, VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ATENDIMENTOS + "/" + eventoDeOntem + "/retorno"))))
                .andExpect(content().string(containsString("Retorno marcado")));
    }

    @Test
    @DisplayName("retorno no passado volta ao formulario com o erro no campo data")
    void retornoNoPassadoMostraErroNoCampo() throws Exception {
        concluirPelaApi(eventoDeOntem);

        enviarFormulario(ATENDIMENTOS + "/" + eventoDeOntem + "/retorno", comoUsuario(VETERINARIA),
                "data", LocalDate.now().minusDays(1).toString(),
                "hora", "08:00")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("retorno", "data"))
                .andExpect(content().string(containsString("O retorno precisa ser marcado para uma data futura")));
    }

    @Test
    @DisplayName("a veterinaria nao conclui atendimento de outra clinica")
    void outraClinicaRecebe403() throws Exception {
        String daPetmed = eventoNaPetmed(LocalDate.now().minusDays(1));

        enviarFormulario(ATENDIMENTOS + "/" + daPetmed + "/concluir", comoUsuario(VETERINARIA),
                "pesoKg", "5")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("os retornos vencidos aparecem com o contato do tutor")
    void listaRetornosVencidos() throws Exception {
        String antigo = eventoNaVetcare(LocalDate.now().minusDays(60));
        criar("/api/v1/eventos-clinicos/" + antigo + "/concluir", tokenVeterinaria(), """
                {"dataRetornoPrevisto":"%s"}""".formatted(LocalDate.now().plusDays(1)))
                .andExpect(status().isOk());

        // O @Future do formulario recusa data passada, como deve; o vencimento
        // so se simula recuando a data direto no banco.
        EventoClinico evento = eventoClinicoRepository.findById(UUID.fromString(antigo)).orElseThrow();
        evento.setDataRetornoPrevisto(LocalDate.now().minusDays(30));
        eventoClinicoRepository.save(evento);

        abrir("/veterinario/retornos-vencidos", VETERINARIA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bolinha")))
                .andExpect(content().string(containsString("30 dias")));
    }

    private void concluirPelaApi(String id) throws Exception {
        criar("/api/v1/eventos-clinicos/" + id + "/concluir", tokenVeterinaria(), "{}")
                .andExpect(status().isOk());
    }

    private String eventoNaVetcare(LocalDate data) throws Exception {
        return evento(data, SeedV2.VET_CAMILA, SeedV2.ANIMAL_BOLINHA_DO_LUCAS, SeedV2.CLINICA_VETCARE);
    }

    private String eventoNaPetmed(LocalDate data) throws Exception {
        return evento(data, SeedV2.VET_RAFAEL_DA_PETMED, SeedV2.ANIMAL_MIMI_DA_MARIA, SeedV2.CLINICA_PETMED);
    }

    private String evento(LocalDate data, String veterinarioId, String animalId, String clinicaId) throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", tokenAdmin(), """
                {"data":"%s","hora":"09:00","descricao":"Consulta de rotina","tipoEvento":"CONSULTA",
                 "veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(data, veterinarioId, animalId, clinicaId));
        evento.andExpect(status().isCreated());
        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }
}
