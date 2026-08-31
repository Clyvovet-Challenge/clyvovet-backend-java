package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.eventoClinico.ConclusaoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.ConclusaoResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.RetornoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.RetornoVencidoResponse;
import br.com.fiap.clyvovet.service.RetornoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conclusao de atendimento, retorno e falta — o segundo fluxo nao-CRUD.
 *
 * As rotas vivem sob /eventos-clinicos porque agem sobre o evento, mas sao
 * ACOES, nao edicao: nenhuma delas aceita o status no corpo. Essa e a diferenca
 * que faz as regras R1-R21 valerem — com o status editavel por PATCH, bastaria
 * um {"statusEvento":"REALIZADO"} para contornar todas.
 */
@RestController
@RequestMapping("/eventos-clinicos")
@RequiredArgsConstructor
@Tag(name = "Retorno e falta", description = "Conclusão do atendimento e controle de retorno")
public class RetornoController {

    private final RetornoService retornoService;

    @PostMapping("/{id}/concluir")
    @Operation(summary = "Concluir o atendimento: peso, desfecho e retorno previsto")
    public ResponseEntity<ConclusaoResponse> concluir(
            @PathVariable UUID id,
            @Valid @RequestBody ConclusaoRequest request) {
        return ResponseEntity.ok(retornoService.concluir(id, request));
    }

    @PostMapping("/{id}/retorno")
    @Operation(summary = "Marcar o retorno ligado a esta consulta")
    public ResponseEntity<EventoClinicoResponse> agendarRetorno(
            @PathVariable UUID id,
            @Valid @RequestBody RetornoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(retornoService.agendarRetorno(id, request));
    }

    /** O resultado do fluxo: sobre quem a clinica precisa agir. */
    @GetMapping("/retornos-vencidos")
    @Operation(summary = "Pets que deveriam ter voltado e não voltaram")
    public ResponseEntity<List<RetornoVencidoResponse>> vencidos(
            @RequestParam(required = false) UUID veterinarioId,
            @RequestParam(required = false) UUID clinicaId) {
        return ResponseEntity.ok(retornoService.vencidos(veterinarioId, clinicaId));
    }

    @PostMapping("/marcar-faltas")
    @Operation(summary = "Marcar como FALTOU os agendamentos vencidos sem conclusão")
    public ResponseEntity<Map<String, Integer>> marcarFaltas() {
        return ResponseEntity.ok(Map.of("marcados", retornoService.marcarFaltas()));
    }
}
