package br.com.fiap.clyvovet.dto.clinica;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ClinicaRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
    @NotBlank
    @Size(min = 14, max = 14)
    private String cnpj;
    @NotBlank
    @Size(min = 10, max = 11)
    private String telefone;
    @NotBlank
    @Email
    @Size(min = 10, max = 100)
    private String email;
    @Valid
    @NotNull
    private EnderecoRequest endereco;
    /**
     * Coordenada, opcional.
     *
     * Sem ela, clinica criada pela API depois da V13 nunca ganharia coordenada
     * e ficaria invisivel no mapa para sempre -- o tipo de ausencia silenciosa
     * que este projeto ja pagou caro.
     *
     * A faixa e validada porque latitude e longitude trocadas de lugar e o erro
     * mais comum aqui: com -46 no campo da latitude o pino sai do Brasil sem
     * nenhum erro, e a API recusa em vez de plantar a clinica no oceano.
     */
    @DecimalMin(value = "-90.0",  message = "latitude fora da faixa (-90 a 90)")
    @DecimalMax(value = "90.0",   message = "latitude fora da faixa (-90 a 90)")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude fora da faixa (-180 a 180)")
    @DecimalMax(value = "180.0",  message = "longitude fora da faixa (-180 a 180)")
    private BigDecimal longitude;
}
