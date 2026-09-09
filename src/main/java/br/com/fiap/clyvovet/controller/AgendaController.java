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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    /**
     * A API sabia criar e apagar bloqueio, e nao sabia lista-lo: quem marcasse as
     * ferias nunca mais via aquilo, nem para conferir nem para desfazer.
     *
     * <p>A leitura e da CASA, e nao de quem estiver autenticado. O corpo do bloqueio
     * traz {@code motivo}, texto livre da clinica, onde cabe "licenca medica" tanto
     * quanto "congresso" — e o tutor nao precisa da rota para nada: a busca por vagas
     * ja desconta os bloqueios do outro lado.</p>
     */
    @PreAuthorize("@seguranca.podeVerAgendaDe(#veterinarioId)")
    @GetMapping("/veterinarios/{veterinarioId}/bloqueios")
    @Operation(summary = "Bloqueios de um veterinário — férias, folgas e almoço — de uma data em diante")
    public ResponseEntity<List<BloqueioResponse>> bloqueios(
            @PathVariable UUID veterinarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return ResponseEntity.ok(agendaCadastroService.bloqueiosDe(veterinarioId, desde));
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
