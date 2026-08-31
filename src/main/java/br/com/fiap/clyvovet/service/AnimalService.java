package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.animal.AnimalPatchRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.mapper.AnimalMapper;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso de animal. Orquestra repositorio, mapeamento e cache — a copia
 * campo a campo entre DTO e entidade fica no {@link AnimalMapper}, e a decisao
 * de "quem enxerga o que" no {@link SegurancaService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final TutorRepository tutorRepository;
    private final AnimalMapper animalMapper;
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
                .map(animalMapper::toResponse);
    }

    public AnimalResponse buscarPorId(UUID id) {
        return animalMapper.toResponse(animalRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = {"animais", "eventos", "pagamentos"}, allEntries = true)
    public AnimalResponse criar(AnimalRequest request) {
        Animal animal = animalMapper.toEntity(request, tutorRepository.obterPorId(request.getTutorId()));
        return animalMapper.toResponse(animalRepository.save(animal));
    }

    @Transactional
    @CacheEvict(value = {"animais", "eventos", "pagamentos"}, allEntries = true)
    public AnimalResponse atualizar(UUID id, AnimalRequest request) {
        Animal animal = animalRepository.obterPorId(id);
        animalMapper.atualizar(animal, request, tutorRepository.obterPorId(request.getTutorId()));
        return animalMapper.toResponse(animalRepository.save(animal));
    }

    @Transactional
    @CacheEvict(value = {"animais", "eventos", "pagamentos"}, allEntries = true)
    public AnimalResponse atualizarParcialmente(UUID id, AnimalPatchRequest patch) {
        Animal animal = animalRepository.obterPorId(id);
        // O tutor so e buscado quando o patch pede troca de dono; null diz ao
        // mapper para deixar o vinculo como esta.
        Tutor tutor = patch.getTutorId() == null ? null : tutorRepository.obterPorId(patch.getTutorId());
        animalMapper.aplicarPatch(animal, patch, tutor);
        return animalMapper.toResponse(animalRepository.save(animal));
    }

    @Transactional
    @CacheEvict(value = {"animais", "eventos", "pagamentos"}, allEntries = true)
    public void deletar(UUID id) {
        animalRepository.garantirQueExiste(id);
        animalRepository.deleteById(id);
    }
}
