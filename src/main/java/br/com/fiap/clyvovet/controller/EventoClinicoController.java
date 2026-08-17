package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.service.EventoClinicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/eventos-clinicos")
@RequiredArgsConstructor
@Tag(name = "Eventos Clínicos", description = "Gerenciamento de eventos clínicos")
public class EventoClinicoController {

    private final EventoClinicoService eventoClinicoService;

    @GetMapping
    @Operation(summary = "Listar eventos com paginação e filtros por tipo e nome do animal")
    public ResponseEntity<Page<EventoClinicoResponse>> listarTodos(
            @RequestParam(required = false) TipoEvento tipoEvento,
            @RequestParam(required = false) String animalNome,
            @PageableDefault(size = 10, sort = "data") Pageable pageable) {
        return ResponseEntity.ok(eventoClinicoService.listarTodos(tipoEvento, animalNome, pageable));
    }

    // Escrita ja e restrita a VETERINARIO/ADMIN pela regra de rota; aqui o que
    // se protege e a leitura: um tutor so pode ver eventos dos proprios pets.
    @GetMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Buscar evento clínico por ID")
    public ResponseEntity<EventoClinicoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoClinicoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registrar novo evento clínico")
    public ResponseEntity<EventoClinicoResponse> criar(@Valid @RequestBody EventoClinicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoClinicoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento clínico existente")
    public ResponseEntity<EventoClinicoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EventoClinicoRequest request) {
        return ResponseEntity.ok(eventoClinicoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover evento clínico")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        eventoClinicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}