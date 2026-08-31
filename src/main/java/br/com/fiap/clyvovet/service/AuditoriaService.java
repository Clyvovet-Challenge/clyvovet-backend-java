package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.historico.ExcessoDeAcessoResponse;
import br.com.fiap.clyvovet.repository.AcessoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A leitura dos tetos, para revisao do administrador da plataforma.
 *
 * Os limites vivem aqui repetidos como constantes de CONSULTA, e nao importados
 * do HistoricoService: la eles decidem se uma requisicao passa, aqui eles apenas
 * filtram o que vale a pena olhar. Sao papeis diferentes e podem divergir de
 * proposito — o admin pode querer revisar a partir de um numero mais baixo do
 * que o que dispara alarme automatico.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    /** A partir de quantos animais distintos por dia vale a pena olhar. */
    private static final long ANIMAIS_POR_DIA_PARA_REVISAR = 30;

    /**
     * Zero, e nao um: TODA quebra de vidro entra na lista.
     *
     * Ela e o caminho de excecao — acesso ao prontuario sem consentimento do
     * tutor. Filtrar por frequencia aqui deixaria de fora justamente o caso que
     * mais interessa: o profissional que aciona uma vez, contra um paciente
     * escolhido. Frequencia e um padrao a observar na lista, nao um criterio
     * para entrar nela.
     */
    private static final long QUEBRAS_NO_DIA_PARA_REVISAR = 0;

    private final AcessoHistoricoRepository acessoRepository;

    public List<ExcessoDeAcessoResponse> excessosDeLeitura(int dias) {
        return montar(dias, false, ANIMAIS_POR_DIA_PARA_REVISAR);
    }

    public List<ExcessoDeAcessoResponse> quebrasDeVidroRecorrentes(int dias) {
        return montar(dias, true, QUEBRAS_NO_DIA_PARA_REVISAR);
    }

    /**
     * A consulta devolve Object[] porque e uma projecao com GROUP BY, e nao uma
     * entidade — o JPQL nao tem uma linha de AcessoHistorico para materializar
     * quando o resultado e uma contagem agrupada. A conversao para o record
     * acontece aqui, num lugar so, para que o Object[] nao vaze para o
     * controller.
     */
    private List<ExcessoDeAcessoResponse> montar(int dias, boolean emergencial, long teto) {
        LocalDate desde = LocalDate.now().minusDays(Math.max(dias, 1));

        return acessoRepository.acimaDoTeto(desde, emergencial, teto).stream()
                .map(linha -> new ExcessoDeAcessoResponse(
                        (UUID) linha[0],
                        (String) linha[1],
                        (LocalDate) linha[2],
                        (Long) linha[3],
                        emergencial))
                .toList();
    }
}
