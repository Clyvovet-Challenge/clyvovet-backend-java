package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.servico.ServicoRequest;
import br.com.fiap.clyvovet.dto.servico.ServicoResponse;
import br.com.fiap.clyvovet.mapper.ServicoMapper;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final SegurancaService seguranca;

    /**
     * O catalogo de uma clinica.
     *
     * <p>Por padrao so os ativos: e o que o tutor precisa ver para agendar, e um
     * servico desativado na lista de agendamento e um horario que ninguem vai
     * atender.</p>
     *
     * <p>{@code incluirInativos} e a visao de quem administra, e por isso e
     * verificado aqui e nao numa anotacao — a permissao depende de QUAL clinica o
     * parametro pede, e a decisao so existe quando ele vem marcado.</p>
     */
    public List<ServicoResponse> daClinica(UUID clinicaId, boolean incluirInativos) {
        clinicaRepository.garantirQueExiste(clinicaId);
        if (incluirInativos && !seguranca.podeGerirCatalogoDe(clinicaId)) {
            throw new AccessDeniedException(
                    "O catálogo completo, com os serviços desativados, é de quem administra a clínica");
        }
        List<Servico> servicos = incluirInativos
                ? servicoRepository.daClinica(clinicaId)
                : servicoRepository.ativosDaClinica(clinicaId);
        return servicos.stream().map(servicoMapper::toResponse).toList();
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

    /**
     * O caminho de volta, que nao existia.
     *
     * <p>Desativar era irreversivel por acidente: {@code ServicoRequest} nao tem o
     * campo {@code ativo}, entao nem o PUT trazia o servico de volta, e a listagem
     * escondia o desativado — a clinica que parasse de oferecer banho e voltasse
     * atras precisava cadastrar outro servico, com outro id, e o historico de preco
     * ficava partido em dois.</p>
     *
     * <p>E acao, e nao campo do PUT, pelo mesmo motivo que concluir atendimento nao
     * e {@code {"statusEvento":"REALIZADO"}}: a transicao tem nome proprio.</p>
     */
    @Transactional
    public ServicoResponse reativar(UUID id) {
        Servico servico = servicoRepository.obterPorId(id);
        servico.setAtivo(true);
        return servicoMapper.toResponse(servicoRepository.save(servico));
    }
}
