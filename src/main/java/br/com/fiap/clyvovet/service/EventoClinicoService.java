package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoPatchRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.mapper.EventoClinicoMapper;
import br.com.fiap.clyvovet.mapper.RelacionamentosDoEvento;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
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
public class EventoClinicoService {

    private final EventoClinicoRepository eventoClinicoRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AnimalRepository animalRepository;
    private final ClinicaRepository clinicaRepository;
    private final ServicoRepository servicoRepository;
    private final EventoClinicoMapper eventoClinicoMapper;
    private final SegurancaService seguranca;

    /** Ver a nota sobre a chave de cache em {@link AnimalService#listarTodos}. */
    @Cacheable(value = "eventos",
            key = "#tipoEvento + '-' + #animalNome + '-' + @seguranca.tutorIdParaFiltro() + '-' + #pageable")
    public Page<EventoClinicoResponse> listarTodos(TipoEvento tipoEvento, String animalNome, Pageable pageable) {
        return eventoClinicoRepository.buscarPorFiltros(tipoEvento, animalNome, seguranca.tutorIdParaFiltro(), pageable)
                .map(eventoClinicoMapper::toResponse);
    }

    public EventoClinicoResponse buscarPorId(UUID id) {
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.obterPorId(id));
    }

    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse criar(EventoClinicoRequest request) {
        EventoClinico evento = eventoClinicoMapper.toEntity(request, resolverRelacionamentos(request));
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse atualizar(UUID id, EventoClinicoRequest request) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);
        eventoClinicoMapper.atualizar(evento, request, resolverRelacionamentos(request));
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public EventoClinicoResponse atualizarParcialmente(UUID id, EventoClinicoPatchRequest patch) {
        EventoClinico evento = eventoClinicoRepository.obterPorId(id);
        eventoClinicoMapper.aplicarPatch(evento, patch, resolverRelacionamentos(patch));
        return eventoClinicoMapper.toResponse(eventoClinicoRepository.save(evento));
    }

    @Transactional
    @CacheEvict(value = "eventos", allEntries = true)
    public void deletar(UUID id) {
        eventoClinicoRepository.garantirQueExiste(id);
        eventoClinicoRepository.deleteById(id);
    }

    /**
     * Criar e atualizar precisam das mesmas tres entidades, com a mesma regra de
     * "existe ou 404". Estava escrito duas vezes, doze linhas cada.
     */
    private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoRequest request) {
        return new RelacionamentosDoEvento(
                veterinarioRepository.obterPorId(request.getVeterinarioId()),
                animalRepository.obterPorId(request.getAnimalId()),
                clinicaRepository.obterPorId(request.getClinicaId()),
                request.getServicoId() == null ? null : servicoRepository.obterPorId(request.getServicoId()));
    }

    /**
     * Mesma resolucao, mas para PATCH: cada id ausente vira null, e o mapper
     * entende null como "mantenha o vinculo atual". Buscar mesmo assim custaria
     * tres consultas a toa e transformaria um id omitido num 404.
     */
    private RelacionamentosDoEvento resolverRelacionamentos(EventoClinicoPatchRequest patch) {
        return new RelacionamentosDoEvento(
                patch.getVeterinarioId() == null ? null : veterinarioRepository.obterPorId(patch.getVeterinarioId()),
                patch.getAnimalId() == null ? null : animalRepository.obterPorId(patch.getAnimalId()),
                patch.getClinicaId() == null ? null : clinicaRepository.obterPorId(patch.getClinicaId()),
                patch.getServicoId() == null ? null : servicoRepository.obterPorId(patch.getServicoId()));
    }
}
