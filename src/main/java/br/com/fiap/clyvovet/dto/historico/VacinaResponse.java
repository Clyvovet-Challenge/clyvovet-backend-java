package br.com.fiap.clyvovet.dto.historico;

import java.time.LocalDate;

/** Derivada dos eventos de tipo VACINA, nunca digitada a parte. */
public record VacinaResponse(LocalDate data, String descricao) {}
