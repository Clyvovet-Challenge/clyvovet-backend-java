package br.com.fiap.clyvovet.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Dia da semana da grade de disponibilidade.
 *
 * Existe em vez de java.time.DayOfWeek por dois motivos. O dominio inteiro
 * deste projeto fala portugues (Perfil, TipoEvento, StatusPagamento), e uma
 * linha gravada como 'MONDAY' no meio de 'SEGUNDA' seria a unica em ingles.
 * E gravar o ordinal, que e o padrao do JPA para enums, tornaria a tabela
 * ilegivel em consulta manual — quem abre a grade no cliente SQL nao deveria
 * precisar lembrar se domingo e 1 ou 7.
 *
 * A conversao de e para DayOfWeek fica aqui, num lugar so.
 */
public enum DiaSemana {
    SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO;

    public static DiaSemana de(DayOfWeek dia) {
        return values()[dia.getValue() - 1];   // DayOfWeek: MONDAY = 1 .. SUNDAY = 7
    }

    public static DiaSemana de(LocalDate data) {
        return de(data.getDayOfWeek());
    }
}
