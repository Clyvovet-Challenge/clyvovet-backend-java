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
 * Leitura dos tetos, para revisao do admin.
 *
 * Os limites sao constantes proprias, e nao as do HistoricoService: la eles
 * decidem se a requisicao passa, aqui so filtram o que vale olhar.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    /** A partir de quantos animais distintos por dia vale a pena olhar. */
    private static final long ANIMAIS_POR_DIA_PARA_REVISAR = 30;

    /**
     * Zero: toda quebra de vidro entra na lista. Filtrar por frequencia
     * esconderia quem aciona uma vez, contra um paciente escolhido.
     */
    private static final long QUEBRAS_NO_DIA_PARA_REVISAR = 0;

    private final AcessoHistoricoRepository acessoRepository;

    public List<ExcessoDeAcessoResponse> excessosDeLeitura(int dias) {
        return montar(dias, false, ANIMAIS_POR_DIA_PARA_REVISAR);
    }

    public List<ExcessoDeAcessoResponse> quebrasDeVidroRecorrentes(int dias) {
        return montar(dias, true, QUEBRAS_NO_DIA_PARA_REVISAR);
    }

    /** Object[] porque a consulta e uma projecao com GROUP BY. Nao vaza daqui. */
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
