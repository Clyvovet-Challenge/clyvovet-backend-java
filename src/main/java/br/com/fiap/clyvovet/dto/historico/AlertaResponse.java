package br.com.fiap.clyvovet.dto.historico;

import br.com.fiap.clyvovet.model.OrigemAlerta;
import br.com.fiap.clyvovet.model.TipoAlerta;

import java.time.LocalDate;
import java.util.UUID;

/** A origem vem junto porque "o tutor disse" e "o veterinario registrou" pesam diferente. */
public record AlertaResponse(
        UUID id,
        TipoAlerta tipo,
        String descricao,
        OrigemAlerta origem,
        LocalDate registradoEm
) {}
