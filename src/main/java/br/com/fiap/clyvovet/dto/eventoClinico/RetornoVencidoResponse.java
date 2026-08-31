package br.com.fiap.clyvovet.dto.eventoClinico;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da lista de pets que deviam ter voltado e nao voltaram.
 *
 * Traz o telefone do tutor de proposito: esta lista existe para virar ligacao.
 * Sem o contato, quem a recebe precisaria de uma segunda consulta por linha
 * antes de conseguir agir sobre ela.
 */
public record RetornoVencidoResponse(
        UUID eventoId,
        UUID animalId,
        String animalNome,
        String tutorNome,
        String tutorTelefone,
        LocalDate dataDoAtendimento,
        LocalDate retornoPrevisto,
        int diasEmAtraso,
        String veterinarioNome
) {}
