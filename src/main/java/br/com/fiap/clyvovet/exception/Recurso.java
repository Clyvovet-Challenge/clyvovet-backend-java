package br.com.fiap.clyvovet.exception;

import java.util.UUID;

/**
 * Recursos de dominio que podem ser buscados por id.
 *
 * Existe para tirar a mensagem de "nao encontrado" de dentro dos services: ela
 * estava repetida em cerca de vinte pontos, cada um concatenando o proprio
 * texto. Alem da duplicacao, a concordancia variava ("nao encontrado" x "nao
 * encontrada") e nada garantia que continuasse coerente. Aqui cada recurso
 * declara a propria frase uma unica vez.
 */
public enum Recurso {

    ALERTA_CLINICO("Alerta clínico não encontrado"),
    ANIMAL("Animal não encontrado"),
    BLOQUEIO("Bloqueio não encontrado"),
    CLINICA("Clínica não encontrada"),
    EVENTO_CLINICO("Evento clínico não encontrado"),
    PAGAMENTO("Pagamento não encontrado"),
    DISPONIBILIDADE("Disponibilidade não encontrada"),
    SERVICO("Serviço não encontrado"),
    TUTOR("Tutor não encontrado"),
    USUARIO("Usuário não encontrado"),
    VETERINARIO("Veterinário não encontrado");

    private final String ausencia;

    Recurso(String ausencia) {
        this.ausencia = ausencia;
    }

    public String mensagemDeAusencia(UUID id) {
        return ausencia + " com ID: " + id;
    }
}
