package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.solicitacao.RecusaRequest;
import br.com.fiap.clyvovet.dto.solicitacao.SolicitacaoAlteracaoRequest;
import br.com.fiap.clyvovet.dto.solicitacao.SolicitacaoAlteracaoResponse;
import br.com.fiap.clyvovet.service.SolicitacaoAlteracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * O veterinário pede para alterar o cadastro do animal; o tutor aprova ou recusa.
 *
 * <p>Existe porque a escrita direta foi fechada: {@code PUT}, {@code PATCH} e
 * {@code DELETE} de animal exigem ser o dono ou o ADMIN da plataforma. Antes disso,
 * qualquer veterinário autenticado alterava e excluía o pet de qualquer tutor —
 * verificado contra a pilha local, com zero autorizações concedidas.</p>
 *
 * <p>A regra do produto sempre foi que ele pode alterar <b>com autorização do
 * dono</b>. Estas rotas são essa autorização.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Solicitações de alteração",
     description = "Pedidos do veterinário para alterar o cadastro de um animal")
public class SolicitacaoAlteracaoController {

    private final SolicitacaoAlteracaoService service;

    // ================================================================
    // O veterinário pede
    // ================================================================

    /**
     * O {@code podeAcessarAnimal} aqui é de LEITURA, e é o certo: o veterinário
     * precisa enxergar o animal para propor uma mudança nele. Quem decide se a
     * mudança acontece é o tutor, no aprovar.
     */
    @PostMapping("/animais/{animalId}/solicitacoes-alteracao")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#animalId)")
    @Operation(summary = "Solicitar alteração do cadastro de um animal")
    public ResponseEntity<SolicitacaoAlteracaoResponse> solicitar(
            @PathVariable UUID animalId,
            @Valid @RequestBody SolicitacaoAlteracaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.solicitar(animalId, request));
    }

    /** O que o veterinário logado pediu e ainda não foi respondido. */
    @GetMapping("/solicitacoes-alteracao/meus-pedidos")
    @Operation(summary = "Pedidos que eu fiz e aguardam resposta")
    public ResponseEntity<List<SolicitacaoAlteracaoResponse>> meusPedidos() {
        return ResponseEntity.ok(service.meusPedidosPendentes());
    }

    // ================================================================
    // O tutor responde
    // ================================================================

    /**
     * A caixa de pedidos do tutor.
     *
     * <p>Sem {@code @PreAuthorize} de propósito: não há id na URL para autorizar.
     * O recorte é a própria consulta, que filtra pelo tutor do usuário logado —
     * como em {@code /agendamentos/meus} e {@code /autorizacoes/minhas}.</p>
     */
    @GetMapping("/solicitacoes-alteracao/minhas")
    @Operation(summary = "Pedidos aguardando a minha resposta")
    public ResponseEntity<List<SolicitacaoAlteracaoResponse>> minhas() {
        return ResponseEntity.ok(service.pendentesDoTutorLogado());
    }

    @GetMapping("/solicitacoes-alteracao/historico")
    @Operation(summary = "Todos os pedidos feitos nos meus animais, respondidos ou não")
    public ResponseEntity<List<SolicitacaoAlteracaoResponse>> historico() {
        return ResponseEntity.ok(service.historicoDoTutorLogado());
    }

    @PostMapping("/solicitacoes-alteracao/{id}/aprovar")
    @PreAuthorize("@seguranca.ehDonoOuAdministrador(@solicitacaoAlteracaoService.doAnimalDe(#id))")
    @Operation(summary = "Aprovar o pedido — os valores são gravados no animal")
    public ResponseEntity<SolicitacaoAlteracaoResponse> aprovar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.aprovar(id));
    }

    @PostMapping("/solicitacoes-alteracao/{id}/recusar")
    @PreAuthorize("@seguranca.ehDonoOuAdministrador(@solicitacaoAlteracaoService.doAnimalDe(#id))")
    @Operation(summary = "Recusar o pedido, com motivo")
    public ResponseEntity<SolicitacaoAlteracaoResponse> recusar(
            @PathVariable UUID id, @Valid @RequestBody RecusaRequest request) {
        return ResponseEntity.ok(service.recusar(id, request.getMotivo()));
    }

    // ================================================================
    // Histórico por animal
    // ================================================================

    @GetMapping("/animais/{animalId}/solicitacoes-alteracao")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#animalId)")
    @Operation(summary = "Histórico de pedidos deste animal")
    public ResponseEntity<List<SolicitacaoAlteracaoResponse>> doAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(service.doAnimal(animalId));
    }
}
