package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.documento.DocumentoResponse;
import br.com.fiap.clyvovet.service.DocumentoClinicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Os anexos do prontuario — o PDF do laudo, a foto do raio-X.
 *
 * <h2>Por que as rotas moram sob /animais/{animalId}</h2>
 *
 * <p>Nao e estetica de REST: e o que faz a autorizacao continuar valendo sem uma
 * linha nova no {@code SecurityConfig}. A regra {@code /animais/**} ja exige
 * autenticacao, e o {@code animalId} no caminho da ao {@code @PreAuthorize} o
 * argumento de que ele precisa para decidir <b>antes</b> de o metodo rodar.</p>
 *
 * <p>Uma rota {@code /documentos/{id}} seria mais curta e teria o defeito de so
 * poder decidir depois de carregar o documento — e de convidar a tratar o id do
 * arquivo como se ele fosse a credencial.</p>
 *
 * <h2>O interruptor</h2>
 *
 * <p>{@code clyvovet.documentos.habilitado=false} apaga este controller do
 * contexto: as quatro rotas passam a responder 404 e o resto da API nao muda de
 * comportamento. E o plano de aborto da feature — ela e nova, guarda binario no
 * banco, e se der problema em producao o desligamento nao deveria depender de
 * um {@code git revert} as pressas. O app tem o interruptor equivalente do lado
 * dele ({@code EXPO_PUBLIC_ANEXOS}).</p>
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "clyvovet.documentos.habilitado", matchIfMissing = true)
@Tag(name = "Documentos do prontuário",
     description = "Anexos clínicos: envio, listagem e download com a mesma autorização do histórico")
public class DocumentoClinicoController {

    private final DocumentoClinicoService documentoService;

    @GetMapping("/animais/{animalId}/documentos")
    @Operation(summary = "Documentos anexados ao prontuário do animal (sem o arquivo)")
    public ResponseEntity<List<DocumentoResponse>> listar(@PathVariable UUID animalId) {
        return ResponseEntity.ok(documentoService.listar(animalId));
    }

    /**
     * Multipart, e nao JSON com base64.
     *
     * <p>Base64 infla o corpo em um terco e obriga a carregar a string inteira na
     * memoria antes de decodificar — 8 MB de PDF viram 11 MB de texto no heap. O
     * multipart e o formato que o {@code fetch} do React Native monta nativamente
     * com {@code FormData}.</p>
     */
    @PostMapping(path = "/animais/{animalId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@seguranca.podeAcessarAnimal(#animalId)")
    @Operation(summary = "Anexa um PDF, JPG ou PNG ao prontuário (exige acesso completo ao animal)")
    public ResponseEntity<DocumentoResponse> enviar(
            @PathVariable UUID animalId,
            @RequestPart("arquivo") MultipartFile arquivo,
            // Sem `required = true` de proposito: a ausencia cai na validacao do
            // service, que responde com a frase que o usuario precisa ler em vez
            // do texto generico de parametro ausente.
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) UUID eventoId) {

        DocumentoResponse criado = documentoService.enviar(animalId, eventoId, titulo, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /**
     * O arquivo.
     *
     * <p>{@code no-store} porque um laudo nao deve ficar em cache de proxy nem no
     * disco do navegador depois que a sessao termina. {@code attachment} porque
     * a resposta e para ser salva e aberta pelo visualizador do sistema, e nao
     * renderizada dentro de um contexto que possa executar algo.</p>
     */
    @GetMapping("/animais/{animalId}/documentos/{documentoId}/arquivo")
    @Operation(summary = "Baixa o arquivo — registra o acesso na auditoria do prontuário")
    public ResponseEntity<byte[]> baixar(
            @PathVariable UUID animalId, @PathVariable UUID documentoId) {

        DocumentoClinicoService.Arquivo arquivo = documentoService.baixar(animalId, documentoId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8)
                        .build().toString())
                .cacheControl(CacheControl.noStore())
                .body(arquivo.conteudo());
    }

    @DeleteMapping("/animais/{animalId}/documentos/{documentoId}")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#animalId)")
    @Operation(summary = "Remove um documento — somente quem o enviou, ou o ADMIN da plataforma")
    public ResponseEntity<Void> remover(
            @PathVariable UUID animalId, @PathVariable UUID documentoId) {

        documentoService.remover(animalId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
