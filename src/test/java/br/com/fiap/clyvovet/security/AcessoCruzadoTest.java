package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escrita e leitura entre clinicas.
 *
 * Complementa o AcessoPorClinicaTest, que cobre a LEITURA: aqui estao os
 * caminhos de ESCRITA e os relatorios agregados, que ficaram de fora quando a
 * ruptura B1 foi implementada. Cada teste nasceu como prova de conceito de uma
 * vulnerabilidade confirmada e foi visto falhando antes da correcao.
 *
 * A Camila e da VetCare; tudo aqui acontece na PetMed, a concorrente.
 */
class AcessoCruzadoTest extends TesteDeApi {

    @Test
    @DisplayName("veterinario de outra clinica NAO deve estornar o pagamento")
    void estornoCruzado() throws Exception {
        String admin = tokenAdmin();
        String pagamentoId = pagamentoPagoNaPetMed(admin);

        criar("/api/v1/pagamentos/" + pagamentoId + "/estornar", tokenVeterinaria(), """
                {"motivo":"estorno por quem nao e da clinica"}""")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario de outra clinica NAO deve apagar o atendimento")
    void deleteCruzadoDeEvento() throws Exception {
        String admin = tokenAdmin();
        String eventoId = eventoNaPetMed(admin);

        // A leitura ja e 403 (ruptura B1). A escrita precisa ser tambem.
        buscar("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isForbidden());

        remover("/api/v1/eventos-clinicos/" + eventoId, tokenVeterinaria())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o extrato do tutor NAO deve entregar pagamento de outra clinica")
    void extratoVazaPagamentoDeOutraClinica() throws Exception {
        String admin = tokenAdmin();
        String pagamentoId = pagamentoPagoNaPetMed(admin);

        // Controle positivo: para o ADMIN o pagamento ESTA la. Sem esta linha,
        // um setup que falhasse em silencio deixaria o teste passar sem provar
        // filtro nenhum -- so provaria que o registro nunca existiu.
        assertThat(extratoContem(admin, pagamentoId))
                .as("controle: o ADMIN enxerga o pagamento da PetMed")
                .isTrue();

        assertThat(extratoContem(tokenVeterinaria(), pagamentoId))
                .as("pagamento da PetMed visivel no extrato lido pela veterinaria da VetCare")
                .isFalse();
    }

    @Test
    @DisplayName("a inadimplencia NAO deve listar atendimento de outra clinica")
    void inadimplenciaVazaOutraClinica() throws Exception {
        String admin = tokenAdmin();
        String eventoId = eventoRealizadoComDividaNaPetMed(admin);

        // Controle positivo: ver a nota no teste do extrato.
        assertThat(inadimplenciaContem(admin, eventoId))
                .as("controle: o ADMIN enxerga o devedor da PetMed")
                .isTrue();

        assertThat(inadimplenciaContem(tokenVeterinaria(), eventoId))
                .as("devedor da PetMed, com nome e telefone do tutor, visivel para a VetCare")
                .isFalse();
    }

    @Test
    @DisplayName("veterinario NAO deve religar o resumo que o tutor desligou")
    void veterinarioReligaOptOutDoTutor() throws Exception {
        String animalId = animalNovo();

        // O tutor desliga o resumo de seguranca do proprio animal.
        atualizarParcialmente("/api/v1/animais/" + animalId, tokenTutor(LUCAS), """
                {"resumoDeSegurancaAtivo":false}""")
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.resumoDeSegurancaAtivo").value(false));

        // Um veterinario qualquer religa.
        atualizarParcialmente("/api/v1/animais/" + animalId, tokenVeterinaria(), """
                {"resumoDeSegurancaAtivo":true}""")
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------

    private boolean extratoContem(String token, String pagamentoId) throws Exception {
        JsonNode extrato = corpoDe(buscar(
                "/api/v1/tutores/" + SeedV2.TUTOR_LUCAS + "/extrato", token));
        for (JsonNode pagamento : extrato.get("pagamentos")) {
            if (pagamentoId.equals(pagamento.get("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean inadimplenciaContem(String token, String eventoId) throws Exception {
        JsonNode lista = corpoDe(buscar(
                "/api/v1/pagamentos/inadimplencia?diasMinimos=0", token));
        for (JsonNode linha : lista) {
            if (eventoId.equals(linha.get("eventoId").asText())) {
                return true;
            }
        }
        return false;
    }

    private String animalNovo() throws Exception {
        ResultActions animal = criar("/api/v1/animais", tokenTutor(LUCAS), """
                {"nome":"Paciente PoC","raca":"SRD","especie":"Canina","porte":"MEDIO",
                 "cor":"Caramelo","sexo":"MACHO","dataNascimento":"2023-05-01",
                 "tutorId":"%s"}""".formatted(SeedV2.TUTOR_LUCAS));
        animal.andExpect(status().isCreated());
        String id = idDe(animal);
        removerDepois("/api/v1/animais/" + id);
        return id;
    }

    private String eventoNaPetMed(String admin) throws Exception {
        ResultActions evento = criar("/api/v1/eventos-clinicos", admin, """
                {"data":"%s","hora":"10:00","descricao":"Consulta na concorrente",
                 "tipoEvento":"CONSULTA","veterinarioId":"33333333-3333-3333-3333-000000000002",
                 "animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(2), animalNovo(), SeedV2.CLINICA_PETMED));
        evento.andExpect(status().isCreated());
        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);
        return id;
    }

    private String eventoRealizadoComDividaNaPetMed(String admin) throws Exception {
        String servicoId = idDe(criar("/api/v1/servicos", admin, """
                {"clinicaId":"%s","nome":"Consulta PoC %s","tipoEvento":"CONSULTA",
                 "preco":150.00,"duracaoMinutos":30}"""
                .formatted(SeedV2.CLINICA_PETMED, System.nanoTime())));

        ResultActions evento = criar("/api/v1/eventos-clinicos", admin, """
                {"data":"%s","hora":"10:00","descricao":"Consulta com divida",
                 "tipoEvento":"CONSULTA","veterinarioId":"33333333-3333-3333-3333-000000000002",
                 "animalId":"%s","clinicaId":"%s","servicoId":"%s"}"""
                .formatted(LocalDate.now().minusDays(2), animalNovo(),
                        SeedV2.CLINICA_PETMED, servicoId));
        evento.andExpect(status().isCreated());
        String id = idDe(evento);
        removerDepois("/api/v1/eventos-clinicos/" + id);

        // O POST ignora statusEvento de proposito: concluir e o unico caminho
        // para REALIZADO, e so REALIZADO entra em realizadosAte().
        criar("/api/v1/eventos-clinicos/" + id + "/concluir", admin, "{}")
                .andExpect(status().isOk());
        return id;
    }

    private String pagamentoPagoNaPetMed(String admin) throws Exception {
        String eventoId = eventoNaPetMed(admin);

        ResultActions pagamento = criar("/api/v1/pagamentos", admin, """
                {"formaPagamento":"PIX","valor":200.00,"descricao":"Consulta PetMed",
                 "eventoClinicoId":"%s","statusPagamento":"PENDENTE"}""".formatted(eventoId));
        pagamento.andExpect(status().isCreated());
        String id = idDe(pagamento);
        removerDepois("/api/v1/pagamentos/" + id);

        criar("/api/v1/pagamentos/" + id + "/confirmar", admin, """
                {"dataPagamento":"%s","formaPagamento":"PIX"}""".formatted(LocalDate.now()))
                .andExpect(status().isOk());
        return id;
    }
}
