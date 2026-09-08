package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.solicitacao.CampoAlterado;
import br.com.fiap.clyvovet.dto.solicitacao.SolicitacaoAlteracaoRequest;
import br.com.fiap.clyvovet.dto.solicitacao.SolicitacaoAlteracaoResponse;
import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.SolicitacaoAlteracao;
import br.com.fiap.clyvovet.model.StatusSolicitacao;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.SolicitacaoAlteracaoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O fluxo "o veterinário pede, o tutor aprova".
 *
 * <p>Fecha a regra do produto que dizia que o veterinário só altera os dados do
 * animal <b>com autorização do dono</b>. Antes desta classe, ele alterava sem pedir
 * nada — verificado contra a pilha local, um veterinário sem nenhuma autorização
 * dava PATCH e DELETE em animal de qualquer tutor. A escrita foi fechada, e este
 * serviço é o caminho de volta.</p>
 *
 * <p>Aprovar <b>é</b> aplicar, na mesma transação. Não existe estado entre "o tutor
 * disse sim" e "o dado mudou": um intermediário só representaria falha, e falha
 * aqui desfaz tudo.</p>
 */
@Service
@RequiredArgsConstructor
public class SolicitacaoAlteracaoService {

    private final SolicitacaoAlteracaoRepository repository;
    private final AnimalRepository animalRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final SegurancaService seguranca;

    // ================================================================
    // O veterinário pede
    // ================================================================

    @Transactional
    public SolicitacaoAlteracaoResponse solicitar(UUID animalId, SolicitacaoAlteracaoRequest request) {
        Animal animal = animalRepository.obterPorId(animalId, Recurso.ANIMAL);

        UUID veterinarioId = seguranca.autenticadoOuNulo() == null
                ? null : seguranca.autenticadoOuNulo().getVeterinarioId();
        if (veterinarioId == null) {
            // ADMIN da plataforma não passa por aqui: ele edita o cadastro
            // diretamente. Quem precisa pedir é quem não pode escrever.
            throw new RegraDeNegocioException("veterinario",
                    "Apenas um veterinário vinculado pode solicitar alteração de cadastro.");
        }
        Veterinario veterinario = veterinarioRepository.obterPorId(veterinarioId, Recurso.VETERINARIO);

        if (repository.existsByAnimalIdAndVeterinarioIdAndStatus(
                animalId, veterinarioId, StatusSolicitacao.PENDENTE)) {
            // Sem isto, um duplo clique enche a caixa do tutor com pedidos
            // idênticos, e ele teria que responder um por um.
            throw new RegraDeNegocioException("solicitacao",
                    "Já existe um pedido seu pendente para este animal. "
                            + "Aguarde a resposta do tutor ou peça para ele recusar o anterior.");
        }

        SolicitacaoAlteracao solicitacao = montar(request, animal, veterinario);

        if (solicitacao.semNenhumCampo()) {
            throw new RegraDeNegocioException("solicitacao",
                    "Informe ao menos um campo a alterar.");
        }
        if (nadaMudaria(solicitacao, animal)) {
            // Um pedido cujos valores já são os atuais gasta a atenção do tutor
            // para nada, e uma aprovação dele não mudaria uma linha.
            throw new RegraDeNegocioException("solicitacao",
                    "Os valores enviados são iguais aos que o animal já tem.");
        }

        return paraResposta(repository.save(solicitacao), animal);
    }

    // ================================================================
    // O tutor responde
    // ================================================================

    @Transactional
    public SolicitacaoAlteracaoResponse aprovar(UUID solicitacaoId) {
        SolicitacaoAlteracao solicitacao = pendente(solicitacaoId);
        Animal animal = solicitacao.getAnimal();

        solicitacao.aplicarEm(animal);
        animalRepository.save(animal);

        solicitacao.setStatus(StatusSolicitacao.APROVADA);
        solicitacao.setRespondidoEm(LocalDateTime.now());
        solicitacao.setRespondidoPor(seguranca.usuarioAutenticadoId());

        // O antes/depois é calculado ANTES de salvar a solicitação, mas o animal
        // já mudou — então a resposta mostra o estado novo nos dois lados. É o
        // correto aqui: o pedido acabou de ser aplicado, e "atual" é o novo valor.
        return paraResposta(repository.save(solicitacao), animal);
    }

    @Transactional
    public SolicitacaoAlteracaoResponse recusar(UUID solicitacaoId, String motivo) {
        SolicitacaoAlteracao solicitacao = pendente(solicitacaoId);

        solicitacao.setStatus(StatusSolicitacao.RECUSADA);
        solicitacao.setMotivoRecusa(motivo);
        solicitacao.setRespondidoEm(LocalDateTime.now());
        solicitacao.setRespondidoPor(seguranca.usuarioAutenticadoId());

        return paraResposta(repository.save(solicitacao), solicitacao.getAnimal());
    }

    private SolicitacaoAlteracao pendente(UUID id) {
        SolicitacaoAlteracao solicitacao =
                repository.obterPorId(id, Recurso.SOLICITACAO_ALTERACAO);
        if (!solicitacao.estaPendente()) {
            throw new RegraDeNegocioException("status",
                    "Este pedido já foi respondido (" + solicitacao.getStatus() + ").");
        }
        return solicitacao;
    }

    /**
     * O animal de um pedido, para o {@code @PreAuthorize} do controller poder
     * perguntar "quem manda neste animal?".
     *
     * <p>É uma leitura antes da autorização, e é o preço de autorizar por DONO em
     * vez de por perfil: o id que vem na URL é o do pedido, e o dono está a uma
     * relação de distância.</p>
     */
    @Transactional(readOnly = true)
    public UUID doAnimalDe(UUID solicitacaoId) {
        return repository.obterPorId(solicitacaoId, Recurso.SOLICITACAO_ALTERACAO)
                .getAnimal().getId();
    }

    // ================================================================
    // Consultas
    // ================================================================

    /** A caixa de pedidos do tutor: o que espera resposta dele, em qualquer pet. */
    @Transactional(readOnly = true)
    public List<SolicitacaoAlteracaoResponse> pendentesDoTutorLogado() {
        UUID tutorId = seguranca.tutorIdParaFiltro();
        if (tutorId == null) {
            return List.of();
        }
        return repository
                .findByAnimalTutorIdAndStatusOrderByCriadoEmDesc(tutorId, StatusSolicitacao.PENDENTE)
                .stream().map(s -> paraResposta(s, s.getAnimal())).toList();
    }

    /** Todo o histórico do tutor, respondido ou não. */
    @Transactional(readOnly = true)
    public List<SolicitacaoAlteracaoResponse> historicoDoTutorLogado() {
        UUID tutorId = seguranca.tutorIdParaFiltro();
        if (tutorId == null) {
            return List.of();
        }
        return repository.findByAnimalTutorIdOrderByCriadoEmDesc(tutorId)
                .stream().map(s -> paraResposta(s, s.getAnimal())).toList();
    }

    /** O histórico de um animal: quem pediu o quê, e como terminou. */
    @Transactional(readOnly = true)
    public List<SolicitacaoAlteracaoResponse> doAnimal(UUID animalId) {
        animalRepository.garantirQueExiste(animalId, Recurso.ANIMAL);
        return repository.findByAnimalIdOrderByCriadoEmDesc(animalId)
                .stream().map(s -> paraResposta(s, s.getAnimal())).toList();
    }

    /** O outro lado: o que o veterinário logado pediu e ainda não foi respondido. */
    @Transactional(readOnly = true)
    public List<SolicitacaoAlteracaoResponse> meusPedidosPendentes() {
        var usuario = seguranca.autenticadoOuNulo();
        UUID veterinarioId = usuario == null ? null : usuario.getVeterinarioId();
        if (veterinarioId == null) {
            return List.of();
        }
        return repository
                .findByVeterinarioIdAndStatusOrderByCriadoEmDesc(veterinarioId, StatusSolicitacao.PENDENTE)
                .stream().map(s -> paraResposta(s, s.getAnimal())).toList();
    }

    // ================================================================
    // Montagem
    // ================================================================

    private SolicitacaoAlteracao montar(SolicitacaoAlteracaoRequest r, Animal animal, Veterinario vet) {
        SolicitacaoAlteracao s = new SolicitacaoAlteracao();
        s.setAnimal(animal);
        s.setVeterinario(vet);
        s.setJustificativa(r.getJustificativa());
        s.setNome(r.getNome());
        s.setRaca(r.getRaca());
        s.setEspecie(r.getEspecie());
        s.setPorte(r.getPorte());
        s.setCor(r.getCor());
        s.setSexo(r.getSexo());
        s.setDataNascimento(r.getDataNascimento());
        s.setMicrochip(r.getMicrochip());
        s.setCastrado(r.getCastrado());
        s.setObservacao(r.getObservacao());
        return s;
    }

    private boolean nadaMudaria(SolicitacaoAlteracao s, Animal a) {
        return campos(s, a).isEmpty();
    }

    /**
     * O antes e o depois de cada campo do pedido.
     *
     * <p>Campos cujo valor proposto é igual ao atual ficam de fora: o tutor precisa
     * ver o que <i>muda</i>, e uma lista cheia de linhas idênticas esconde a única
     * que importa.</p>
     */
    private List<CampoAlterado> campos(SolicitacaoAlteracao s, Animal a) {
        List<CampoAlterado> lista = new ArrayList<>();
        comparar(lista, "nome", a.getNome(), s.getNome());
        comparar(lista, "raça", a.getRaca(), s.getRaca());
        comparar(lista, "espécie", a.getEspecie(), s.getEspecie());
        comparar(lista, "porte", a.getPorte(), s.getPorte());
        comparar(lista, "cor", a.getCor(), s.getCor());
        comparar(lista, "sexo", texto(a.getSexo()), texto(s.getSexo()));
        comparar(lista, "data de nascimento", texto(a.getDataNascimento()), texto(s.getDataNascimento()));
        comparar(lista, "microchip", a.getMicrochip(), s.getMicrochip());
        comparar(lista, "castrado", texto(a.getCastrado()), texto(s.getCastrado()));
        comparar(lista, "observação", a.getObservacao(), s.getObservacao());
        return lista;
    }

    private void comparar(List<CampoAlterado> destino, String campo, String atual, String proposto) {
        if (proposto != null && !Objects.equals(atual, proposto)) {
            destino.add(new CampoAlterado(campo, atual, proposto));
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private SolicitacaoAlteracaoResponse paraResposta(SolicitacaoAlteracao s, Animal animal) {
        Veterinario vet = s.getVeterinario();
        return new SolicitacaoAlteracaoResponse(
                s.getId(),
                animal.getId(),
                animal.getNome(),
                vet.getId(),
                vet.getNome(),
                vet.getClinica() == null ? null : vet.getClinica().getNome(),
                s.getStatus(),
                s.getJustificativa(),
                campos(s, animal),
                s.getCriadoEm(),
                s.getRespondidoEm(),
                s.getMotivoRecusa());
    }
}
