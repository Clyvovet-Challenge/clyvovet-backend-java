package br.com.fiap.clyvovet.dto.agenda;

import java.time.LocalDate;
import java.util.UUID;

public record BloqueioResponse(
        UUID id,
        UUID veterinarioId,
        String veterinarioNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        String horaInicio,
        String horaFim,
        String motivo
) {}
