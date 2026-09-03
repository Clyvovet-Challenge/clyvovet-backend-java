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
 *
 * E por isso que as fabricas nao aceitam nulo no lugar do proprio vinculo. Um
 * TUTOR sem tutor vinculado, ou um VETERINARIO sem veterinario, produzia
 * {@code (null, null)} — que e exatamente o recorte do ADMIN. O usuario nao
 * ficava sem ver nada; ficava vendo TUDO.
 */
public record RecorteDeAcesso(UUID tutorId, UUID clinicaId) {

    private static final RecorteDeAcesso IRRESTRITO = new RecorteDeAcesso(null, null);

    /**
     * O id que nao pertence a ninguem, usado quando o vinculo esperado falta.
     *
     * Precisa ser um UUID, e nao nulo, porque a consulta le nulo como "sem
     * recorte" — {@code (:clinicaId IS NULL OR ...)}. Com um valor que nenhuma
     * linha carrega, a mesma clausula devolve pagina vazia, e o erro de
     * cadastro para de valer como promocao a ADMIN.
     *
     * Nao e a unica saida possivel: dava para lancar 403 aqui. Nao lanca porque
     * este metodo tambem e chamado de dentro da chave de cache, em SpEL, onde
     * uma excecao vira falha de avaliacao da expressao — 500 no lugar de 403,
     * e a rota inteira quebrada em vez de vazia.
     */
    private static final UUID NENHUM = new UUID(0L, 0L);

    /** ADMIN: enxerga a base inteira. */
    public static RecorteDeAcesso irrestrito() {
        return IRRESTRITO;
    }

    public static RecorteDeAcesso doTutor(UUID tutorId) {
        return new RecorteDeAcesso(ouNenhum(tutorId), null);
    }

    public static RecorteDeAcesso daClinica(UUID clinicaId) {
        return new RecorteDeAcesso(null, ouNenhum(clinicaId));
    }

    /** Falta de vinculo vira recorte que nao alcanca nada — nunca "sem recorte". */
    private static UUID ouNenhum(UUID id) {
        return id != null ? id : NENHUM;
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
