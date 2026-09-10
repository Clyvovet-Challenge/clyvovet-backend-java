package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.clinica.ClinicaPatchRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaResponse;
import br.com.fiap.clyvovet.mapper.ClinicaMapper;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicaService {

    private final ClinicaRepository clinicaRepository;
    private final ClinicaMapper clinicaMapper;

    /**
     * Ver a nota sobre {@code #pageable} na chave em {@code TutorService}.
     *
     * <p><b>Todo parametro de filtro precisa entrar na chave.</b> A chave e
     * escrita a mao, entao ela nao acompanha a assinatura do metodo: quando
     * {@code busca} foi acrescentado, a chave continuou sendo
     * {@code nome-cidade-pageable} e duas pesquisas diferentes passaram a
     * colidir. Efeito verificado em teste: {@code ?busca=VetCare} devolveu
     * "PetMed Centro" -- o resultado da pesquisa ANTERIOR, servido por dez
     * minutos, sem erro nenhum na resposta.</p>
     */
    @Cacheable(value = "clinicas", key = "#nome + '-' + #cidade + '-' + #busca + '-' + #pageable")
    public Page<ClinicaResponse> listarTodos(String nome, String cidade, String busca, Pageable pageable) {
        return clinicaRepository.buscarPorFiltros(nome, cidade, vazioComoNulo(busca), pageable)
                .map(clinicaMapper::toResponse);
    }

    /**
     * Texto em branco vira nulo, porque a consulta trata os dois de formas
     * opostas: nulo DESLIGA o filtro, e string vazia casa com tudo via
     * {@code LIKE '%%'}.
     *
     * <p>Hoje os dois dao o mesmo resultado, por acidente. Deixariam de dar no
     * dia em que a busca ganhasse mais uma condicao -- e o app manda
     * {@code busca=} de verdade, a cada tecla apagada na caixa de pesquisa.</p>
     */
    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public ClinicaResponse buscarPorId(UUID id) {
        return clinicaMapper.toResponse(clinicaRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = {"clinicas", "eventos"}, allEntries = true)
    public ClinicaResponse criar(ClinicaRequest request) {
        return clinicaMapper.toResponse(clinicaRepository.save(clinicaMapper.toEntity(request)));
    }

    @Transactional
    @CacheEvict(value = {"clinicas", "eventos"}, allEntries = true)
    public ClinicaResponse atualizar(UUID id, ClinicaRequest request) {
        Clinica clinica = clinicaRepository.obterPorId(id);
        clinicaMapper.atualizar(clinica, request);
        return clinicaMapper.toResponse(clinicaRepository.save(clinica));
    }

    @Transactional
    @CacheEvict(value = {"clinicas", "eventos"}, allEntries = true)
    public ClinicaResponse atualizarParcialmente(UUID id, ClinicaPatchRequest patch) {
        Clinica clinica = clinicaRepository.obterPorId(id);
        clinicaMapper.aplicarPatch(clinica, patch);
        return clinicaMapper.toResponse(clinicaRepository.save(clinica));
    }

    @Transactional
    @CacheEvict(value = {"clinicas", "eventos"}, allEntries = true)
    public void deletar(UUID id) {
        clinicaRepository.garantirQueExiste(id);
        clinicaRepository.deleteById(id);
    }
}
