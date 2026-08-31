package br.com.fiap.clyvovet.model;

/**
 * Natureza de um alerta clinico — o conteudo do nivel 1 do fluxo C.
 *
 * Sao as quatro coisas que um veterinario precisa saber antes de medicar um
 * animal que ele nunca viu, e que por isso ficam acessiveis pelo microchip sem
 * consentimento previo. CRITICO e a saida para o que nao cabe nas outras tres
 * e ainda assim nao pode passar despercebido — cardiopatia, hemofilia,
 * temperamento agressivo sob dor.
 */
public enum TipoAlerta {
    ALERGIA,
    CONDICAO_CRONICA,
    MEDICACAO_CONTINUA,
    CRITICO
}
