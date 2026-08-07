package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.mapper.EnderecoMapper;
import br.com.fiap.clyvovet.mapper.TutorMapper;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.TutorRepository;
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
public class TutorService {

    private final TutorRepository tutorRepository;
    private final TutorMapper tutorMapper;
    private final EnderecoMapper enderecoMapper;

    // #pageable inclui o sort na chave; com pageNumber e pageSize apenas,
    // ?sort=nome,asc e ?sort=nome,desc colidiam e devolviam a ordem errada.
    @Cacheable(value = "tutores", key = "#nome + '-' + #cidade + '-' + #pageable")
    public Page<TutorResponse> listarTodos(String nome, String cidade, Pageable pageable) {
        return tutorRepository.buscarPorFiltros(nome, cidade, pageable)
                .map(tutorMapper::tutorToResponse);
    }

    public TutorResponse buscarPorId(UUID id) {
        return tutorRepository.findById(id)
                .map(tutorMapper::tutorToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado com ID: " + id));
    }

    @CacheEvict(value = "tutores", allEntries = true)
    public TutorResponse salvar(TutorRequest tutorRequest) {
        Tutor tutor = tutorMapper.requestToTutor(tutorRequest);
        return tutorMapper.tutorToResponse(tutorRepository.save(tutor));
    }

    @CacheEvict(value = "tutores", allEntries = true)
    public TutorResponse atualizar(UUID id, TutorRequest tutorRequest) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado com ID: " + id));
        tutor.setNome(tutorRequest.getNome());
        tutor.setCpf(tutorRequest.getCpf());
        tutor.setEmail(tutorRequest.getEmail());
        tutor.setTelefone(tutorRequest.getTelefone());
        tutor.setSexo(tutorRequest.getSexo());
        tutor.setDataNascimento(tutorRequest.getDataNascimento());
        tutor.setEndereco(enderecoMapper.requestToEndereco(tutorRequest.getEndereco()));
        tutorRepository.save(tutor);
        return tutorMapper.tutorToResponse(tutor);
    }

    @CacheEvict(value = "tutores", allEntries = true)
    public void deletar(UUID id) {
        if (!tutorRepository.existsById(id)) {
            throw new EntityNotFoundException("Tutor não encontrado com ID: " + id);
        }
        tutorRepository.deleteById(id);
    }
}