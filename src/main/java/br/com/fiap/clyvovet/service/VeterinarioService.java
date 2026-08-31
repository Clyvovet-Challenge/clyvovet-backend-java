package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioPatchRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.mapper.VeterinarioMapper;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
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
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final VeterinarioMapper veterinarioMapper;

    // Ver a nota sobre #pageable na chave em TutorService.
    @Cacheable(value = "veterinarios", key = "#nome + '-' + #especialidade + '-' + #pageable")
    public Page<VeterinarioResponse> listarTodos(String nome, String especialidade, Pageable pageable) {
        return veterinarioRepository.buscarPorFiltros(nome, especialidade, pageable)
                .map(veterinarioMapper::toResponse);
    }

    public VeterinarioResponse buscarPorId(UUID id) {
        return veterinarioMapper.toResponse(veterinarioRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = {"veterinarios", "eventos"}, allEntries = true)
    public VeterinarioResponse criar(VeterinarioRequest request) {
        Veterinario veterinario = veterinarioMapper.toEntity(
                request, clinicaRepository.obterPorId(request.getClinicaId()));
        return veterinarioMapper.toResponse(veterinarioRepository.save(veterinario));
    }

    @Transactional
    @CacheEvict(value = {"veterinarios", "eventos"}, allEntries = true)
    public VeterinarioResponse atualizar(UUID id, VeterinarioRequest request) {
        Veterinario veterinario = veterinarioRepository.obterPorId(id);
        veterinarioMapper.atualizar(veterinario, request, clinicaRepository.obterPorId(request.getClinicaId()));
        return veterinarioMapper.toResponse(veterinarioRepository.save(veterinario));
    }

    @Transactional
    @CacheEvict(value = {"veterinarios", "eventos"}, allEntries = true)
    public VeterinarioResponse atualizarParcialmente(UUID id, VeterinarioPatchRequest patch) {
        Veterinario veterinario = veterinarioRepository.obterPorId(id);
        Clinica clinica = patch.getClinicaId() == null ? null : clinicaRepository.obterPorId(patch.getClinicaId());
        veterinarioMapper.aplicarPatch(veterinario, patch, clinica);
        return veterinarioMapper.toResponse(veterinarioRepository.save(veterinario));
    }

    @Transactional
    @CacheEvict(value = {"veterinarios", "eventos"}, allEntries = true)
    public void deletar(UUID id) {
        veterinarioRepository.garantirQueExiste(id);
        veterinarioRepository.deleteById(id);
    }
}
