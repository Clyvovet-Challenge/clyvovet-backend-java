package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.pagamento.*;
import br.com.fiap.clyvovet.service.CobrancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cobrança — as transições de pagamento e as consultas financeiras.
 *
 * Separado do PagamentoController pelo mesmo motivo que o agendamento é
 * separado do evento clínico: aquele expõe CRUD sobre a entidade, este expõe
 * ações do domínio. Nenhuma delas aceita o status no corpo.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Cobrança", description = "Confirmação, estorno e situação financeira")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    @PostMapping("/pagamentos/{id}/confirmar")
    @Operation(summary = "Confirmar o recebimento: PENDENTE vira PAGO")
    public ResponseEntity<PagamentoResponse> confirmar(
            @PathVariable UUID id, @Valid @RequestBody ConfirmacaoRequest request) {
        return ResponseEntity.ok(cobrancaService.confirmar(id, request));
    }

    @PostMapping("/pagamentos/{id}/estornar")
    @Operation(summary = "Estornar um pagamento confirmado. O motivo é obrigatório")
    public ResponseEntity<PagamentoResponse> estornar(
            @PathVariable UUID id, @Valid @RequestBody EstornoRequest request) {
        return ResponseEntity.ok(cobrancaService.estornar(id, request));
    }

    @GetMapping("/eventos-clinicos/{id}/saldo")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Quanto o atendimento custou, quanto entrou e quanto falta")
    public ResponseEntity<SaldoResponse> saldo(@PathVariable UUID id) {
        return ResponseEntity.ok(cobrancaService.saldo(id));
    }

    @GetMapping("/pagamentos/inadimplencia")
    @Operation(summary = "Atendimentos realizados com saldo em aberto")
    public ResponseEntity<List<InadimplenciaResponse>> inadimplencia(
            @RequestParam(defaultValue = "30") int diasMinimos) {
        return ResponseEntity.ok(cobrancaService.inadimplencia(diasMinimos));
    }

    @GetMapping("/tutores/{id}/extrato")
    @PreAuthorize("@seguranca.podeAcessarTutor(#id)")
    @Operation(summary = "O que o tutor pagou e o que está em aberto, no período")
    public ResponseEntity<ExtratoResponse> extrato(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(cobrancaService.extrato(id, de, ate));
    }
}
