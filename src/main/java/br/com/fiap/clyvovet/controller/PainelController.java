package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.painel.PainelDaClinicaResponse;
import br.com.fiap.clyvovet.service.PainelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O painel de uma clinica.
 *
 * <p>Separado do {@link ClinicaController} pelo mesmo motivo que a cobranca e
 * separada do pagamento: aquele expoe o CRUD da entidade, este expoe uma LEITURA
 * agregada que nao mapeia para nenhuma linha de tabela.</p>
 *
 * <p>A clinica vem no caminho, e nao do usuario logado, e isso e escolha. Um
 * {@code GET /painel} implicito responderia o que para o ADMIN da plataforma, que
 * nao tem clinica? Somaria todas — um numero que nao e o painel de ninguem. Com o
 * id explicito, o ADMIN abre o painel de qualquer casa, cada um dos outros abre o
 * da sua, e a pergunta e a mesma nos dois casos.</p>
 */
@RestController
@RequestMapping("/clinicas")
@RequiredArgsConstructor
@Tag(name = "Painel da clínica", description = "Como o negócio foi no período")
public class PainelController {

    private final PainelService painelService;

    @GetMapping("/{id}/painel")
    @PreAuthorize("@seguranca.podeVerPainelDe(#id)")
    @Operation(summary = "Movimento, faturamento, desfechos, raças e serviços da clínica",
            description = "Tudo ancorado na data do ATENDIMENTO, inclusive o dinheiro: "
                    + "o faturamento mostrado é o dos atendimentos contados ao lado dele, "
                    + "e não o dos pagamentos que caíram no período. "
                    + "Sem parâmetros, a janela são os últimos 30 dias.")
    public ResponseEntity<PainelDaClinicaResponse> painel(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(painelService.daClinica(id, de, ate));
    }
}
