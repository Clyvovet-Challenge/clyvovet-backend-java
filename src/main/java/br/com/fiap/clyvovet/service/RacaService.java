package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.raca.RacaResponse;
import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import br.com.fiap.clyvovet.model.EspecieAnimal;
import br.com.fiap.clyvovet.model.Raca;
import br.com.fiap.clyvovet.repository.RacaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * O catalogo de racas.
 *
 * So leitura: o catalogo muda por migracao, nao por chamada de API. Isso e
 * deliberado -- ele e a fonte da verdade que o app, a arte em pixel e as
 * predisposicoes da .NET compartilham, e deixar qualquer cliente inserir linha
 * ali traria de volta exatamente o problema que a tabela veio resolver.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RacaService {

    private final RacaRepository racaRepository;

    /**
     * O catalogo inteiro, ou o de uma especie.
     *
     * Cacheado porque muda por migracao: entre dois deploys a resposta e sempre
     * a mesma, e a tela de cadastro pede isso toda vez que abre.
     */
    @Cacheable(value = "racas", key = "#especie != null ? #especie.name() : 'todas'")
    public List<RacaResponse> listar(EspecieAnimal especie) {
        List<Raca> racas = especie == null
                ? racaRepository.findByAtivoTrueOrderByNomeAsc()
                : racaRepository.findByEspecieAndAtivoTrueOrderByNomeAsc(especie);
        return racas.stream().map(RacaService::paraResposta).toList();
    }

    /**
     * Busca a entidade para o AnimalService usar ao resolver `racaId`.
     *
     * Devolve a entidade, e nao o DTO, porque quem chama precisa da referencia
     * para gravar na FK.
     */
    public Raca buscarEntidade(UUID id) {
        return racaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(Recurso.RACA, id));
    }

    static RacaResponse paraResposta(Raca r) {
        return new RacaResponse(
                r.getId(),
                r.getEspecie(),
                r.getEspecie().rotulo(),
                r.getNome(),
                r.getChave(),
                r.getPorteTipico());
    }
}
