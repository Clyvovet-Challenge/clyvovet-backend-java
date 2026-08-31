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
 * A revisao dos tetos de acesso ao historico, pelo administrador da plataforma.
 *
 * POR QUE ISTO EXISTE
 * Sem uma consulta, os tetos das regras C6 e C22 produziriam apenas linhas de
 * log — e log que ninguem abre nao e controle, e registro. O alarme precisa
 * chegar a alguem que possa agir, e a forma mais barata de garantir isso e uma
 * rota que responde a pergunta "quem passou do limite?".
 *
 * Nao ha entidade nova por tras: a resposta sai da mesma tabela de auditoria
 * que ja registra cada leitura. Uma tabela de alertas paralela criaria uma
 * segunda versao da verdade para manter em sincronia com esta.
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Revisão dos tetos de acesso ao histórico clínico")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /**
     * Quem leu muitos prontuarios distintos num dia (regra C6).
     *
     * A resposta e por profissional e por dia, e nao um total acumulado: trinta
     * animais num dia e um plantao cheio; trinta por dia durante um mes e outra
     * coisa, e so a serie diaria deixa as duas distinguiveis.
     */
    @GetMapping("/excessos")
    @Operation(summary = "Profissionais acima do teto diário de consultas ao resumo de segurança")
    public ResponseEntity<List<ExcessoDeAcessoResponse>> excessos(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(auditoriaService.excessosDeLeitura(dias));
    }

    /**
     * Quem aciona quebra de vidro com frequencia (regra C22).
     *
     * A quebra de vidro nunca e bloqueada — travar o acesso numa emergencia
     * cobraria a conta do paciente, nao de quem abusa. O controle e este: ela
     * passa, e fica visivel aqui.
     */
    @GetMapping("/quebras-de-vidro")
    @Operation(summary = "Profissionais que acionaram acesso emergencial com frequência")
    public ResponseEntity<List<ExcessoDeAcessoResponse>> quebrasDeVidro(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(auditoriaService.quebrasDeVidroRecorrentes(dias));
    }
}
