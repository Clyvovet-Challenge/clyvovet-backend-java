package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoPatchRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.controller.hateoas.LinksDoEvento;
import br.com.fiap.clyvovet.service.EventoClinicoService;
import org.springframework.hateoas.EntityModel;
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
    private final LinksDoEvento links;

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
    /**
     * A resposta carrega os links das transicoes possiveis NESTE estado.
     *
     * Um evento AGENDADO traz "cancelar" e "concluir"; o mesmo evento, depois de
     * cancelado, nao traz nenhum dos dois. E o que leva a API do nivel 2 ao
     * nivel 3 de Richardson: o cliente descobre o que pode fazer pela resposta,
     * em vez de carregar por fora uma copia da maquina de estados que envelhece
     * em silencio quando a regra do servidor muda.
     *
     * O EntityModel serializa o conteudo inline e acrescenta "_links", entao o
     * contrato antigo continua valendo: quem lia "$.id" continua lendo "$.id".
     */
    @GetMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Buscar evento clínico por ID, com os links das ações possíveis")
    public ResponseEntity<EntityModel<EventoClinicoResponse>> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(links.comLinks(eventoClinicoService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Registrar novo evento clínico")
    public ResponseEntity<EventoClinicoResponse> criar(@Valid @RequestBody EventoClinicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoClinicoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Atualizar evento clínico existente")
    public ResponseEntity<EventoClinicoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EventoClinicoRequest request) {
        return ResponseEntity.ok(eventoClinicoService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Atualizar parcialmente um evento clínico: envie apenas os campos que mudam")
    public ResponseEntity<EventoClinicoResponse> atualizarParcialmente(
            @PathVariable UUID id,
            @Valid @RequestBody EventoClinicoPatchRequest patch) {
        return ResponseEntity.ok(eventoClinicoService.atualizarParcialmente(id, patch));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    @Operation(summary = "Remover evento clínico")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        eventoClinicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}