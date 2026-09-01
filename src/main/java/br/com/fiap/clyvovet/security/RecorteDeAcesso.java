package br.com.fiap.clyvovet.security;

import java.util.UUID;

/**
 * O que este usuario enxerga nas listagens, resolvido uma vez.
 *
 * Antes isto nao era um objeto, e sim tres coisas escritas a mao em cada
 * recurso: uma string SpEL na chave do cache, um par de argumentos na chamada
 * do repositorio e um par de clausulas {@code IS NULL OR} na consulta. Manter as
 * tres em acordo era responsabilidade de quem lembrasse — e acrescentar o
 * recorte de clinica ao extrato e a inadimplencia exigiu editar as tres, uma a
 * uma.
 *
 * Nulo significa "sem recorte nesta dimensao": o TUTOR nao tem clinica, o
 * VETERINARIO nao tem tutor, e o ADMIN nao tem nenhum dos dois.
 */
public record RecorteDeAcesso(UUID tutorId, UUID clinicaId) {

    private static final RecorteDeAcesso IRRESTRITO = new RecorteDeAcesso(null, null);

    /** ADMIN: enxerga a base inteira. */
    public static RecorteDeAcesso irrestrito() {
        return IRRESTRITO;
    }

    public static RecorteDeAcesso doTutor(UUID tutorId) {
        return new RecorteDeAcesso(tutorId, null);
    }

    public static RecorteDeAcesso daClinica(UUID clinicaId) {
        return new RecorteDeAcesso(null, clinicaId);
    }

    /**
     * O pedaco de chave de cache que representa este recorte.
     *
     * Inclui SEMPRE as duas dimensoes, mesmo onde a consulta filtra por apenas
     * uma — o caso de {@code AnimalService}, em que o cadastro do animal e
     * nivel 0 e todo veterinario alcanca.
     *
     * A assimetria e o motivo: chave mais larga que o filtro so custa entradas
     * a mais no cache; chave mais estreita que o filtro ENTREGA a pagina de um
     * usuario a outro. Foi assim que a ruptura B1 quase vazou — o recorte
     * existia na consulta e faltava na chave. Com a chave saindo daqui, ela nao
     * tem como ficar mais estreita que o recorte.
     */
    public String chaveDeCache() {
        return tutorId + "-" + clinicaId;
    }
}
