package br.com.fiap.clyvovet.exception;

/**
 * Teto de consultas ao historico clinico ultrapassado — mapeada para 429.
 *
 * DISTINTA DO RATE LIMIT POR IP, e a distincao importa. Aquele protege a
 * infraestrutura contra rajada e conta requisicoes; este protege os pacientes
 * contra coleta e conta ANIMAIS DISTINTOS por profissional por dia. Um
 * veterinario que abre o mesmo prontuario quarenta vezes durante uma cirurgia
 * nao chega perto do teto; um que consulta duzentos animais diferentes numa
 * tarde chega, e nao esta atendendo nenhum deles.
 */
public class LimiteDeAcessoExcedidoException extends RuntimeException {

    private final String campo;

    public LimiteDeAcessoExcedidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
