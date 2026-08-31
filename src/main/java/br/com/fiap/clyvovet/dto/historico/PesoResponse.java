package br.com.fiap.clyvovet.dto.historico;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um ponto da serie de peso. A tendencia diz mais que qualquer aferição isolada. */
public record PesoResponse(LocalDate data, BigDecimal pesoKg) {}
