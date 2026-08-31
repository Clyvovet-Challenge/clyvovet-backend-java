package br.com.fiap.clyvovet.dto.eventoClinico;

/**
 * O atendimento concluído, mais o aviso clínico quando há um.
 *
 * O aviso vem na resposta, e não só no log: alerta que o veterinário não vê
 * não é alerta. Nulo quando não há nada a apontar — é o caso normal.
 */
public record ConclusaoResponse(EventoClinicoResponse evento, String aviso) {}
