package br.com.fiap.clyvovet.dto.pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExtratoResponse(
        UUID tutorId,
        LocalDate de,
        LocalDate ate,
        BigDecimal totalPago,
        BigDecimal totalPendente,
        BigDecimal totalEstornado,
        List<PagamentoResponse> pagamentos
) {}
