package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma faixa de horario em que o veterinario atende, num dia da semana.
 *
 * A grade e semanal e recorrente: "toda terca, das 08:00 as 12:00". O furo
 * pontual — ferias, folga, almoco — e {@link Bloqueio}, e nao uma excecao
 * dentro desta linha. Separar os dois evita a alternativa, que seria recriar a
 * grade inteira toda vez que alguem tira um dia.
 *
 * A vigencia existe para que a grade possa mudar sem apagar o passado: quando
 * o veterinario troca de horario, a linha antiga ganha vigencia_fim e uma nova
 * comeca. Sem isso, remarcar a grade invalidaria retroativamente agendamentos
 * que eram legitimos quando foram feitos.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "disponibilidade_veterinario")
public class DisponibilidadeVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    /** Formato "HH:mm", o mesmo de EventoClinico.hora. */
    @Column(name = "hora_inicio", nullable = false)
    private String horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private String horaFim;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    /** Nulo = vigente por tempo indeterminado. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(vigenciaInicio)
                && (vigenciaFim == null || !data.isAfter(vigenciaFim));
    }
}
