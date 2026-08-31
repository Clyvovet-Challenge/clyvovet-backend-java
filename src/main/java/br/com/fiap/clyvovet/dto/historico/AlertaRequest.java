package br.com.fiap.clyvovet.dto.historico;

import br.com.fiap.clyvovet.model.TipoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro de alergia, condicao cronica ou medicacao continua.
 *
 * Nao existe campo origem: ela e derivada do perfil de quem registra. Aceita-la
 * do corpo permitiria a um tutor gravar um alerta como se fosse do veterinario
 * — e a origem e justamente o que diz ao proximo profissional o quanto confiar
 * naquela informacao.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AlertaRequest {

    @NotNull
    private TipoAlerta tipo;

    @NotBlank
    @Size(min = 3, max = 500)
    private String descricao;
}
