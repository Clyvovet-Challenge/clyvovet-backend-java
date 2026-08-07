package br.com.fiap.clyvovet.exception;

/**
 * Violacao de regra de negocio — mapeada para 409 pelo GlobalExceptionHandler.
 * Distinta de EntityNotFoundException (404) e de falha de Bean Validation (400).
 */
public class RegraDeNegocioException extends RuntimeException {

    private final String campo;

    public RegraDeNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
