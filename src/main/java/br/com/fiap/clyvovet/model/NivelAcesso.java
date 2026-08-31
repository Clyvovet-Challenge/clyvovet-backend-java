package br.com.fiap.clyvovet.model;

/**
 * Quanto do historico clinico o solicitante alcanca.
 *
 * Cada nivel tem base legal propria, e e isso que faz o desenho se sustentar:
 * OPERACIONAL pela execucao do atendimento, RESUMO_DE_SEGURANCA pela protecao
 * da vida do animal, COMPLETO por consentimento informado do tutor.
 *
 * O microchip identifica o animal em qualquer um deles; ele nunca decide o
 * nivel. Quem credencia o nivel 1 e a autenticacao do veterinario, e o nivel 2,
 * a autorizacao do tutor.
 */
public enum NivelAcesso {
    /** Nome, especie, raca, porte, idade. O minimo para atender. */
    OPERACIONAL(0),
    /** Alergias, condicoes cronicas, medicacao continua, vacinas, peso. */
    RESUMO_DE_SEGURANCA(1),
    /** Linha do tempo, documentos, laudos, desfechos, dados do tutor. */
    COMPLETO(2);

    private final int codigo;

    NivelAcesso(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public boolean alcanca(NivelAcesso exigido) {
        return this.codigo >= exigido.codigo;
    }
}
