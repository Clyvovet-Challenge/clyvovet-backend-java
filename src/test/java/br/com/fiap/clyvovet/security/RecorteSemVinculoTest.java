package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O usuario que existe, autentica, e nao esta ligado a ninguem.
 *
 * POST /auth/usuarios aceita {"perfil":"VETERINARIO"} sem veterinarioId — a
 * validacao de vinculo so recusa o cruzado (TUTOR apontando para veterinario e
 * vice-versa), nunca o ausente. O usuario nasce valido e sem lastro.
 *
 * Antes, o recorte desse usuario era (null, null), que e exatamente o do ADMIN:
 * nulo significa "sem recorte nesta dimensao" na consulta. O cadastro
 * incompleto nao tirava acesso — DAVA. Hoje a falta de vinculo vira um id que
 * nenhuma linha carrega, e a mesma clausula devolve pagina vazia.
 */
class RecorteSemVinculoTest extends TesteDeApi {

    @Test
    @DisplayName("veterinário sem clínica vinculada não lê atendimento nenhum")
    void veterinarioSemClinicaNaoLeAtendimentos() throws Exception {
        String orfao = tokenSemVinculo("VETERINARIO");

        // A comparacao com o ADMIN e o que da sentido ao zero: sem ela, uma
        // base vazia faria o teste passar sozinho.
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos", tokenAdmin()))).isPositive();
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos", orfao))).isZero();
    }

    @Test
    @DisplayName("veterinário sem clínica vinculada não registra atendimento em clínica alguma")
    void veterinarioSemClinicaNaoRegistraAtendimento() throws Exception {
        String orfao = tokenSemVinculo("VETERINARIO");

        // Este era o lado de ESCRITA do mesmo buraco: a guarda de clinica
        // propria comparava com null, e null passava por qualquer clinica.
        criar("/api/v1/eventos-clinicos", orfao, """
                {"data":"%s","hora":"10:00","descricao":"Atendimento sem lastro",
                 "tipoEvento":"CONSULTA","veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(1), SeedV2.VET_RAFAEL_DA_PETMED,
                        SeedV2.ANIMAL_MIMI_DA_MARIA, SeedV2.CLINICA_PETMED))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("tutor sem tutor vinculado não lê animal nenhum")
    void tutorSemVinculoNaoLeAnimais() throws Exception {
        String orfao = tokenSemVinculo("TUTOR");

        // A outra dimensao do mesmo record. O cadastro do animal e nivel 0 para
        // o corpo clinico, mas o TUTOR so alcanca os proprios — e sem vinculo
        // ele nao tem "proprios".
        assertThat(totalDe(buscar("/api/v1/animais", tokenAdmin()))).isPositive();
        assertThat(totalDe(buscar("/api/v1/animais", orfao))).isZero();
    }

    // ------------------------------------------------------------------

    /**
     * Cria um usuario do perfil pedido, sem tutorId nem veterinarioId, e devolve
     * o token dele.
     *
     * E-mail unico porque nao ha DELETE de usuario para limpar depois, e a
     * suite roda tambem contra um banco que persiste entre execucoes.
     */
    private String tokenSemVinculo(String perfil) throws Exception {
        String email = "sem-vinculo-" + UUID.randomUUID() + "@teste.com";
        criar("/api/v1/auth/usuarios", tokenAdmin(), """
                {"email":"%s","senha":"senha12345","perfil":"%s"}"""
                .formatted(email, perfil))
                .andExpect(status().isCreated());
        return token(email, "senha12345");
    }
}
