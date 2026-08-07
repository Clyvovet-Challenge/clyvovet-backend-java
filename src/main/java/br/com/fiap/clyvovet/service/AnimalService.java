package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.mapper.AnimalMapper;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
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
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalMapper animalMapper;
    private final TutorRepository tutorRepository;
    private final SegurancaService seguranca;

    /**
     * A chave do cache inclui o tutor do usuario logado. Sem isso, a primeira
     * listagem de um tutor seria servida a qualquer outro que usasse os mesmos
     * filtros e paginacao — vazamento de dados entre contas.
     *
     * Usar #pageable inteiro em vez de pageNumber e pageSize traz o sort junto:
     * antes, ?sort=nome,asc e ?sort=nome,desc colidiam na mesma chave e a
     * segunda chamada recebia o resultado da primeira, na ordem errada.
     */
    @Cacheable(value = "animais",
            key = "#nome + '-' + #especie + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
    public Page<AnimalResponse> listarTodos(String nome, String especie, Pageable pageable) {
        return animalRepository.buscarPorFiltros(nome, especie, seguranca.tutorIdParaFiltro(), pageable)
                .map(animalMapper::animalToResponse);
    }

    public AnimalResponse buscarPorId(UUID id) {
        return animalRepository.findById(id)
                .map(animalMapper::animalToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Animal não encontrado com ID: " + id));
    }

    @CacheEvict(value = "animais", allEntries = true)
    public AnimalResponse salvar(AnimalRequest request) {
        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado com ID: " + request.getTutorId()));
        Animal animal = animalMapper.toEntity(request, tutor);
        return animalMapper.animalToResponse(animalRepository.save(animal));
    }

    @CacheEvict(value = "animais", allEntries = true)
    public AnimalResponse atualizar(UUID id, AnimalRequest request) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animal não encontrado com ID: " + id));
        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new EntityNotFoundException("Tutor não encontrado com ID: " + request.getTutorId()));
        animal.setNome(request.getNome());
        animal.setRaca(request.getRaca());
        animal.setEspecie(request.getEspecie());
        animal.setPorte(request.getPorte());
        animal.setCor(request.getCor());
        animal.setSexo(request.getSexo());
        animal.setDataNascimento(request.getDataNascimento());
        animal.setObservacao(request.getObservacao());
        animal.setTutor(tutor);
        return animalMapper.animalToResponse(animalRepository.save(animal));
    }

    @CacheEvict(value = "animais", allEntries = true)
    public void deletar(UUID id) {
        if (!animalRepository.existsById(id)) {
            throw new EntityNotFoundException("Animal não encontrado com ID: " + id);
        }
        animalRepository.deleteById(id);
    }
}