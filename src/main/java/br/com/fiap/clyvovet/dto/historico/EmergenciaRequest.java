package br.com.fiap.clyvovet.dto.historico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Quebra de vidro.
 *
 * O motivo minimo de 10 caracteres nao e burocracia: e o unico custo que o
 * acesso sem consentimento impoe a quem o aciona. Um campo que aceitasse "x"
 * transformaria a excecao no caminho mais curto, e o consentimento viraria
 * enfeite.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class EmergenciaRequest {

    @NotBlank
    @Size(min = 10, max = 500, message = "Descreva a emergência em pelo menos 10 caracteres")
    private String motivo;
}
