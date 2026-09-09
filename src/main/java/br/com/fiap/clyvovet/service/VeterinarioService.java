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
    /**
     * O filtro por clinica e o que faz existir "a minha equipe".
     *
     * <p>O administrador da clinica gerencia os profissionais da casa, e a listagem
     * so sabia filtrar por nome e especialidade — para montar a equipe dele, a tela
     * teria de puxar a plataforma inteira e filtrar no cliente, paginacao e tudo.</p>
     *
     * <p>Nao e recorte de seguranca: o cadastro de veterinario e publico a quem esta
     * autenticado, e precisa ser — e por ele que o tutor escolhe com quem marcar.</p>
     */
    @Cacheable(value = "veterinarios",
            key = "#nome + '-' + #especialidade + '-' + #clinicaId + '-' + #pageable")
    public Page<VeterinarioResponse> listarTodos(String nome, String especialidade,
                                                 UUID clinicaId, Pageable pageable) {
        return veterinarioRepository.buscarPorFiltros(nome, especialidade, clinicaId, pageable)
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
