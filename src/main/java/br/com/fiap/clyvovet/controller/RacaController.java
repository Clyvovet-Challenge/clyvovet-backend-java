package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.raca.RacaResponse;
import br.com.fiap.clyvovet.model.EspecieAnimal;
import br.com.fiap.clyvovet.service.RacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * O catalogo de racas que alimenta o seletor do cadastro de animal.
 *
 * Sem paginacao de proposito: sao algumas dezenas de linhas e o cliente precisa
 * de todas de uma vez para montar a lista. Paginar aqui obrigaria a tela a
 * juntar paginas para exibir um seletor.
 *
 * Sem POST/PUT/DELETE de proposito: o catalogo muda por migracao. Ver a nota em
 * RacaService.
 */
@RestController
@RequestMapping("/racas")
@RequiredArgsConstructor
@Tag(name = "Raças", description = "Catálogo de raças por espécie")
public class RacaController {

    private final RacaService racaService;

    @GetMapping
    @Operation(summary = "Listar o catálogo de raças, opcionalmente de uma espécie")
    public ResponseEntity<List<RacaResponse>> listar(
            @RequestParam(required = false) EspecieAnimal especie) {
        return ResponseEntity.ok(racaService.listar(especie));
    }
}
