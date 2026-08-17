package br.com.fiap.clyvovet.dto.exception;

/**
 * Formato unico de erro da API. Record por ser um dado de saida imutavel —
 * ninguem precisa alterar um erro depois de montado.
 */
public record ErroValidacao(String campo, String mensagem) {
}
