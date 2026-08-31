package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.autorizacao.AutorizacaoResponse;
import br.com.fiap.clyvovet.dto.historico.AcessoResponse;
import br.com.fiap.clyvovet.dto.historico.AlertaRequest;
import br.com.fiap.clyvovet.dto.historico.AlertaResponse;
import br.com.fiap.clyvovet.dto.historico.EmergenciaRequest;
import br.com.fiap.clyvovet.dto.historico.HistoricoResponse;
import br.com.fiap.clyvovet.dto.historico.ResumoDeSegurancaResponse;
import br.com.fiap.clyvovet.service.AlertaService;
import br.com.fiap.clyvovet.service.AutorizacaoService;
import br.com.fiap.clyvovet.service.HistoricoService;
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
 * Acesso ao historico clinico, nos tres niveis da spec 08.
 *
 * As rotas nao dizem o nivel: ele e resolvido por quem pergunta, sobre qual
 * animal, com qual consentimento. Um veterinario e o tutor chamam o MESMO
 * /historico e recebem objetos de tamanhos diferentes — e a resposta carrega
 * nivelDeAcesso justamente para que quem consome saiba qual dos dois recebeu.
 * Sem esse campo, uma linha do tempo curta seria indistinguivel de um animal
 * com pouco historico.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Histórico clínico",
     description = "Resumo de segurança, histórico completo e auditoria de acesso")
public class HistoricoController {

    private final HistoricoService historicoService;
    private final AutorizacaoService autorizacaoService;
    private final AlertaService alertaService;

    /**
     * Nivel 1 — o resumo pelo microchip.
     *
     * Sem consentimento e sem vinculo previo: e o caso do animal que chega numa
     * clinica que nunca o atendeu. O que credencia e a autenticacao do
     * veterinario; o chip apenas identifica.
     */
    @GetMapping("/animais/resumo")
    @Operation(summary = "Resumo de segurança pelo microchip: alergias, condições crônicas, vacinas")
    public ResponseEntity<ResumoDeSegurancaResponse> porMicrochip(@RequestParam String microchip) {
        return ResponseEntity.ok(historicoService.porMicrochip(microchip));
    }

    @GetMapping("/animais/{id}/historico")
    @Operation(summary = "Histórico clínico, no nível que o solicitante alcança")
    public ResponseEntity<HistoricoResponse> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(historicoService.historico(id));
    }

    @PostMapping("/animais/{id}/acesso-emergencial")
    @Operation(summary = "Quebra de vidro: histórico sem consentimento, com motivo e aviso ao tutor")
    public ResponseEntity<HistoricoResponse> emergencia(
            @PathVariable UUID id, @Valid @RequestBody EmergenciaRequest request) {
        return ResponseEntity.ok(historicoService.acessoEmergencial(id, request.getMotivo()));
    }

    /** A transparencia e o que torna o acesso sem consentimento aceitavel. */
    @GetMapping("/animais/{id}/acessos")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
    @Operation(summary = "Quem leu o histórico deste animal, e quando")
    public ResponseEntity<List<AcessoResponse>> acessos(@PathVariable UUID id) {
        return ResponseEntity.ok(historicoService.acessos(id));
    }

    @PostMapping("/animais/{id}/alertas")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
    @Operation(summary = "Registrar alergia, condição crônica ou medicação contínua")
    public ResponseEntity<AlertaResponse> registrarAlerta(
            @PathVariable UUID id, @Valid @RequestBody AlertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaService.registrar(id, request));
    }

    @DeleteMapping("/alertas/{id}")
    @Operation(summary = "Desativar alerta. Não remove: o registro de quem o criou permanece")
    public ResponseEntity<Void> desativarAlerta(@PathVariable UUID id) {
        alertaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/autorizacoes/minhas")
    @Operation(summary = "Quais clínicas têm acesso aos históricos dos meus animais")
    public ResponseEntity<List<AutorizacaoResponse>> minhasAutorizacoes() {
        return ResponseEntity.ok(autorizacaoService.minhas());
    }

    @PostMapping("/autorizacoes/{id}/revogar")
    @Operation(summary = "Retirar o acesso de uma clínica ao histórico do animal")
    public ResponseEntity<AutorizacaoResponse> revogar(@PathVariable UUID id) {
        return ResponseEntity.ok(autorizacaoService.revogar(id));
    }
}
