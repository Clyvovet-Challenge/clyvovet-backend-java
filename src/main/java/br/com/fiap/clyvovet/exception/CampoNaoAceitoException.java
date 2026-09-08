package br.com.fiap.clyvovet.exception;

import lombok.Getter;

/**
 * O corpo trouxe um campo que este DTO não aceita.
 *
 * <p>Existe porque omitir um campo do DTO fecha só metade da porta: o campo deixa
 * de ter efeito, e a requisição continua respondendo <b>200</b>. Foi o que
 * aconteceu com {@code statusPagamento} no PATCH de pagamento — ele saiu do DTO
 * para fechar a regra P14, e um {@code PATCH {"statusPagamento":"REEMBOLSADO"}}
 * seguia devolvendo 200 com o registro intacto. Verificado contra a pilha no ar.</p>
 *
 * <p>Resposta que mente é pior que erro: quem integra lê o 200, acredita que
 * mudou, e só descobre depois.</p>
 *
 * <p>É lançada de dentro de um {@code @JsonAnySetter}, e não por
 * {@code FAIL_ON_UNKNOWN_PROPERTIES}: aquela é uma chave global, e ligá-la faria
 * <b>toda</b> rota da API passar a recusar qualquer campo extra — mudança grande
 * demais para o problema em questão. Assim a recusa fica onde precisa estar, no
 * DTO que a exige.</p>
 */
@Getter
public class CampoNaoAceitoException extends RuntimeException {

    private final String campo;

    public CampoNaoAceitoException(String campo) {
        super("Campo não aceito neste corpo: '" + campo + "'");
        this.campo = campo;
    }
}
