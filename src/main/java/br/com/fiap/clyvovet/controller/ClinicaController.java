package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.clinica.ClinicaPatchRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaResponse;
import br.com.fiap.clyvovet.service.ClinicaService;
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
@RequestMapping("/clinicas")
@RequiredArgsConstructor
@Tag(name = "Clínicas", description = "Gerenciamento de clínicas parceiras")
public class ClinicaController {

    private final ClinicaService clinicaService;

    /**
     * @param busca busca livre do app: cruza nome, bairro, cidade e estado com
     *              OR. O tutor digita "VetCare", "Pinheiros" ou "Sao Paulo" na
     *              mesma caixa, e nao e ele que deve descobrir em qual campo
     *              aquilo se encaixa.
     */
    @GetMapping
    @Operation(summary = "Listar clínicas: busca livre (nome/bairro/cidade/estado) ou filtros separados")
    public ResponseEntity<Page<ClinicaResponse>> listarTodos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(clinicaService.listarTodos(nome, cidade, busca, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar clínica por ID")
    public ResponseEntity<ClinicaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicaService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova clínica")
    public ResponseEntity<ClinicaResponse> criar(@Valid @RequestBody ClinicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicaService.criar(request));
    }

    // O ADMIN_CLINICA edita a PROPRIA clinica, e so ela.
    @PreAuthorize("@seguranca.ehAdministradorDe(#id)")
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar clínica existente")
    public ResponseEntity<ClinicaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicaRequest request) {
        return ResponseEntity.ok(clinicaService.atualizar(id, request));
    }

    @PreAuthorize("@seguranca.ehAdministradorDe(#id)")
    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar parcialmente um clínica: envie apenas os campos que mudam")
    public ResponseEntity<ClinicaResponse> atualizarParcialmente(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicaPatchRequest patch) {
        return ResponseEntity.ok(clinicaService.atualizarParcialmente(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover clínica")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        clinicaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}