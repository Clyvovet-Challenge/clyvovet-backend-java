package br.com.fiap.clyvovet.dto.pagamento;

import java.math.BigDecimal;
import java.util.UUID;

public record SaldoResponse(
        UUID eventoId,
        String descricao,
        BigDecimal valorCobrado,
        BigDecimal totalPago,
        BigDecimal emAberto,
        boolean quitado
) {}
