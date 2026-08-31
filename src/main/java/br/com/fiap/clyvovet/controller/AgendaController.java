package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.agenda.BloqueioRequest;
import br.com.fiap.clyvovet.dto.agenda.BloqueioResponse;
import br.com.fiap.clyvovet.dto.agenda.DisponibilidadeRequest;
import br.com.fiap.clyvovet.dto.agenda.DisponibilidadeResponse;
import br.com.fiap.clyvovet.service.AgendaCadastroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A grade de atendimento do veterinario e os furos dela.
 *
 * A leitura e aberta a qualquer autenticado de proposito: o tutor precisa ver
 * quando o profissional atende para escolher um horario. O que a grade expoe e
 * disponibilidade profissional, nao dado pessoal.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Grade de horários e bloqueios do veterinário")
public class AgendaController {

    private final AgendaCadastroService agendaCadastroService;

    @GetMapping("/veterinarios/{veterinarioId}/disponibilidades")
    @Operation(summary = "Grade de horários de um veterinário")
    public ResponseEntity<List<DisponibilidadeResponse>> grade(@PathVariable UUID veterinarioId) {
        return ResponseEntity.ok(agendaCadastroService.gradeDe(veterinarioId));
    }

    @PostMapping("/disponibilidades")
    @Operation(summary = "Cadastrar faixa de atendimento recorrente")
    public ResponseEntity<DisponibilidadeResponse> criarFaixa(
            @Valid @RequestBody DisponibilidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaCadastroService.criarFaixa(request));
    }

    @DeleteMapping("/disponibilidades/{id}")
    @Operation(summary = "Remover faixa da grade")
    public ResponseEntity<Void> removerFaixa(@PathVariable UUID id) {
        agendaCadastroService.removerFaixa(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bloqueios")
    @Operation(summary = "Bloquear período: férias, folga ou almoço")
    public ResponseEntity<BloqueioResponse> criarBloqueio(@Valid @RequestBody BloqueioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendaCadastroService.criarBloqueio(request));
    }

    @DeleteMapping("/bloqueios/{id}")
    @Operation(summary = "Remover bloqueio")
    public ResponseEntity<Void> removerBloqueio(@PathVariable UUID id) {
        agendaCadastroService.removerBloqueio(id);
        return ResponseEntity.noContent().build();
    }
}
