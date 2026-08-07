package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.clinica.ClinicaRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaResponse;
import br.com.fiap.clyvovet.mapper.ClinicaMapper;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicaService {

    private final ClinicaRepository clinicaRepository;
    private final ClinicaMapper clinicaMapper;

    // Ver a nota sobre #pageable na chave em TutorService.
    @Cacheable(value = "clinicas", key = "#nome + '-' + #cidade + '-' + #pageable")
    public Page<ClinicaResponse> listarTodos(String nome, String cidade, Pageable pageable) {
        return clinicaRepository.buscarPorFiltros(nome, cidade, pageable)
                .map(clinicaMapper::clinicaToResponse);
    }

    public ClinicaResponse buscarPorId(UUID id) {
        return clinicaRepository.findById(id)
                .map(clinicaMapper::clinicaToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + id));
    }

    @CacheEvict(value = "clinicas", allEntries = true)
    public ClinicaResponse salvar(ClinicaRequest request) {
        return clinicaMapper.clinicaToResponse(
                clinicaRepository.save(clinicaMapper.toEntity(request)));
    }

    @CacheEvict(value = "clinicas", allEntries = true)
    public ClinicaResponse atualizar(UUID id, ClinicaRequest request) {
        Clinica clinica = clinicaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + id));
        clinicaMapper.atualizar(clinica, request);
        return clinicaMapper.clinicaToResponse(clinicaRepository.save(clinica));
    }

    @CacheEvict(value = "clinicas", allEntries = true)
    public void deletar(UUID id) {
        if (!clinicaRepository.existsById(id)) {
            throw new EntityNotFoundException("Clínica não encontrada com ID: " + id);
        }
        clinicaRepository.deleteById(id);
    }
}