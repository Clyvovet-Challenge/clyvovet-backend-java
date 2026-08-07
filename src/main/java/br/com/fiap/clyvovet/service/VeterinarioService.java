package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.mapper.VeterinarioMapper;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
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
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final VeterinarioMapper veterinarioMapper;
    private final ClinicaRepository clinicaRepository;

    // Ver a nota sobre #pageable na chave em TutorService.
    @Cacheable(value = "veterinarios", key = "#nome + '-' + #especialidade + '-' + #pageable")
    public Page<VeterinarioResponse> listarTodos(String nome, String especialidade, Pageable pageable) {
        return veterinarioRepository.buscarPorFiltros(nome, especialidade, pageable)
                .map(veterinarioMapper::veterinarioToResponse);
    }

    public VeterinarioResponse buscarPorId(UUID id) {
        return veterinarioRepository.findById(id)
                .map(veterinarioMapper::veterinarioToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado com ID: " + id));
    }

    @CacheEvict(value = "veterinarios", allEntries = true)
    public VeterinarioResponse salvar(VeterinarioRequest request) {
        Clinica clinica = clinicaRepository.findById(request.getClinicaId())
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + request.getClinicaId()));
        Veterinario veterinario = veterinarioMapper.toEntity(request);
        veterinario.setClinica(clinica);
        return veterinarioMapper.veterinarioToResponse(veterinarioRepository.save(veterinario));
    }

    @CacheEvict(value = "veterinarios", allEntries = true)
    public VeterinarioResponse atualizar(UUID id, VeterinarioRequest request) {
        Veterinario veterinario = veterinarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado com ID: " + id));
        Clinica clinica = clinicaRepository.findById(request.getClinicaId())
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + request.getClinicaId()));
        veterinarioMapper.atualizar(veterinario, request, clinica);
        return veterinarioMapper.veterinarioToResponse(veterinarioRepository.save(veterinario));
    }

    @CacheEvict(value = "veterinarios", allEntries = true)
    public void deletar(UUID id) {
        if (!veterinarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Veterinário não encontrado com ID: " + id);
        }
        veterinarioRepository.deleteById(id);
    }
}