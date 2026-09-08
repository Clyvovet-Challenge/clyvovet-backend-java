package br.com.fiap.clyvovet.dto.solicitacao;

/**
 * Um campo do pedido, com o valor de agora e o proposto.
 *
 * <p>Existe para a tela do tutor poder mostrar "raça: Poodle → Poodle Toy" em vez
 * de só o valor novo. Decidir sem ver o que sai é decidir pela metade.</p>
 */
public record CampoAlterado(String campo, String atual, String proposto) {
}
