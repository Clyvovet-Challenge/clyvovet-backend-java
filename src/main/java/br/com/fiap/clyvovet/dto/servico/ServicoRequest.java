package br.com.fiap.clyvovet.dto.servico;

import br.com.fiap.clyvovet.model.TipoEvento;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ServicoRequest {

    @NotNull
    private UUID clinicaId;

    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;

    @NotNull
    private TipoEvento tipoEvento;

    @NotNull
    @PositiveOrZero(message = "Preço não pode ser negativo")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal preco;

    /**
     * A faixa espelha o check da migration. Duracao zero produziria colisao de
     * agenda impossivel de resolver — dois atendimentos ocupando o mesmo
     * instante sem se sobrepor.
     */
    @NotNull
    @Min(value = 5, message = "Duração mínima é de 5 minutos")
    @Max(value = 480, message = "Duração máxima é de 8 horas")
    private Integer duracaoMinutos;
}
