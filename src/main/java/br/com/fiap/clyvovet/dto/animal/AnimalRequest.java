package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AnimalRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
    @NotBlank
    @Size(min = 3, max = 100)
    private String raca;
    @NotBlank
    @Size(min = 3, max = 100)
    private String especie;
    @NotBlank
    @Size(min = 3, max = 100)
    private String porte;
    @NotBlank
    @Size(min = 3, max = 100)
    private String cor;
    @NotNull
    private SexoAnimal sexo;
    @NotNull
    private LocalDate dataNascimento;
    // Limite igual ao da coluna, VARCHAR2(1000): sem ele um texto maior passa
    // pela validacao e so falha no INSERT, virando erro de servidor.
    @Size(max = 1000)
    private String observacao;
    @NotNull
    private UUID tutorId;
}