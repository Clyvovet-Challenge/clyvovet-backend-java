package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Um furo na grade do veterinario: ferias, folga, congresso, almoco.
 *
 * Uma estrutura so cobre os dois formatos. Quando hora_inicio e hora_fim sao
 * nulos, o bloqueio vale para os dias inteiros entre data_inicio e data_fim —
 * e o caso das ferias. Quando vem preenchidos, vale so naquela faixa de cada
 * dia do intervalo — e o caso do almoco.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    /** Nulo, junto com horaFim, significa o dia inteiro. */
    @Column(name = "hora_inicio")
    private String horaInicio;

    @Column(name = "hora_fim")
    private String horaFim;

    @Column(nullable = false)
    private String motivo;

    public boolean diaInteiro() {
        return horaInicio == null;
    }

    public boolean alcanca(LocalDate data) {
        return !data.isBefore(dataInicio) && !data.isAfter(dataFim);
    }
}
