package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.mapper.TutorMapper;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.TutorRepository;
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
public class TutorService {

    private final TutorRepository tutorRepository;
    private final TutorMapper tutorMapper;

    // #pageable inclui o sort na chave; com pageNumber e pageSize apenas,
    // ?sort=nome,asc e ?sort=nome,desc colidiam e devolviam a ordem errada.
    @Cacheable(value = "tutores", key = "#nome + '-' + #cidade + '-' + #pageable")
    public Page<TutorResponse> listarTodos(String nome, String cidade, Pageable pageable) {
        return tutorRepository.buscarPorFiltros(nome, cidade, pageable)
                .map(tutorMapper::toResponse);
    }

    public TutorResponse buscarPorId(UUID id) {
        return tutorMapper.toResponse(tutorRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = "tutores", allEntries = true)
    public TutorResponse criar(TutorRequest request) {
        return tutorMapper.toResponse(tutorRepository.save(tutorMapper.toEntity(request)));
    }

    @Transactional
    @CacheEvict(value = "tutores", allEntries = true)
    public TutorResponse atualizar(UUID id, TutorRequest request) {
        Tutor tutor = tutorRepository.obterPorId(id);
        tutorMapper.atualizar(tutor, request);
        return tutorMapper.toResponse(tutorRepository.save(tutor));
    }

    @Transactional
    @CacheEvict(value = "tutores", allEntries = true)
    public void deletar(UUID id) {
        tutorRepository.garantirQueExiste(id);
        tutorRepository.deleteById(id);
    }
}
