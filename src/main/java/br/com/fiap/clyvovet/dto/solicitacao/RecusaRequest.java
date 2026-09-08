package br.com.fiap.clyvovet.dto.solicitacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Por que o tutor recusou.
 *
 * <p>Obrigatório de propósito. Uma recusa sem motivo deixa o veterinário sem saber
 * se o dado estava errado, se o tutor não entendeu, ou se foi engano — e o único
 * caminho que sobra é pedir de novo, igual.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RecusaRequest {

    @NotBlank(message = "Motivo da recusa é obrigatório")
    @Size(min = 5, max = 500)
    private String motivo;
}
