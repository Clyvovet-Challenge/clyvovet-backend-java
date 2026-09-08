package br.com.fiap.clyvovet.dto.solicitacao;

import br.com.fiap.clyvovet.model.StatusSolicitacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O pedido como o tutor e o veterinário o veem.
 *
 * <p>{@link #alteracoes} carrega o antes e o depois de cada campo — é o que
 * permite ao app renderizar a decisão sem uma segunda chamada para buscar o
 * animal.</p>
 */
public record SolicitacaoAlteracaoResponse(
        UUID id,
        UUID animalId,
        String animalNome,
        UUID veterinarioId,
        String veterinarioNome,
        String clinicaNome,
        StatusSolicitacao status,
        String justificativa,
        List<CampoAlterado> alteracoes,
        LocalDateTime criadoEm,
        LocalDateTime respondidoEm,
        String motivoRecusa) {
}
