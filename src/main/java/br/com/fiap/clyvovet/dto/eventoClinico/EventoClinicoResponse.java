package br.com.fiap.clyvovet.dto.eventoClinico;

import br.com.fiap.clyvovet.model.TipoEvento;

import br.com.fiap.clyvovet.model.Desfecho;
import br.com.fiap.clyvovet.model.StatusEvento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EventoClinicoResponse(
        UUID id,
        LocalDate data,
        String hora,
        String descricao,
        TipoEvento tipoEvento,
        UUID veterinarioId,
        String veterinarioNome,
        UUID animalId,
        String animalNome,
        UUID clinicaId,
        String clinicaNome,
        StatusEvento statusEvento,
        LocalDate dataRetornoPrevisto,
        UUID eventoOrigemId,
        BigDecimal pesoKg,
        UUID servicoId,
        String servicoNome,
        BigDecimal valor,
        Desfecho desfecho,
        String motivoCancelamento
) {}