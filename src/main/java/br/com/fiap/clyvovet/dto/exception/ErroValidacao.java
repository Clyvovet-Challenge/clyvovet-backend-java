package br.com.fiap.clyvovet.dto.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato unico de erro da API. Record por ser um dado de saida imutavel —
 * ninguem precisa alterar um erro depois de montado.
 *
 * <p>A {@code referencia} so aparece quando ha o que investigar: e o
 * identificador da requisicao no log, e ele acompanha a falha inesperada para
 * que o usuario possa cita-lo ao pedir ajuda. No erro que o usuario resolve
 * sozinho — campo em branco, data no passado — nao ha nada a investigar: o campo
 * fica nulo e o {@code NON_NULL} o omite do JSON. Assim o contrato que o
 * aplicativo ja consome continua byte a byte igual ao de antes.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroValidacao(String campo, String mensagem, String referencia) {

    /** O erro do dia a dia: o usuario le, corrige e segue. */
    public ErroValidacao(String campo, String mensagem) {
        this(campo, mensagem, null);
    }
}
