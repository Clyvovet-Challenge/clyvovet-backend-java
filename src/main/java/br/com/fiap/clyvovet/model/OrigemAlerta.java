package br.com.fiap.clyvovet.model;

/**
 * Quem registrou o alerta.
 *
 * Nao e metadado decorativo: "o tutor disse que tem alergia a dipirona" e "o
 * veterinario registrou anafilaxia a dipirona" pesam diferente na decisao
 * clinica. Quem le o resumo de seguranca precisa dessa distincao para saber o
 * quanto confiar no que esta lendo.
 */
public enum OrigemAlerta {
    TUTOR,
    VETERINARIO
}
