package br.com.fiap.clyvovet.dto.agenda;

import br.com.fiap.clyvovet.model.DiaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Uma faixa recorrente da grade: "toda terca, das 08:00 as 12:00". */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DisponibilidadeRequest {

    @NotNull
    private UUID veterinarioId;

    @NotNull
    private DiaSemana diaSemana;

    @NotNull
    @Pattern(regexp = HORA, message = "Hora deve estar no formato HH:mm")
    private String horaInicio;

    @NotNull
    @Pattern(regexp = HORA, message = "Hora deve estar no formato HH:mm")
    private String horaFim;

    @NotNull
    private LocalDate vigenciaInicio;

    /** Nulo = vigente por tempo indeterminado. */
    private LocalDate vigenciaFim;

    /**
     * O formato de largura fixa nao e cosmetico: e ele que faz a comparacao de
     * horario funcionar no banco. O check chk_disp_horas compara hora_fim com
     * hora_inicio como TEXTO, e '9:00' > '18:00' lexicograficamente. Sem este
     * @Pattern, o zero a esquerda faltante passaria pela API e desligaria a
     * protecao do banco em silencio.
     */
    private static final String HORA = "([01]\\d|2[0-3]):[0-5]\\d";
}
