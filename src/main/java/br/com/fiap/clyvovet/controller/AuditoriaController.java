package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.historico.ExcessoDeAcessoResponse;
import br.com.fiap.clyvovet.service.AuditoriaService;
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
 * Revisao dos tetos de acesso ao historico (regras C6 e C22 da spec 08).
 *
 * Sem estas consultas os tetos so produziriam linhas de log. A resposta sai da
 * tabela de auditoria; nao ha entidade de alerta separada.
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Revisão dos tetos de acesso ao histórico clínico")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /**
     * Por profissional e por dia, nao um total acumulado: trinta animais num dia
     * e um plantao cheio, trinta por dia num mes e outra coisa.
     */
    @GetMapping("/excessos")
    @Operation(summary = "Profissionais acima do teto diário de consultas ao resumo de segurança")
    public ResponseEntity<List<ExcessoDeAcessoResponse>> excessos(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(auditoriaService.excessosDeLeitura(dias));
    }

    /** A quebra de vidro nunca e bloqueada; o controle e ela ficar visivel aqui. */
    @GetMapping("/quebras-de-vidro")
    @Operation(summary = "Profissionais que acionaram acesso emergencial com frequência")
    public ResponseEntity<List<ExcessoDeAcessoResponse>> quebrasDeVidro(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(auditoriaService.quebrasDeVidroRecorrentes(dias));
    }
}
