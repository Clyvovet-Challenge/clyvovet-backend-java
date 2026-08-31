package br.com.fiap.clyvovet.dto.eventoClinico;

import br.com.fiap.clyvovet.model.Desfecho;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O veterinario fechando o atendimento.
 *
 * Todos os campos sao opcionais: uma consulta pode terminar sem pesagem, sem
 * desfecho conclusivo e sem retorno programado. O que a chamada faz de
 * obrigatorio e a TRANSICAO — AGENDADO vira REALIZADO —, e essa nao vem do
 * corpo, e a propria operacao.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ConclusaoRequest {

    @Positive(message = "Peso deve ser positivo")
    @Digits(integer = 3, fraction = 3, message = "Peso inválido: máximo 3 dígitos inteiros e 3 decimais")
    private BigDecimal pesoKg;

    private Desfecho desfecho;

    /** Quando o pet deve voltar. E o que alimenta a lista de retornos vencidos. */
    @Future(message = "O retorno previsto precisa ser uma data futura")
    private LocalDate dataRetornoPrevisto;

    @Size(max = 1000)
    private String descricao;
}
