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

    // Ver a nota sobre #pageable na chave em TutorService.
    @Cacheable(value = "clinicas", key = "#nome + '-' + #cidade + '-' + #pageable")
    public Page<ClinicaResponse> listarTodos(String nome, String cidade, Pageable pageable) {
        return clinicaRepository.buscarPorFiltros(nome, cidade, pageable)
                .map(clinicaMapper::toResponse);
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
