package br.com.fiap.clyvovet.dto.historico;

import java.time.LocalDate;

/**
 * Uma linha da auditoria, como o tutor a ve.
 *
 * Uma por (veterinario, animal, dia), com o contador de vezes. E o que torna a
 * lista legivel para quem ela existe: "a Dra. Camila leu em 12/09, 3 vezes" diz
 * o que importa; trinta linhas identicas nao dizem.
 */
public record AcessoResponse(
        LocalDate dia,
        String usuarioEmail,
        String clinicaNome,
        Integer nivel,
        Integer vezes,
        boolean emergencial,
        String motivo
) {}
