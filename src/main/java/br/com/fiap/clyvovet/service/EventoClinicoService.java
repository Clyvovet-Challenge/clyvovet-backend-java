package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.mapper.EventoClinicoMapper;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.*;
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
public class EventoClinicoService {

    private final EventoClinicoRepository eventoClinicoRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final VeterinarioRepository veterinarioRepository;
    private final AnimalRepository animalRepository;
    private final ClinicaRepository clinicaRepository;
    private final SegurancaService seguranca;

    /** Ver a nota sobre a chave de cache em {@link AnimalService#listarTodos}. */
    @Cacheable(value = "eventos",
            key = "#tipoEvento + '-' + #animalNome + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
    public Page<EventoClinicoResponse> listarTodos(TipoEvento tipoEvento, String animalNome, Pageable pageable) {
        return eventoClinicoRepository.buscarPorFiltros(tipoEvento, animalNome, seguranca.tutorIdParaFiltro(), pageable)
                .map(eventoClinicoMapper::toResponse);
    }

    public EventoClinicoResponse buscarPorId(UUID id) {
        return eventoClinicoRepository.findById(id)
                .map(eventoClinicoMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Evento clínico não encontrado com ID: " + id));
    }

    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse salvar(EventoClinicoRequest request) {
        Veterinario veterinario = veterinarioRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado com ID: " + request.getVeterinarioId()));
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new EntityNotFoundException("Animal não encontrado com ID: " + request.getAnimalId()));
        Clinica clinica = clinicaRepository.findById(request.getClinicaId())
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + request.getClinicaId()));
        return eventoClinicoMapper.toResponse(
                eventoClinicoRepository.save(
                        eventoClinicoMapper.toEntity(request, veterinario, animal, clinica)));
    }

    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse atualizar(UUID id, EventoClinicoRequest request) {
        EventoClinico evento = eventoClinicoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento clínico não encontrado com ID: " + id));
        Veterinario veterinario = veterinarioRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new EntityNotFoundException("Veterinário não encontrado com ID: " + request.getVeterinarioId()));
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new EntityNotFoundException("Animal não encontrado com ID: " + request.getAnimalId()));
        Clinica clinica = clinicaRepository.findById(request.getClinicaId())
                .orElseThrow(() -> new EntityNotFoundException("Clínica não encontrada com ID: " + request.getClinicaId()));
        eventoClinicoMapper.atualizar(evento, request, veterinario, animal, clinica);
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @CacheEvict(value = "eventos", allEntries = true)
    public void deletar(UUID id) {
        if (!eventoClinicoRepository.existsById(id)) {
            throw new EntityNotFoundException("Evento clínico não encontrado com ID: " + id);
        }
        eventoClinicoRepository.deleteById(id);
    }
}