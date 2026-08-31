package br.com.fiap.clyvovet.dto.eventoClinico;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Marcacao do retorno. O animal, a clinica e o servico vem da consulta de
 * origem — nao do corpo.
 *
 * Aceitar o animalId aqui permitiria marcar um retorno de um pet ligado a
 * consulta de outro, o que quebraria R10 sem que nada no banco reclamasse: a
 * FK evento_origem_id nao sabe nada sobre qual animal e qual.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RetornoRequest {

    @NotNull
    @Future(message = "O retorno precisa ser marcado para uma data futura")
    private LocalDate data;

    @NotNull
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Hora deve estar no formato HH:mm")
    private String hora;

    /** Opcional: sem ele, o retorno fica com o mesmo veterinario da consulta. */
    private UUID veterinarioId;
}
