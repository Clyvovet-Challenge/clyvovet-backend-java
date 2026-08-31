package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressao das tres falhas de autorizacao por recurso encontradas na revisao
 * de seguranca de 31/08/2026.
 *
 * TODAS TINHAM A MESMA CAUSA: a rota nova confiava na regra de rota, que so
 * sabe responder "qual PERFIL entra aqui" e nao "de quem e este REGISTRO". O
 * projeto ja resolvia isso corretamente em /animais e /eventos-clinicos; as
 * rotas das ondas 2 e 3 nao seguiram o padrao.
 *
 * Cada teste abaixo era uma prova de conceito que passava — isto e, um exploit
 * que funcionava.
 */
class AutorizacaoPorRecursoTest extends TesteDeApi {

    // ------------------------------------------------------------------
    // VULN-001 — alerta clinico
    // ------------------------------------------------------------------

    @Test
    @DisplayName("tutor nao desativa alerta clinico do animal de outro tutor")
    void tutorNaoDesativaAlertaAlheio() throws Exception {
        // O exploit: DELETE /alertas/{id} nao tinha checagem nenhuma, e como o
        // auto-cadastro e publico, bastava criar uma conta. O alvo era o dado
        // que impede um veterinario de medicar errado um animal que nunca viu.
        ResultActions alerta = criar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/alertas",
                tokenTutor(MARIA), """
                {"tipo":"ALERGIA","descricao":"Anafilaxia a dipirona"}""");
        alerta.andExpect(status().isCreated());
        String alertaId = idDe(alerta);

        remover("/api/v1/alertas/" + alertaId, tokenTutor(LUCAS))
                .andExpect(status().isForbidden());

        // E o alerta continua valendo no resumo de seguranca.
        remover("/api/v1/alertas/" + alertaId, tokenTutor(MARIA))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("tutor nao derruba alerta registrado por veterinario, nem no proprio pet")
    void tutorNaoDerrubaAlertaDeVeterinario() throws Exception {
        // Ele pode corrigir o que ele mesmo informou; um achado clinico e outra
        // coisa — quem o retira e quem tem competencia para reavalia-lo.
        ResultActions alerta = criar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/alertas",
                tokenVeterinaria(), """
                {"tipo":"CONDICAO_CRONICA","descricao":"Cardiopatia congênita"}""");
        alerta.andExpect(status().isCreated());
        String alertaId = idDe(alerta);

        remover("/api/v1/alertas/" + alertaId, tokenTutor(LUCAS))
                .andExpect(status().isConflict());

        remover("/api/v1/alertas/" + alertaId, tokenVeterinaria())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("o tutor desativa o alerta que ele mesmo registrou")
    void tutorDesativaOProprioAlerta() throws Exception {
        // A regra nao pode ser tao apertada a ponto de impedir a correcao de um
        // erro de digitacao do proprio dono.
        ResultActions alerta = criar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/alertas",
                tokenTutor(LUCAS), """
                {"tipo":"ALERGIA","descricao":"Alergia informada por engano"}""");
        alerta.andExpect(status().isCreated());

        remover("/api/v1/alertas/" + idDe(alerta), tokenTutor(LUCAS))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // VULN-002 — agenda do veterinario
    // ------------------------------------------------------------------

    @Test
    @DisplayName("veterinario nao remove a grade de veterinario de outra clinica")
    void vetNaoRemoveGradeAlheia() throws Exception {
        // O exploit tirava a clinica concorrente inteira da busca por vagas:
        // sem grade, GET /agendamentos/vagas para de devolver horarios.
        String admin = tokenAdmin();
        ResultActions faixa = criar("/api/v1/disponibilidades", admin, """
                {"veterinarioId":"%s","diaSemana":"SEXTA","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}""".formatted(VET_DA_PETMED, LocalDate.now()));
        faixa.andExpect(status().isCreated());
        String faixaId = idDe(faixa);
        removerDepois("/api/v1/disponibilidades/" + faixaId);

        remover("/api/v1/disponibilidades/" + faixaId, tokenVeterinaria())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario nao cria grade no nome de outro profissional")
    void vetNaoCriaGradeAlheia() throws Exception {
        // Pelo POST dava para inventar disponibilidade de outro veterinario e
        // gerar agendamentos que ninguem iria atender.
        criar("/api/v1/disponibilidades", tokenVeterinaria(), """
                {"veterinarioId":"%s","diaSemana":"QUINTA","horaInicio":"08:00","horaFim":"12:00",
                 "vigenciaInicio":"%s"}""".formatted(VET_DA_PETMED, LocalDate.now()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinario nao bloqueia a agenda de outro profissional")
    void vetNaoBloqueiaAgendaAlheia() throws Exception {
        criar("/api/v1/bloqueios", tokenVeterinaria(), """
                {"veterinarioId":"%s","dataInicio":"%s","dataFim":"%s","motivo":"Sabotagem"}"""
                .formatted(VET_DA_PETMED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(30)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o veterinario gerencia a propria grade")
    void vetGerenciaAPropriaGrade() throws Exception {
        // O conserto nao pode travar o caso legitimo: a grade e do profissional,
        // e e ele quem a mantem.
        ResultActions faixa = criar("/api/v1/disponibilidades", tokenVeterinaria(), """
                {"veterinarioId":"%s","diaSemana":"DOMINGO","horaInicio":"14:00","horaFim":"18:00",
                 "vigenciaInicio":"%s"}""".formatted(SeedV2.VET_CAMILA, LocalDate.now()));
        faixa.andExpect(status().isCreated());

        remover("/api/v1/disponibilidades/" + idDe(faixa), tokenVeterinaria())
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // VULN-003 — auditoria de acesso
    // ------------------------------------------------------------------

    @Test
    @DisplayName("veterinario nao le a auditoria de acesso de um animal qualquer")
    void vetNaoLeAuditoriaAlheia() throws Exception {
        // A lista de quem leu o prontuario e a ferramenta de transparencia DO
        // TUTOR. Aberta ao corpo clinico ela vira o contrario: expoe o e-mail
        // dos profissionais de outras clinicas e revela quais delas atenderam
        // aquele paciente.
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/acessos", tokenVeterinaria())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o tutor le a auditoria do proprio animal")
    void tutorLeAPropriaAuditoria() throws Exception {
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_BOLINHA_DO_LUCAS + "/acessos", tokenTutor(LUCAS))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("tutor nao le a auditoria do animal de outro tutor")
    void tutorNaoLeAuditoriaAlheia() throws Exception {
        buscar("/api/v1/animais/" + SeedV2.ANIMAL_MIMI_DA_MARIA + "/acessos", tokenTutor(LUCAS))
                .andExpect(status().isForbidden());
    }

    /** Rafael Matos, do seed da V2: atende na PetMed, nao na VetCare da Camila. */
    private static final String VET_DA_PETMED = "33333333-3333-3333-3333-000000000002";
}
