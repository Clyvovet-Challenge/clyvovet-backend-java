package br.com.fiap.clyvovet.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A invariante que o recorte existe para sustentar: dois usuarios que enxergam
 * coisas diferentes nunca compartilham chave de cache.
 *
 * Antes, a chave era montada a mao em cada service e podia ficar mais ESTREITA
 * que o filtro da consulta — foi assim que a ruptura B1 quase entregou a pagina
 * de um veterinario ao de outra clinica. Com a chave saindo do proprio recorte,
 * isso deixa de depender de quem lembrou.
 */
class RecorteDeAcessoTest {

    private static final UUID TUTOR_A = UUID.randomUUID();
    private static final UUID TUTOR_B = UUID.randomUUID();
    private static final UUID CLINICA_A = UUID.randomUUID();
    private static final UUID CLINICA_B = UUID.randomUUID();

    @Test
    @DisplayName("tutores diferentes nao colidem na chave")
    void tutoresNaoColidem() {
        assertThat(RecorteDeAcesso.doTutor(TUTOR_A).chaveDeCache())
                .isNotEqualTo(RecorteDeAcesso.doTutor(TUTOR_B).chaveDeCache());
    }

    @Test
    @DisplayName("clinicas diferentes nao colidem na chave")
    void clinicasNaoColidem() {
        // A dimensao que faltava na chave quando a B1 foi implementada.
        assertThat(RecorteDeAcesso.daClinica(CLINICA_A).chaveDeCache())
                .isNotEqualTo(RecorteDeAcesso.daClinica(CLINICA_B).chaveDeCache());
    }

    @Test
    @DisplayName("tutor, veterinario e admin sao tres chaves distintas")
    void perfisNaoColidem() {
        String tutor = RecorteDeAcesso.doTutor(TUTOR_A).chaveDeCache();
        String clinica = RecorteDeAcesso.daClinica(CLINICA_A).chaveDeCache();
        String admin = RecorteDeAcesso.irrestrito().chaveDeCache();

        assertThat(tutor).isNotEqualTo(clinica).isNotEqualTo(admin);
        assertThat(clinica).isNotEqualTo(admin);
    }

    @Test
    @DisplayName("a chave e estavel: o mesmo recorte devolve a mesma chave")
    void chaveEstavel() {
        // Sem isso o cache nunca acertaria — cada chamada geraria uma entrada.
        assertThat(RecorteDeAcesso.doTutor(TUTOR_A).chaveDeCache())
                .isEqualTo(RecorteDeAcesso.doTutor(TUTOR_A).chaveDeCache());
        assertThat(RecorteDeAcesso.irrestrito().chaveDeCache())
                .isEqualTo(RecorteDeAcesso.irrestrito().chaveDeCache());
    }

    @Test
    @DisplayName("a chave cobre as duas dimensoes, e nao so a que a consulta filtra")
    void chaveCobreAsDuasDimensoes() {
        // AnimalService filtra so por tutor. Se a chave tambem cobrisse so o
        // tutor, dois veterinarios de clinicas distintas -- ambos com tutorId
        // nulo -- cairiam na mesma entrada. Aqui isso e barrado por construcao.
        assertThat(RecorteDeAcesso.daClinica(CLINICA_A).chaveDeCache())
                .contains(CLINICA_A.toString());
        assertThat(RecorteDeAcesso.doTutor(TUTOR_A).chaveDeCache())
                .contains(TUTOR_A.toString());
    }
}
