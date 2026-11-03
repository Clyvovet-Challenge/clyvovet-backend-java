package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.documento.DocumentoResponse;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.DocumentoClinico;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.NivelAcesso;
import br.com.fiap.clyvovet.repository.AcessoHistoricoRepository;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.DocumentoClinicoRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.repository.projecao.DocumentoResumo;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.fiap.clyvovet.service.ValidadorDeArquivo.bytesDe;
import static br.com.fiap.clyvovet.service.ValidadorDeArquivo.nomeSeguro;
import static br.com.fiap.clyvovet.service.ValidadorDeArquivo.sha256De;
import static br.com.fiap.clyvovet.service.ValidadorDeArquivo.tipoRealDe;
import static br.com.fiap.clyvovet.service.ValidadorDeArquivo.tituloValido;

/**
 * Os arquivos do prontuario: enviar, listar, baixar, remover.
 *
 * <h2>A regra de acesso e a MESMA do historico, e isso e a razao do desenho</h2>
 *
 * <p>O laudo em PDF nao e um dado novo: e o historico clinico em outro formato.
 * Entao ele nao ganha politica propria — {@code garantirLeitura} chama o
 * {@code nivelSobre} e o {@code registrarAcesso} do proprio
 * {@code HistoricoService}. Duas consequencias que valem por si:</p>
 *
 * <ul>
 *   <li>Baixar um exame <b>aparece na auditoria</b> que o tutor consulta em
 *       {@code GET /animais/{id}/acessos}. Nao existe porta por onde ler o
 *       prontuario sem deixar rastro.</li>
 *   <li>Revogar o consentimento de uma clinica fecha o arquivo no mesmo instante
 *       em que fecha o texto — sem uma segunda revogacao para lembrar.</li>
 * </ul>
 *
 * <h2>Ler exige COMPLETO; o resumo de seguranca nao alcanca o arquivo</h2>
 *
 * <p>O nivel 1 existe para salvar a vida do animal na mesa: alergia, condicao
 * cronica, vacina. Ele e deliberadamente <b>curto</b>. Um PDF nao tem versao
 * curta — o laudo do hemograma vem inteiro ou nao vem. Entregar o arquivo a quem
 * so alcanca o resumo seria contornar o proprio nivel 1, anexando o prontuario
 * inteiro a ele.</p>
 *
 * <p>O veterinario sem consentimento tem o caminho que ja existia: a quebra de
 * vidro. Ela pede motivo, avisa o tutor e fica registrada — e por isso
 * <b>libera o arquivo pelo resto do dia</b>, sem inventar um segundo mecanismo
 * de excecao. Ver {@code quebrouOVidroHoje}.</p>
 *
 * <h2>Escrever NAO tem quebra de vidro</h2>
 *
 * <p>A emergencia justifica LER sem consentimento; nao justifica gravar no
 * prontuario de um animal que a clinica nao foi autorizada a atender. Enviar
 * exige COMPLETO, sempre.</p>
 *
 * <h2>Por que as recusas de upload respondem 409</h2>
 *
 * <p>Tipo nao aceito, arquivo grande e duplicata sao rejeicoes de regra, e o
 * {@code GlobalExceptionHandler} mapeia {@code RegraDeNegocioException} para
 * 409. O 400 do projeto nasce do Bean Validation sobre {@code @RequestBody}, e
 * aqui nao ha corpo JSON: a requisicao e multipart. Preferiu-se o 409 com
 * mensagem exata a fabricar um 400 por outro caminho — o app le a mensagem nos
 * dois casos (ver {@code http.ts}), e o status continua honesto: o pedido
 * conflita com uma regra, nao esta malformado.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoClinicoService {

    /**
     * Teto por animal. Com 8 MB por arquivo, 30 documentos sao 240 MB no pior
     * caso — e o prontuario de um pet nao tem trinta laudos sem que a feature
     * esteja sendo usada como armazenamento de fotos.
     */
    static final int MAXIMO_POR_ANIMAL = 30;

    private final DocumentoClinicoRepository documentoRepository;
    private final AnimalRepository animalRepository;
    private final EventoClinicoRepository eventoRepository;
    private final ClinicaRepository clinicaRepository;
    private final AcessoHistoricoRepository acessoRepository;
    private final HistoricoService historicoService;
    private final SegurancaService seguranca;

    /** O arquivo pronto para virar resposta HTTP. */
    public record Arquivo(byte[] conteudo, String nomeArquivo, String tipoConteudo) {
    }

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    @Transactional
    public List<DocumentoResponse> listar(UUID animalId) {
        Animal animal = animalRepository.obterPorId(animalId);
        garantirLeitura(animal);

        return documentoRepository.resumosDoAnimal(animalId).stream()
                .map(DocumentoClinicoService::paraResposta)
                .toList();
    }

    /**
     * O arquivo em si.
     *
     * <p>Passa pela MESMA {@code garantirLeitura} da listagem — e nao por um
     * "ja listou, entao pode baixar". O id do documento e um UUID que o cliente
     * conhece, e uma rota de download que confiasse nele seria uma URL que
     * entrega laudo a quem tiver o link.</p>
     *
     * <p>O documento tambem precisa pertencer AO ANIMAL do caminho: sem isso,
     * {@code /animais/{meu-pet}/documentos/{id-de-outro}/arquivo} passaria pela
     * autorizacao do meu pet e entregaria o arquivo do animal alheio.</p>
     */
    @Transactional
    public Arquivo baixar(UUID animalId, UUID documentoId) {
        Animal animal = animalRepository.obterPorId(animalId);
        garantirLeitura(animal);

        DocumentoClinico documento = documentoRepository.obterPorId(documentoId);
        garantirQueEDesteAnimal(documento, animalId);

        return new Arquivo(documento.getConteudo(), documento.getNomeArquivo(), documento.getTipoConteudo());
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DocumentoResponse enviar(UUID animalId, UUID eventoId, String titulo, MultipartFile arquivo) {
        // Com trava: o mesmo arquivo enviado duas vezes ao mesmo tempo passava
        // pelas duas conferencias (teto e hash repetido) e entrava em dobro.
        Animal animal = animalRepository.travarPorId(animalId);
        garantirEscrita(animal);

        String tituloLimpo = tituloValido(titulo);
        byte[] conteudo = bytesDe(arquivo);
        String tipo = tipoRealDe(conteudo);
        String hash = sha256De(conteudo);

        if (documentoRepository.countByAnimalId(animalId) >= MAXIMO_POR_ANIMAL) {
            throw new RegraDeNegocioException("arquivo",
                    "Este pet ja tem " + MAXIMO_POR_ANIMAL
                            + " documentos. Remova algum antes de enviar outro.");
        }
        if (documentoRepository.existsByAnimalIdAndSha256(animalId, hash)) {
            throw new RegraDeNegocioException("arquivo",
                    "Este arquivo ja esta no prontuario deste pet.");
        }

        UsuarioAutenticado usuario = seguranca.autenticadoOuNulo();

        DocumentoClinico documento = new DocumentoClinico();
        documento.setAnimal(animal);
        documento.setEnviadoPor(usuario.getUsuario());
        documento.setClinica(clinicaDoRemetente());
        documento.setEvento(eventoId != null ? eventoDoAnimal(eventoId, animalId) : null);
        documento.setTitulo(tituloLimpo);
        documento.setNomeArquivo(nomeSeguro(arquivo.getOriginalFilename(), tipo));
        documento.setTipoConteudo(tipo);
        documento.setTamanhoBytes((long) conteudo.length);
        documento.setSha256(hash);
        documento.setConteudo(conteudo);

        DocumentoClinico salvo = documentoRepository.save(documento);
        log.info("Documento {} ({} bytes, {}) anexado ao animal {} por {}",
                salvo.getId(), conteudo.length, tipo, animalId, usuario.getUsuario().getEmail());

        return paraResposta(salvo);
    }

    /**
     * Remove um documento — e so quem o enviou pode.
     *
     * <p>Nao e o dono do prontuario, e nao e a clinica: e o REMETENTE. A escolha
     * evita as duas assimetrias ruins. Se o tutor pudesse apagar o laudo que a
     * clinica anexou, a clinica perderia o proprio registro de atendimento
     * dentro do sistema em que o registrou. Se a clinica pudesse apagar o que o
     * tutor trouxe de fora, o prontuario deixaria de ser do tutor exatamente no
     * unico ponto em que ele o alimenta.</p>
     *
     * <p>O ADMIN da plataforma passa porque precisa responder a pedido de remocao
     * de dado — e a LGPD nao aceita "quem enviou nao trabalha mais aqui".</p>
     */
    @Transactional
    public void remover(UUID animalId, UUID documentoId) {
        animalRepository.garantirQueExiste(animalId);
        DocumentoClinico documento = documentoRepository.obterPorId(documentoId);
        garantirQueEDesteAnimal(documento, animalId);

        UUID meuId = seguranca.usuarioAutenticadoId();
        boolean fuiEuQuemEnviou = meuId != null && meuId.equals(documento.getEnviadoPor().getId());

        if (!fuiEuQuemEnviou && !seguranca.ehAdministradorDaPlataforma()) {
            throw new RegraDeNegocioException("documentoId",
                    "Somente quem enviou o documento pode remove-lo.");
        }
        documentoRepository.delete(documento);
        log.info("Documento {} do animal {} removido por {}", documentoId, animalId, meuId);
    }

    // ------------------------------------------------------------------
    // Autorizacao
    // ------------------------------------------------------------------

    private void garantirLeitura(Animal animal) {
        NivelAcesso nivel = historicoService.nivelSobre(animal);

        if (nivel != NivelAcesso.COMPLETO && !quebrouOVidroHoje(animal)) {
            throw new RegraDeNegocioException("animalId",
                    "Sem acesso aos documentos deste animal");
        }
        // Registrado como COMPLETO porque foi o que a pessoa recebeu: o arquivo
        // inteiro. Gravar o nivel resolvido diria "leu o resumo" sobre quem
        // acabou de baixar o laudo.
        historicoService.registrarAcesso(animal, NivelAcesso.COMPLETO, false, null);
    }

    private void garantirEscrita(Animal animal) {
        if (historicoService.nivelSobre(animal) != NivelAcesso.COMPLETO) {
            throw new RegraDeNegocioException("animalId",
                    "Sem permissao para anexar documentos a este animal");
        }
    }

    /**
     * O veterinario abriu a quebra de vidro para este animal hoje?
     *
     * <p>Reusa a linha de auditoria que {@code acessoEmergencial} ja grava, em
     * vez de criar um token ou uma tabela de liberacao temporaria. O efeito
     * colateral e a propriedade que se queria: a liberacao do arquivo <b>e</b> o
     * registro dela — nao existe uma sem a outra. E ela expira sozinha a
     * meia-noite, porque a chave da linha inclui o dia.</p>
     */
    private boolean quebrouOVidroHoje(Animal animal) {
        UUID usuarioId = seguranca.usuarioAutenticadoId();
        if (usuarioId == null) {
            return false;
        }
        return acessoRepository.findByAnimalIdAndUsuarioIdAndDiaAndEmergencial(
                animal.getId(), usuarioId, LocalDate.now(), true).isPresent();
    }

    private void garantirQueEDesteAnimal(DocumentoClinico documento, UUID animalId) {
        if (!documento.getAnimal().getId().equals(animalId)) {
            throw new RegraDeNegocioException("documentoId",
                    "Este documento nao pertence ao animal informado");
        }
    }

    // ------------------------------------------------------------------
    // Montagem
    // ------------------------------------------------------------------

    /**
     * A clinica de quem esta enviando, ou nulo quando e o tutor.
     *
     * <p>Usa {@code clinicaDoUsuario} e nao {@code veterinario.getClinica()}: o
     * ADMIN_CLINICA nao tem registro de veterinario, e o documento que ele anexa
     * (o laudo de um laboratorio parceiro, por exemplo) e da clinica dele — nao
     * de ninguem.</p>
     */
    private Clinica clinicaDoRemetente() {
        UUID clinicaId = seguranca.clinicaDoUsuario();
        return clinicaId == null ? null : clinicaRepository.findById(clinicaId).orElse(null);
    }

    private EventoClinico eventoDoAnimal(UUID eventoId, UUID animalId) {
        EventoClinico evento = eventoRepository.obterPorId(eventoId);
        if (evento.getAnimal() == null || !evento.getAnimal().getId().equals(animalId)) {
            throw new RegraDeNegocioException("eventoId", "Este atendimento nao e deste animal");
        }
        return evento;
    }

    private static DocumentoResponse paraResposta(DocumentoResumo r) {
        return new DocumentoResponse(
                r.id(), r.titulo(), r.nomeArquivo(), r.tipoConteudo(), r.tamanhoBytes(),
                r.enviadoEm(), r.enviadoPorEmail(), r.clinicaNome(), r.eventoId(),
                r.clinicaNome() == null ? "TUTOR" : "CLINICA");
    }

    private static DocumentoResponse paraResposta(DocumentoClinico d) {
        return new DocumentoResponse(
                d.getId(), d.getTitulo(), d.getNomeArquivo(), d.getTipoConteudo(), d.getTamanhoBytes(),
                d.getEnviadoEm(), d.getEnviadoPor().getEmail(),
                d.getClinica() != null ? d.getClinica().getNome() : null,
                d.getEvento() != null ? d.getEvento().getId() : null,
                d.getClinica() == null ? "TUTOR" : "CLINICA");
    }
}
