package br.com.fiap.clyvovet.dto.agendamento;

import java.time.LocalDate;
import java.util.UUID;

/** Uma vaga livre na agenda, pronta para virar um POST /agendamentos. */
public record VagaResponse(
        LocalDate data,
        String horaInicio,
        String horaFim,
        UUID veterinarioId,
        String veterinarioNome
) {}
