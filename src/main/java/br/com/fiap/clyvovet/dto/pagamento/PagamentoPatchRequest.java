package br.com.fiap.clyvovet.dto.pagamento;

import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;
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
}
