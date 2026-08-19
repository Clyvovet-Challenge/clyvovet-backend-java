package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioPatchRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.service.VeterinarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/veterinarios")
@RequiredArgsConstructor
@Tag(name = "Veterinários", description = "Gerenciamento de veterinários")
public class VeterinarioController {

    private final VeterinarioService veterinarioService;

    @GetMapping
    @Operation(summary = "Listar veterinários com paginação e filtros por nome e especialidade")
    public ResponseEntity<Page<VeterinarioResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String especialidade,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(veterinarioService.listarTodos(nome, especialidade, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veterinário por ID")
    public ResponseEntity<VeterinarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(veterinarioService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo veterinário")
    public ResponseEntity<VeterinarioResponse> criar(@Valid @RequestBody VeterinarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veterinarioService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veterinário existente")
    public ResponseEntity<VeterinarioResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VeterinarioRequest request) {
        return ResponseEntity.ok(veterinarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar parcialmente um veterinário: envie apenas os campos que mudam")
    public ResponseEntity<VeterinarioResponse> atualizarParcialmente(
            @PathVariable UUID id,
            @Valid @RequestBody VeterinarioPatchRequest patch) {
        return ResponseEntity.ok(veterinarioService.atualizarParcialmente(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover veterinário")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        veterinarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}