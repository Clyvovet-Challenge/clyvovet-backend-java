package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    /**
     * Numero do microchip, padrao ISO 11784/11785.
     *
     * Opcional: animal sem chip e comum, e o indice unico do banco ignora
     * nulos, entao varios convivem. Dois com o MESMO chip, nao -- seriam dois
     * animais com a mesma identidade no balcao, e o resumo de seguranca do
     * errado.
     */
    @Pattern(regexp = "\\d{15}", message = "Microchip deve ter 15 dígitos")
    private String microchip;

    /** Compoe o resumo de seguranca. Nulo = nao informado, que nao e o mesmo que nao. */
    private Boolean castrado;
}