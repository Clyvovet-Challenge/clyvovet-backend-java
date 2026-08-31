package br.com.fiap.clyvovet.dto.pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Traz o contato pelo mesmo motivo da lista de retornos vencidos: vira ligação. */
public record InadimplenciaResponse(
        UUID eventoId,
        LocalDate dataDoAtendimento,
        String animalNome,
        String tutorNome,
        String tutorTelefone,
        BigDecimal valorCobrado,
        BigDecimal totalPago,
        BigDecimal emAberto,
        int diasEmAberto
) {}
