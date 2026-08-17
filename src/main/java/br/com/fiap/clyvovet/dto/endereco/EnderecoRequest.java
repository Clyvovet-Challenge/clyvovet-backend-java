package br.com.fiap.clyvovet.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class EnderecoRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String logradouro;
    @NotBlank
    @Size(max = 10)   // coluna VARCHAR2(10)
    private String numero;
    @NotBlank
    @Size(min = 3, max = 100)
    private String bairro;
    @NotBlank
    @Size(min = 3, max = 100)
    private String cidade;
    @NotBlank
    @Size(min = 2, max = 2)
    private String estado;
    @NotBlank
    @Size(min = 8, max = 8)
    private String cep;
    private String complemento;
}
