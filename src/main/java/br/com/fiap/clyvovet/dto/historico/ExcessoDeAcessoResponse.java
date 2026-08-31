package br.com.fiap.clyvovet.dto.historico;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da revisao de tetos: um profissional, um dia, uma contagem.
 *
 * Por DIA, e nao acumulado no periodo. Trinta animais num dia e um plantao
 * cheio; trinta por dia durante um mes e outra coisa — e so a serie diaria
 * deixa as duas distinguiveis. Um total unico esconderia a diferenca.
 */
public record ExcessoDeAcessoResponse(
        UUID usuarioId,
        String usuarioEmail,
        LocalDate dia,
        Long quantidade,
        boolean emergencial
) {}
