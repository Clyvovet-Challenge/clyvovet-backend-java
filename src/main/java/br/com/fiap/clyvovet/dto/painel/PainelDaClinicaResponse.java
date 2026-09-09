package br.com.fiap.clyvovet.dto.painel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O painel de uma clinica num periodo — a tela de quem administra o negocio.
 *
 * <p>O periodo vem na resposta, e nao so no pedido, porque ele tem padrao: sem
 * {@code de} e {@code ate} a API decide a janela, e quem le precisa saber qual foi
 * para nao atribuir os numeros ao intervalo errado.</p>
 *
 * <p>Tudo aqui esta ancorado na data do ATENDIMENTO, inclusive o dinheiro.</p>
 */
public record PainelDaClinicaResponse(
        UUID clinicaId,
        String clinicaNome,
        LocalDate de,
        LocalDate ate,
        AtendimentosNoPainel atendimentos,
        FaturamentoNoPainel faturamento,
        List<ContagemNoPainel> desfechos,
        List<ContagemNoPainel> racas,
        List<ServicoNoPainel> servicos) {
}
