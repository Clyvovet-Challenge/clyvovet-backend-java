package br.com.fiap.clyvovet.dto.historico;

import br.com.fiap.clyvovet.model.Desfecho;
import br.com.fiap.clyvovet.model.StatusEvento;
import br.com.fiap.clyvovet.model.TipoEvento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um atendimento na linha do tempo.
 *
 * desta Clinica e o que da sentido ao nivel 2: sem consentimento o veterinario
 * ve so os atendimentos da propria casa; com ele, ve a linha inteira e passa a
 * distinguir o que e dele do que veio de fora.
 */
public record LinhaDoTempoResponse(
        UUID eventoId,
        LocalDate data,
        TipoEvento tipoEvento,
        StatusEvento statusEvento,
        String descricao,
        BigDecimal pesoKg,
        Desfecho desfecho,
        String clinicaNome,
        boolean destaClinica
) {}
