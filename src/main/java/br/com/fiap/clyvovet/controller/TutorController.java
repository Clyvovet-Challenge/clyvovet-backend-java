package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.service.TutorService;
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
@RequestMapping("/tutores")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Gerenciamento de tutores de pets")
public class TutorController {

    private final TutorService tutorService;

    @GetMapping
    @Operation(summary = "Listar tutores com paginação e filtros por nome e cidade")
    public ResponseEntity<Page<TutorResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarTodos(nome, cidade, pageable));
    }

    // Um tutor pode consultar o proprio cadastro, mas nao o de terceiros.
    @GetMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarTutor(#id)")
    @Operation(summary = "Buscar tutor por ID")
    public ResponseEntity<TutorResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo tutor")
    public ResponseEntity<TutorResponse> criar(@Valid @RequestBody TutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tutorService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarTutor(#id)")
    @Operation(summary = "Atualizar tutor existente")
    public ResponseEntity<TutorResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TutorRequest request) {
        return ResponseEntity.ok(tutorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover tutor")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        tutorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}