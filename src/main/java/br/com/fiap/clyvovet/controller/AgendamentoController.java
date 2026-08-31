package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.agendamento.AgendamentoRequest;
import br.com.fiap.clyvovet.dto.agendamento.CancelamentoRequest;
import br.com.fiap.clyvovet.dto.agendamento.VagaResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Agendamento pelo tutor — o primeiro fluxo nao-CRUD do sistema.
 *
 * Fica separado de EventoClinicoController de proposito. Aquele expoe as seis
 * operacoes de CRUD sobre a entidade; este expoe TRES ACOES do dominio —
 * marcar, cancelar, ver minha agenda — cada uma com regra propria e recusa
 * propria. Misturar os dois faria a acao parecer mais um verbo de CRUD, que e
 * exatamente o que a Sprint 3 pede para evitar.
 */
@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Marcação de atendimentos pelo tutor")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    /**
     * As vagas livres de um servico. E o que o calendario do frontend consome.
     *
     * Fica antes do POST porque e a ordem em que o tutor as usa: primeiro ve o
     * que ha, depois escolhe.
     */
    @GetMapping("/vagas")
    @Operation(summary = "Vagas livres de um serviço num intervalo de datas")
    public ResponseEntity<List<VagaResponse>> vagas(
            @RequestParam UUID servicoId,
            @RequestParam(required = false) UUID veterinarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(agendamentoService.vagas(servicoId, veterinarioId, de, ate));
    }

    /**
     * O ownership e verificado por animal, e nao por perfil: o tutor so agenda
     * para os proprios pets (A1), e a recepcao da clinica agenda para qualquer
     * um. E a mesma regra que ja protege /animais/{id}.
     */
    @PostMapping
    @PreAuthorize("@seguranca.podeAcessarAnimal(#request.animalId)")
    @Operation(summary = "Marcar um atendimento. O evento nasce AGENDADO")
    public ResponseEntity<EventoClinicoResponse> agendar(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.agendar(request));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Cancelar um agendamento. O motivo é obrigatório")
    public ResponseEntity<EventoClinicoResponse> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody CancelamentoRequest request) {
        return ResponseEntity.ok(agendamentoService.cancelar(id, request.getMotivo()));
    }

    @GetMapping("/meus")
    @Operation(summary = "Agendamentos do tutor autenticado")
    public ResponseEntity<Page<EventoClinicoResponse>> meus(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(agendamentoService.meus(pageable));
    }
}
