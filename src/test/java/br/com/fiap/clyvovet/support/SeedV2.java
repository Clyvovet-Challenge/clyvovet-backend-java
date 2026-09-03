package br.com.fiap.clyvovet.support;

/**
 * Ids fixos da migration V2, usados como ponto de partida pelos testes.
 *
 * Estavam repetidos como literais soltos em cada classe, o que obrigava a
 * decorar qual UUID era de quem. Aqui o nome diz a que registro pertence.
 */
public final class SeedV2 {

    public static final String CLINICA_VETCARE = "11111111-1111-1111-1111-000000000001";
    public static final String CLINICA_PETMED = "11111111-1111-1111-1111-000000000002";

    public static final String TUTOR_LUCAS = "22222222-2222-2222-2222-000000000001";
    public static final String TUTOR_MARIA = "22222222-2222-2222-2222-000000000002";

    public static final String VET_CAMILA = "33333333-3333-3333-3333-000000000001";
    /** Da PETMED, e nao da VETCARE: e com ele que se prova o recorte por clinica. */
    public static final String VET_RAFAEL_DA_PETMED = "33333333-3333-3333-3333-000000000002";

    /** Lucas e dono do Bolinha; Maria, da Mimi e do Rex. */
    public static final String ANIMAL_BOLINHA_DO_LUCAS = "44444444-4444-4444-4444-000000000001";
    public static final String ANIMAL_MIMI_DA_MARIA = "44444444-4444-4444-4444-000000000002";

    /** Nao existe em tabela nenhuma — serve para exercitar o 404. */
    public static final String ID_INEXISTENTE = "00000000-0000-0000-0000-000000000999";

    private SeedV2() {
    }
}
