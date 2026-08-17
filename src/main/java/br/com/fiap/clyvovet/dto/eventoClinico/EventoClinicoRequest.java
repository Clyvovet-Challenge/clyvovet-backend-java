package br.com.fiap.clyvovet.dto.eventoClinico;

import br.com.fiap.clyvovet.model.TipoEvento;
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
public class EventoClinicoRequest {

    @NotNull
    private LocalDate data;

    /**
     * A coluna e VARCHAR2(5), ou seja, so cabe HH:mm. Antes o campo tinha
     * apenas @NotNull: "" era aceito e "14:30:00" chegava ao banco para
     * estourar la, como erro de servidor. O @Pattern recusa os dois na
     * validacao, que e onde o cliente consegue entender o que errou.
     */
    @NotBlank
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String hora;

    @Size(max = 1000)   // coluna VARCHAR2(1000)
    private String descricao;

    @NotNull
    private UUID veterinarioId;

    @NotNull
    private UUID animalId;

    @NotNull
    private UUID clinicaId;

    @NotNull
    private TipoEvento tipoEvento;
}
