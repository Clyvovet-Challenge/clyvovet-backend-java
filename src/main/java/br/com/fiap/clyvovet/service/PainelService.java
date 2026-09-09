package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.painel.AtendimentosNoPainel;
import br.com.fiap.clyvovet.dto.painel.ContagemNoPainel;
import br.com.fiap.clyvovet.dto.painel.FaturamentoNoPainel;
import br.com.fiap.clyvovet.dto.painel.PainelDaClinicaResponse;
import br.com.fiap.clyvovet.dto.painel.ServicoNoPainel;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.StatusEvento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.PagamentoRepository;
import br.com.fiap.clyvovet.repository.projecao.ContagemPorDesfecho;
import br.com.fiap.clyvovet.repository.projecao.ContagemPorRotulo;
import br.com.fiap.clyvovet.repository.projecao.ContagemPorServico;
import br.com.fiap.clyvovet.repository.projecao.ContagemPorStatus;
import br.com.fiap.clyvovet.repository.projecao.ValorPorServicoEStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * O painel da clinica — o que o administrador do estabelecimento abre para saber
 * como o negocio esta indo.
 *
 * <p>Ate aqui a API respondia bem a perguntas sobre UM registro: este atendimento
 * aconteceu, este pagamento entrou, este pet precisa voltar. Nao respondia a
 * nenhuma pergunta sobre o CONJUNTO — quantos faltaram no mes, quanto entrou,
 * quais servicos sustentam a casa. Era o que faltava para a clinica ser um ator, e
 * nao apenas um campo de recorte nas consultas dos outros.</p>
 *
 * <p><b>Sem cache, de proposito.</b> As listagens da API sao cacheadas por dez
 * minutos, e aqui isso custaria caro: o painel depende de escritas que acontecem em
 * quatro services diferentes (atendimento, conclusao, cobranca, falta), e cada um
 * precisaria lembrar de invalidar. O preco de esquecer uma invalidacao e mostrar
 * dinheiro errado na tela — bem pior que repetir cinco consultas agregadas na vez
 * em que alguem abre o painel.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PainelService {

    /** Janela usada quando quem chama nao diz qual quer. */
    private static final int JANELA_PADRAO_EM_DIAS = 30;

    /** Barras suficientes para enxergar a concentracao, sem virar lista. */
    private static final int LINHAS_NO_TOPO = 5;

    /**
     * Locale fixo no toUpperCase da chave de raca.
     *
     * <p>Sem ele, o padrao e o do servidor. Num container com locale turco, "i" vira
     * "İ" e "Siames" deixa de casar consigo mesmo entre duas maquinas — o classico
     * bug do I turco, que aqui apareceria como raca duplicada no grafico so em
     * producao.</p>
     */
    private static final Locale LOCALE_DO_DADO = Locale.ROOT;

    private static final String RACA_NAO_INFORMADA = "Não informada";
    private static final String DESFECHO_NAO_REGISTRADO = "Não registrado";

    private final ClinicaRepository clinicaRepository;
    private final EventoClinicoRepository eventoClinicoRepository;
    private final PagamentoRepository pagamentoRepository;

    /**
     * Quem pode chamar isto e decidido antes, por {@code @seguranca.podeVerPainelDe}
     * no controller. Aqui a clinica ja e uma so, e nao ha recorte a aplicar: o
     * proprio id do caminho e o recorte.
     */
    public PainelDaClinicaResponse daClinica(UUID clinicaId, LocalDate de, LocalDate ate) {
        Clinica clinica = clinicaRepository.obterPorId(clinicaId);

        LocalDate fim = ate != null ? ate : LocalDate.now();
        // O padrao de "de" sai de "ate", e nao de hoje: quem pede so ?ate=2024-12-31
        // quer os trinta dias ANTES daquela data, e nao uma janela vazia.
        LocalDate inicio = de != null ? de : fim.minusDays(JANELA_PADRAO_EM_DIAS - 1L);
        garantirPeriodoCoerente(inicio, fim);

        Map<StatusEvento, Long> porStatus = porStatus(clinica.getId(), inicio, fim);
        AtendimentosNoPainel atendimentos = atendimentos(porStatus);
        List<ValorPorServicoEStatus> dinheiro =
                pagamentoRepository.totaisPorServicoEStatus(clinica.getId(), inicio, fim);

        return new PainelDaClinicaResponse(
                clinica.getId(),
                clinica.getNome(),
                inicio,
                fim,
                atendimentos,
                faturamento(dinheiro, atendimentos.realizados()),
                desfechos(clinica.getId(), inicio, fim, atendimentos.realizados()),
                racas(clinica.getId(), inicio, fim, atendimentos.realizados()),
                servicos(clinica.getId(), inicio, fim, dinheiro));
    }

    /**
     * Periodo invertido e erro, e nao janela vazia.
     *
     * <p>Sem esta guarda, {@code ?de=2026-09-30&ate=2026-09-01} responde 200 com
     * tudo zerado — e "zero atendimento" e "a clinica parou de trabalhar" viram a
     * mesma tela. O 409 diz qual dos dois e.</p>
     */
    private void garantirPeriodoCoerente(LocalDate de, LocalDate ate) {
        if (de.isAfter(ate)) {
            throw new RegraDeNegocioException("de",
                    "O início do período não pode ser depois do fim");
        }
    }

    private Map<StatusEvento, Long> porStatus(UUID clinicaId, LocalDate de, LocalDate ate) {
        Map<StatusEvento, Long> contagem = new EnumMap<>(StatusEvento.class);
        for (ContagemPorStatus linha : eventoClinicoRepository.contagemPorStatus(clinicaId, de, ate)) {
            contagem.merge(linha.status(), quantidade(linha.quantidade()), Long::sum);
        }
        return contagem;
    }

    private AtendimentosNoPainel atendimentos(Map<StatusEvento, Long> porStatus) {
        long realizados = quantidadeDe(porStatus, StatusEvento.REALIZADO);
        long faltas = quantidadeDe(porStatus, StatusEvento.FALTOU);
        // A base das taxas exclui o cancelado: quem desmarcou devolveu o horario, e
        // somar isso as faltas mistura duas coisas que a clinica trata diferente.
        long compareceramOuNao = realizados + faltas;

        return new AtendimentosNoPainel(
                quantidadeDe(porStatus, StatusEvento.AGENDADO),
                realizados,
                faltas,
                quantidadeDe(porStatus, StatusEvento.CANCELADO),
                // Somado do mapa, e nao dos quatro campos acima: um status novo no
                // ciclo de vida entra no total sem passar despercebido.
                porStatus.values().stream().mapToLong(this::quantidade).sum(),
                percentual(realizados, compareceramOuNao),
                percentual(faltas, compareceramOuNao));
    }

    private FaturamentoNoPainel faturamento(List<ValorPorServicoEStatus> linhas, long realizados) {
        BigDecimal recebido = somaPorStatus(linhas, StatusPagamento.PAGO);
        long pagamentos = linhas.stream().mapToLong(linha -> quantidade(linha.quantidade())).sum();

        return new FaturamentoNoPainel(
                recebido,
                somaPorStatus(linhas, StatusPagamento.PENDENTE),
                somaPorStatus(linhas, StatusPagamento.REEMBOLSADO),
                somaPorStatus(linhas, StatusPagamento.CANCELADO),
                // Sem atendimento nao ha ticket. Dividir aqui seria ArithmeticException
                // em toda clinica que abrisse o painel num periodo parado.
                realizados == 0
                        ? dinheiro(BigDecimal.ZERO)
                        : recebido.divide(BigDecimal.valueOf(realizados), 2, RoundingMode.HALF_UP),
                pagamentos);
    }

    private List<ContagemNoPainel> desfechos(UUID clinicaId, LocalDate de, LocalDate ate, long base) {
        Map<String, Long> agrupado = new LinkedHashMap<>();
        for (ContagemPorDesfecho linha : eventoClinicoRepository.contagemPorDesfecho(clinicaId, de, ate)) {
            String rotulo = linha.desfecho() != null ? linha.desfecho().name() : DESFECHO_NAO_REGISTRADO;
            agrupado.merge(rotulo, quantidade(linha.quantidade()), Long::sum);
        }
        // Sem limite: sao cinco desfechos possiveis mais o nao registrado, e cortar
        // justamente OBITO por ser o menos frequente seria esconder o que importa.
        return maioresPrimeiro(agrupado, base, agrupado.size());
    }

    /**
     * As racas mais atendidas, com a normalizacao que o dado exige.
     *
     * <p>{@code animal.raca} e texto livre — sem catalogo, sem constraint. O GROUP BY
     * do banco separa "Labrador", "labrador" e "Labrador " em tres grupos, e a tela
     * mostra a mesma raca em tres barras, cada uma com um terco da contagem. Por isso
     * a chave do agrupamento aqui e o texto em caixa alta e sem espaco nas bordas,
     * como o risco 3 da SPEC_PAINEL_VETERINARIO_E_DADOS_IA ja previa.</p>
     *
     * <p>E um paliativo, e a spec diz ate onde ele vai: resolve caixa e espaco, e
     * <b>nao</b> resolve acento nem sinonimo. "Pastor Alemao" e "Pastor Alemão"
     * continuam sendo duas racas ate existir a tabela de referencia.</p>
     *
     * <p>O rotulo exibido e a PRIMEIRA grafia que chega, e a consulta vem ordenada da
     * maior contagem para a menor: entre "Labrador" e "labrador", aparece a que mais
     * gente digitou.</p>
     */
    private List<ContagemNoPainel> racas(UUID clinicaId, LocalDate de, LocalDate ate, long base) {
        Map<String, String> grafiaExibida = new LinkedHashMap<>();
        Map<String, Long> agrupado = new LinkedHashMap<>();

        for (ContagemPorRotulo linha : eventoClinicoRepository.contagemPorRaca(clinicaId, de, ate)) {
            // Nulo e "" viram a MESMA chave, e por isso somam em vez de virar duas
            // barras identicas na tela.
            boolean vazio = linha.rotulo() == null || linha.rotulo().isBlank();
            String chave = vazio ? RACA_NAO_INFORMADA : linha.rotulo().trim().toUpperCase(LOCALE_DO_DADO);
            grafiaExibida.putIfAbsent(chave, vazio ? RACA_NAO_INFORMADA : linha.rotulo().trim());
            agrupado.merge(chave, quantidade(linha.quantidade()), Long::sum);
        }

        Map<String, Long> porGrafia = new LinkedHashMap<>();
        agrupado.forEach((chave, total) -> porGrafia.merge(grafiaExibida.get(chave), total, Long::sum));
        return maioresPrimeiro(porGrafia, base, LINHAS_NO_TOPO);
    }

    private List<ServicoNoPainel> servicos(UUID clinicaId, LocalDate de, LocalDate ate,
                                           List<ValorPorServicoEStatus> dinheiro) {
        Map<UUID, BigDecimal> receitaPorServico = new LinkedHashMap<>();
        for (ValorPorServicoEStatus linha : dinheiro) {
            if (linha.status() == StatusPagamento.PAGO && linha.servicoId() != null) {
                receitaPorServico.merge(linha.servicoId(), dinheiro(linha.total()), BigDecimal::add);
            }
        }

        List<ServicoNoPainel> servicos = new ArrayList<>();
        for (ContagemPorServico linha : eventoClinicoRepository.contagemPorServico(clinicaId, de, ate)) {
            if (servicos.size() == LINHAS_NO_TOPO) {
                break;
            }
            servicos.add(new ServicoNoPainel(
                    linha.servicoId(),
                    linha.nome(),
                    quantidade(linha.quantidade()),
                    receitaPorServico.getOrDefault(linha.servicoId(), dinheiro(BigDecimal.ZERO))));
        }
        return servicos;
    }

    /**
     * Ordena pela quantidade e desempata pelo rotulo.
     *
     * <p>O desempate nao e capricho: sem ele, duas racas com a mesma contagem
     * trocam de lugar entre uma chamada e outra conforme o banco devolver, e a tela
     * pisca a cada atualizacao sem nada ter mudado.</p>
     */
    private List<ContagemNoPainel> maioresPrimeiro(Map<String, Long> agrupado, long base, int limite) {
        return agrupado.entrySet().stream()
                .map(entrada -> new ContagemNoPainel(
                        entrada.getKey(), entrada.getValue(), percentual(entrada.getValue(), base)))
                .sorted(Comparator.comparingLong(ContagemNoPainel::quantidade).reversed()
                        .thenComparing(ContagemNoPainel::rotulo))
                .limit(limite)
                .toList();
    }

    private BigDecimal somaPorStatus(List<ValorPorServicoEStatus> linhas, StatusPagamento status) {
        return dinheiro(linhas.stream()
                .filter(linha -> linha.status() == status)
                .map(linha -> linha.total() != null ? linha.total() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Pontos percentuais com uma casa decimal.
     *
     * <p>Base zero devolve zero em vez de dividir: uma clinica sem atendimento
     * nenhum na janela e o caso mais comum do painel recem-aberto, e ele nao pode
     * responder 500.</p>
     */
    private BigDecimal percentual(long parte, long base) {
        if (base <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(parte)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(base), 1, RoundingMode.HALF_UP);
    }

    /** Duas casas em todo valor monetario, venha ele do SUM ou do zero. */
    private BigDecimal dinheiro(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private long quantidadeDe(Map<StatusEvento, Long> porStatus, StatusEvento status) {
        return quantidade(porStatus.get(status));
    }

    /** O COUNT nunca volta nulo; o mapa sem a chave, sim — e ali zero e a resposta. */
    private long quantidade(Long valor) {
        return valor != null ? valor : 0L;
    }
}
