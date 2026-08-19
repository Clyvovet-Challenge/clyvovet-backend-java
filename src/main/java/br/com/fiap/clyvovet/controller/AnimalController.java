package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.animal.AnimalPatchRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.service.AnimalService;
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
@RequestMapping("/animais")
@RequiredArgsConstructor
@Tag(name = "Animais", description = "Gerenciamento de animais")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping
    @Operation(summary = "Listar animais com paginação e filtros por nome e espécie")
    public ResponseEntity<Page<AnimalResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String especie,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(animalService.listarTodos(nome, especie, pageable));
    }

    // A regra de rota nao resolve ownership: qualquer tutor autenticado passaria
    // por ela. @seguranca compara o dono do animal com o tutor do usuario logado
    // e libera direto para VETERINARIO e ADMIN.
    @GetMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
    @Operation(summary = "Buscar animal por ID")
    public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(animalService.buscarPorId(id));
    }

    // O tutorId vem do CORPO, e nao da URL: sem esta checagem um tutor
    // autenticado cadastrava pet no nome de qualquer outro tutor.
    @PostMapping
    @PreAuthorize("@seguranca.podeAcessarTutor(#request.tutorId)")
    @Operation(summary = "Cadastrar novo animal")
    public ResponseEntity<AnimalResponse> criar(@Valid @RequestBody AnimalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalService.criar(request));
    }

    // Duas verificacoes, porque sao duas perguntas diferentes: o pet e meu
    // (#id) e o dono que estou gravando continua sendo eu (#request.tutorId).
    // Sem a segunda, um tutor transferia o proprio pet para outro tutor.
    @PutMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAcessarTutor(#request.tutorId)")
    @Operation(summary = "Atualizar animal existente")
    public ResponseEntity<AnimalResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AnimalRequest request) {
        return ResponseEntity.ok(animalService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    // O patch sem tutorId nao troca o dono, e ai a segunda checagem
    // nao se aplica -- ver SegurancaService.podeAtribuirTutor.
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id) and @seguranca.podeAtribuirTutor(#patch.tutorId)")
    @Operation(summary = "Atualizar parcialmente um animal: envie apenas os campos que mudam")
    public ResponseEntity<AnimalResponse> atualizarParcialmente(
            @PathVariable UUID id,
            @Valid @RequestBody AnimalPatchRequest patch) {
        return ResponseEntity.ok(animalService.atualizarParcialmente(id, patch));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#id)")
    @Operation(summary = "Remover animal")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        animalService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}