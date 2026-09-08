package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.servico.ServicoRequest;
import br.com.fiap.clyvovet.dto.servico.ServicoResponse;
import br.com.fiap.clyvovet.service.ServicoService;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Catálogo de serviços oferecidos pelas clínicas")
public class ServicoController {

    private final ServicoService servicoService;

    /** Aninhado na clinica porque servico nao existe fora de uma. */
    @GetMapping("/clinicas/{clinicaId}/servicos")
    @Operation(summary = "Serviços ativos de uma clínica")
    public ResponseEntity<List<ServicoResponse>> daClinica(@PathVariable UUID clinicaId) {
        return ResponseEntity.ok(servicoService.daClinica(clinicaId));
    }

    // A rota abre o verbo para ADMIN_CLINICA; esta linha decide de QUAL clinica.
    // Sem ela, um administrador cadastraria servico no catalogo da concorrente.
    @PreAuthorize("@seguranca.podeGerirCatalogoDe(#request.clinicaId)")
    @PostMapping("/servicos")
    @Operation(summary = "Cadastrar serviço no catálogo")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(request));
    }

    @PreAuthorize("@seguranca.podeGerirServico(#id)")
    @PutMapping("/servicos/{id}")
    @Operation(summary = "Atualizar serviço do catálogo")
    public ResponseEntity<ServicoResponse> atualizar(
            @PathVariable UUID id, @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(id, request));
    }

    @PreAuthorize("@seguranca.podeGerirServico(#id)")
    @DeleteMapping("/servicos/{id}")
    @Operation(summary = "Desativar serviço. Não remove: o histórico de preços depende dele")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        servicoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
