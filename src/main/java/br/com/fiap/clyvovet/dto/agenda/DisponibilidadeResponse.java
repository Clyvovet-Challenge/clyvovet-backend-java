package br.com.fiap.clyvovet.dto.agenda;

import br.com.fiap.clyvovet.model.DiaSemana;

import java.time.LocalDate;
import java.util.UUID;

public record DisponibilidadeResponse(
        UUID id,
        UUID veterinarioId,
        String veterinarioNome,
        DiaSemana diaSemana,
        String horaInicio,
        String horaFim,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim
) {}
