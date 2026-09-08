package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.model.SolicitacaoAlteracao;
import br.com.fiap.clyvovet.model.StatusSolicitacao;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoAlteracaoRepository extends RepositorioBase<SolicitacaoAlteracao> {

    /**
     * A caixa de pedidos do tutor: tudo que esta esperando resposta dele, em
     * qualquer um dos seus animais.
     *
     * O filtro por dono vive na consulta, e nao no @PreAuthorize, porque aqui nao
     * ha um id na URL para autorizar -- a lista JA e o recorte.
     */
    List<SolicitacaoAlteracao> findByAnimalTutorIdAndStatusOrderByCriadoEmDesc(
            UUID tutorId, StatusSolicitacao status);

    /** Todo o historico de pedidos dos animais deste tutor, respondidos ou nao. */
    List<SolicitacaoAlteracao> findByAnimalTutorIdOrderByCriadoEmDesc(UUID tutorId);

    /** O historico de um animal especifico -- quem pediu o que, e como terminou. */
    List<SolicitacaoAlteracao> findByAnimalIdOrderByCriadoEmDesc(UUID animalId);

    /** O outro lado: o que este veterinario pediu e ainda nao foi respondido. */
    List<SolicitacaoAlteracao> findByVeterinarioIdAndStatusOrderByCriadoEmDesc(
            UUID veterinarioId, StatusSolicitacao status);

    /**
     * Impede o mesmo veterinario de empilhar pedidos no mesmo animal.
     *
     * Sem isto, um profissional que clicasse duas vezes encheria a caixa do tutor
     * com pedidos identicos, e o tutor teria que responder um por um.
     */
    boolean existsByAnimalIdAndVeterinarioIdAndStatus(
            UUID animalId, UUID veterinarioId, StatusSolicitacao status);
}
