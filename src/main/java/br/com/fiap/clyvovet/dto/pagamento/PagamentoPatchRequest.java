package br.com.fiap.clyvovet.dto.pagamento;

import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.exception.CampoNaoAceitoException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo do PATCH: so os campos que mudam.
 *
 * Mantem as restricoes de FORMATO e abre mao das de PRESENCA. O raciocinio
 * completo -- por que nao reaproveitar o Request nem usar grupos de validacao,
 * e por que um campo nao pode ser APAGADO via PATCH -- esta em
 * {@link br.com.fiap.clyvovet.dto.tutor.TutorPatchRequest}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PagamentoPatchRequest {

    private FormaPagamento formaPagamento;

    @Positive(message = "Valor deve ser positivo")
    @Digits(integer = 9, fraction = 2, message = "Valor inválido: máximo 9 dígitos inteiros e 2 decimais")
    private BigDecimal valor;

    @PastOrPresent(message = "Data de pagamento não pode ser futura")
    private LocalDate dataPagamento;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;

    private UUID eventoClinicoId;

    // O statusPagamento SAIU daqui (regra P14). Enquanto ele estivesse no
    // corpo do PATCH, um {"statusPagamento":"PAGO"} contornaria as transicoes
    // de uma vez, e as regras P1 a P13 seriam decorativas. As transicoes
    // acontecem em POST /pagamentos/{id}/confirmar e /estornar.

    /**
     * Mas omitir o campo fechava so metade da porta.
     *
     * <p>Sem isto, o campo deixava de ter EFEITO e a requisicao continuava
     * respondendo <b>200</b>. Verificado contra a pilha no ar: PATCH com
     * {@code statusPagamento: "REEMBOLSADO"} devolvia 200 e o registro seguia
     * PENDENTE. Quem integra le o 200, acredita que mudou, e so descobre depois,
     * olhando o extrato.</p>
     *
     * <p>Nao e {@code FAIL_ON_UNKNOWN_PROPERTIES}: aquela chave e global, e liga-la
     * faria toda rota da API recusar qualquer campo extra -- mudanca grande demais
     * para o problema. Aqui a recusa fica no DTO que a exige.</p>
     */
    @JsonAnySetter
    private void recusarCampoDesconhecido(String nome, Object valorIgnorado) {
        throw new CampoNaoAceitoException(nome);
    }
}
