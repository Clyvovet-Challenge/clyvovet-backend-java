package br.com.fiap.clyvovet.dto.tutor;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import br.com.fiap.clyvovet.model.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TutorRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
    @NotBlank
    @Size(min = 11, max = 11)
    private String cpf;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Size(min = 10, max = 11)
    private String telefone;
    @NotNull
    private Sexo sexo;
    @NotNull
    @PastOrPresent(message = "Data de nascimento não pode ser futura")
    private LocalDate dataNascimento;
    @Valid
    @NotNull
    private EnderecoRequest endereco;
}
