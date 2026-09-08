package br.com.fiap.clyvovet.dto.solicitacao;

import br.com.fiap.clyvovet.model.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * O que o veterinário propõe mudar no cadastro do animal.
 *
 * <p>Mesma forma do {@code AnimalPatchRequest}, e pelo mesmo motivo: só os campos
 * enviados entram no pedido, e nenhum campo pode ser APAGADO por aqui — ausente
 * significa "não faz parte do pedido", nunca "limpe este valor".</p>
 *
 * <p>A diferença é a {@link #justificativa}, que é obrigatória. Ela é o que o
 * tutor lê antes de decidir; sem ela o pedido vira um "deixa eu mexer" sem
 * contexto, que é exatamente o que este fluxo existe para evitar.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SolicitacaoAlteracaoRequest {

    @NotBlank(message = "Justificativa é obrigatória")
    @Size(min = 10, max = 500, message = "Justificativa deve ter entre 10 e 500 caracteres")
    private String justificativa;

    @Size(min = 3, max = 100)
    private String nome;

    @Size(min = 3, max = 100)
    private String raca;

    @Size(min = 3, max = 100)
    private String especie;

    @Size(min = 3, max = 100)
    private String porte;

    @Size(min = 3, max = 100)
    private String cor;

    private SexoAnimal sexo;

    private LocalDate dataNascimento;

    @Pattern(regexp = "\\d{15}", message = "Microchip deve ter 15 dígitos")
    private String microchip;

    private Boolean castrado;

    @Size(max = 1000)
    private String observacao;
}
