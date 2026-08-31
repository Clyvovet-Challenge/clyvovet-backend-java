package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.servico.ServicoRequest;
import br.com.fiap.clyvovet.dto.servico.ServicoResponse;
import br.com.fiap.clyvovet.mapper.ServicoMapper;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** O catalogo da clinica: o que ela oferece, por quanto e em quanto tempo. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ClinicaRepository clinicaRepository;
    private final ServicoMapper servicoMapper;

    public List<ServicoResponse> daClinica(UUID clinicaId) {
        clinicaRepository.garantirQueExiste(clinicaId);
        return servicoRepository.ativosDaClinica(clinicaId).stream()
                .map(servicoMapper::toResponse)
                .toList();
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = servicoMapper.toEntity(
                request, clinicaRepository.obterPorId(request.getClinicaId()));
        return servicoMapper.toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(UUID id, ServicoRequest request) {
        Servico servico = servicoRepository.obterPorId(id);
        servicoMapper.atualizar(servico, request);
        return servicoMapper.toResponse(servicoRepository.save(servico));
    }

    /**
     * Desativa em vez de apagar.
     *
     * DELETE levaria junto o preco historico de tudo que ja foi cobrado por
     * este servico — e a FK evento_clinico.servico_id impediria a remocao de
     * qualquer forma, devolvendo 409 para uma operacao que o usuario entende
     * como legitima ("nao ofereco mais isso").
     */
    @Transactional
    public void desativar(UUID id) {
        Servico servico = servicoRepository.obterPorId(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
