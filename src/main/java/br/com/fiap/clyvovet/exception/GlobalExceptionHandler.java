package br.com.fiap.clyvovet.exception;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.fiap.clyvovet.config.CorrelacaoFilter;
import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Ponto unico de traducao de excecao para resposta HTTP.
 *
 * Concentrar isso aqui e o que mantem os controllers sem try/catch: cada um
 * cuida do caminho feliz, e o desvio vira status e corpo neste lugar so.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErroValidacao>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ErroValidacao> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> new ErroValidacao(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(erros);
    }

    /** Recurso inexistente na regra da aplicacao. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroValidacao> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return respostaDe(HttpStatus.NOT_FOUND, "id", ex.getMessage());
    }

    /**
     * Rede de seguranca para o que vier do proprio JPA (acesso a um proxy de
     * entidade ja removida, por exemplo). Sem isso, viraria 500.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErroValidacao> handleNotFound(EntityNotFoundException ex) {
        return respostaDe(HttpStatus.NOT_FOUND, "id", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroValidacao> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return respostaDe(HttpStatus.CONFLICT, ex.getCampo(), ex.getMessage());
    }

    /**
     * Unicidade de CPF, CNPJ, CRMV e e-mail existe apenas como constraint no
     * banco. Sem este handler, uma duplicata sobe como 500 carregando o SQL e
     * o nome da constraint na resposta — o que expoe a estrutura interna. Por
     * isso a causa vai para o log, e nao para o cliente.
     */
    /**
     * Teto de leitura do historico clinico atingido.
     *
     * 429, e nao 403, de proposito: 403 diria "voce nao pode ver isto", o que e
     * falso -- o veterinario podia, e ate agora vinha podendo. O que aconteceu
     * foi um limite de VOLUME, e a distincao muda o que quem recebe faz a
     * seguir.
     */
    @ExceptionHandler(LimiteDeAcessoExcedidoException.class)
    public ResponseEntity<ErroValidacao> handleLimiteDeAcesso(LimiteDeAcessoExcedidoException ex) {
        log.warn("Teto de acesso ao historico atingido: {}", ex.getMessage());
        return respostaDe(HttpStatus.TOO_MANY_REQUESTS, ex.getCampo(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade ao gravar", ex);
        return respostaDe(HttpStatus.CONFLICT, "registro",
                "Registro duplicado ou em uso por outro cadastro.");
    }

    /**
     * Falha de login. A mensagem e a generica definida no AuthService: distinguir
     * "senha errada" de "e-mail inexistente" permitiria enumerar a base.
     */
    /**
     * Parametro de query obrigatorio ausente.
     *
     * Sem este handler a excecao escapava para o /error do container, e a resposta
     * que chegava ao cliente era um 401 de autenticacao -- ver o comentario do
     * dispatcherTypeMatchers no SecurityConfig. Aquele problema esta corrigido la;
     * este handler existe para que a resposta diga QUAL parametro faltou, em vez de
     * um 400 sem conteudo util.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroValidacao> handleParametroAusente(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new ErroValidacao(
                ex.getParameterName(),
                "Parâmetro obrigatório ausente: " + ex.getParameterName()));
    }

    /**
     * Valor de parametro que nao converte para o tipo esperado -- uma data mal
     * formatada, um UUID invalido. Mesma origem do handler acima.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroValidacao> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String esperado = ex.getRequiredType() == null ? "outro tipo" : ex.getRequiredType().getSimpleName();
        return ResponseEntity.badRequest().body(new ErroValidacao(
                ex.getName(),
                "Valor inválido para '" + ex.getName() + "': esperado " + esperado));
    }

    /**
     * Corpo que o Jackson não consegue ler: JSON quebrado, tipo incompatível, ou um
     * campo que o DTO não aceita.
     *
     * <p>O caso que motivou o handler é o terceiro. {@code PagamentoPatchRequest}
     * deixou de ter {@code statusPagamento} para fechar a regra P14 — as transições
     * acontecem em {@code /confirmar} e {@code /estornar}, e não no cadastro. Só que
     * fechar por omissão fecha pela metade: o campo deixava de ter efeito e a
     * requisição continuava respondendo <b>200</b>. Verificado contra a pilha no ar —
     * {@code PATCH} com {@code statusPagamento: "REEMBOLSADO"} devolvia 200 e o
     * registro seguia PENDENTE.</p>
     *
     * <p>Uma resposta que mente é pior que um erro: quem integra lê o 200, acredita
     * que mudou, e só descobre depois, olhando o extrato. Com
     * {@code ignoreUnknown = false} no DTO a requisição passa a falhar, e este
     * handler é quem diz <b>qual</b> campo não é aceito — sem ele, a resposta seria
     * um 400 sem conteúdo útil.</p>
     *
     * <p>Os demais casos ganham de brinde uma mensagem melhor que o 400 vazio que o
     * Spring devolvia. A mensagem interna do Jackson não vai junto de propósito: ela
     * carrega nomes de classe e de pacote do servidor.</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroValidacao> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        if (ex.getMostSpecificCause() instanceof CampoNaoAceitoException recusado) {
            return ResponseEntity.badRequest().body(new ErroValidacao(
                    recusado.getCampo(), recusado.getMessage()));
        }

        return ResponseEntity.badRequest().body(new ErroValidacao(
                "corpo", "Corpo da requisição inválido ou mal formatado"));
    }

    /**
     * {@code ?sort=campoQueNaoExiste} — ordenação por propriedade que a entidade
     * não tem.
     *
     * <p>Sem este handler a exceção escapava e virava <b>500</b>: qualquer cliente
     * derrubava um endpoint de listagem com uma query string. Verificado contra a
     * pilha no ar — {@code GET /api/v1/animais?sort=naoExiste,asc} respondia 500
     * com token perfeitamente válido.</p>
     *
     * <h3>Duas exceções, porque há dois caminhos</h3>
     *
     * <p>{@link PropertyReferenceException} é o que o Spring Data lança quando
     * resolve a propriedade por reflexão, nas consultas derivadas do nome do
     * método. Mas os repositórios deste projeto usam {@code @Query} com JPQL
     * escrito à mão: ali o {@code Sort} é <b>anexado</b> à consulta, e quem reclama
     * é o Hibernate, com {@code UnknownPathException} embrulhada em
     * {@link InvalidDataAccessApiUsageException}. Tratar só a primeira não resolvia
     * nada — foi o que os testes mostraram.</p>
     *
     * <h3>Não há injeção por aqui</h3>
     *
     * <p>{@code ?sort='; DROP TABLE,asc} não vira SQL: o próprio Spring Data
     * recusa antes, dizendo que a expressão "must only contain property
     * references". Ela cai neste mesmo handler e vira 400.</p>
     *
     * <p>A mensagem devolve a propriedade pedida, e não a lista das válidas:
     * enumerá-las revelaria os campos da entidade, incluindo os que não aparecem em
     * nenhuma resposta.</p>
     */
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<ErroValidacao> handleOrdenacaoInvalida(Exception ex) {
        String propriedade = ex instanceof PropertyReferenceException erro
                ? "'" + erro.getPropertyName() + "'"
                : "esse campo";

        return ResponseEntity.badRequest().body(new ErroValidacao(
                "sort", "Não é possível ordenar por " + propriedade + "."));
    }

    /**
     * O upload passou do limite do parser de multipart.
     *
     * <p>Sem este handler a excecao escapa do {@code @ControllerAdvice} e o
     * cliente le <b>500</b> — "erro no servidor" para algo que o servidor
     * recusou de proposito, e que a pessoa resolve escolhendo outro arquivo. O
     * teto do parser fica acima do teto da feature (ver comum.properties), de
     * modo que este caminho e a rede de fora: o arquivo absurdo, nao o arquivo
     * grande.</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroValidacao> handleUploadGrande(MaxUploadSizeExceededException ex) {
        return respostaDe(HttpStatus.PAYLOAD_TOO_LARGE, "arquivo",
                "O arquivo é grande demais para ser enviado.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroValidacao> handleCredenciais(BadCredentialsException ex) {
        return respostaDe(HttpStatus.UNAUTHORIZED, "credenciais", ex.getMessage());
    }

    /**
     * A rede de seguranca: o que nenhum handler acima reconheceu.
     *
     * <p>Sem ela, a excecao inesperada nao sai por este arquivo — sai pelo
     * tratamento padrao do Spring, que responde outro formato
     * ({@code timestamp}, {@code status}, {@code error}, {@code path}) e em
     * ingles. O aplicativo, que procura {@code mensagem}, cai no ultimo recurso
     * do parser e mostra "Internal Server Error" ao tutor. Ou seja: justamente
     * no pior momento, o contrato unico desta classe deixava de valer.</p>
     *
     * <p>Duas decisoes deliberadas:</p>
     *
     * <ul>
     *   <li>A mensagem e generica. A de {@code ex} cita classe, tabela e as
     *       vezes o SQL — nada disso ajuda quem le a tela, e entrega o desenho
     *       interno para quem estiver sondando.</li>
     *   <li>A referencia vai no corpo. E o unico erro em que o usuario nao tem o
     *       que corrigir sozinho, entao o que resta e ele poder dizer <em>qual</em>
     *       falha aconteceu. O mesmo id esta no log, junto da pilha.</li>
     * </ul>
     *
     * <p><b>O que ela NAO pode capturar.</b> Um catch-all colocado aqui roda
     * ANTES da traducao que o Spring e o Spring Security fazem sozinhos — o
     * {@code ExceptionHandlerExceptionResolver} tem precedencia sobre os demais
     * resolvers, e o {@code ExceptionTranslationFilter} da seguranca so ve o que
     * escapa do DispatcherServlet. Na primeira versao deste metodo, 38 testes de
     * autorizacao passaram a receber 500 no lugar de 403, e uma rota inexistente
     * virou 500 no lugar de 404: o catch-all estava engolindo justamente as
     * excecoes que ja sabiam o proprio status.</p>
     *
     * <p>Daí o {@link #jaSabeOProprioStatus}: se a excecao carrega a propria
     * semantica HTTP, ela nao e inesperada — e devolvida a cadeia, que a traduz
     * como sempre traduziu.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroValidacao> handleInesperado(Exception ex) throws Exception {
        if (jaSabeOProprioStatus(ex)) {
            throw ex;
        }

        String referencia = CorrelacaoFilter.atual();
        // A excecao inteira no log: a pilha e a unica coisa que sobra para
        // investigar, ja que o corpo da resposta nao pode conta-la.
        log.error("Falha nao tratada (referencia {})", referencia, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroValidacao(
                        "servidor",
                        "Erro inesperado no servidor. Tente novamente em instantes.",
                        referencia));
    }

    /**
     * A excecao ja diz qual resposta HTTP merece?
     *
     * <p>Tres familias dizem:</p>
     *
     * <ul>
     *   <li>{@code AccessDeniedException} e {@code AuthenticationException} — a
     *       cadeia do Spring Security responde 403 e 401 no formato do
     *       {@code RespostaErroSeguranca}. Traduzi-las aqui daria 500 numa
     *       negativa de acesso, que e o oposto do que aconteceu: o sistema
     *       funcionou exatamente como devia.</li>
     *   <li>{@code ErrorResponse} — a interface que as excecoes de MVC do Spring
     *       implementam justamente para carregar o proprio status
     *       ({@code NoResourceFoundException} 404,
     *       {@code HttpRequestMethodNotSupportedException} 405, e as demais).</li>
     *   <li>Qualquer excecao anotada com {@code @ResponseStatus}, que e a forma
     *       declarativa de dizer a mesma coisa.</li>
     * </ul>
     */
    private static boolean jaSabeOProprioStatus(Exception ex) {
        return ex instanceof AccessDeniedException
                || ex instanceof AuthenticationException
                || ex instanceof ErrorResponse
                || AnnotatedElementUtils.hasAnnotation(ex.getClass(), ResponseStatus.class);
    }

    private ResponseEntity<ErroValidacao> respostaDe(HttpStatus status, String campo, String mensagem) {
        return ResponseEntity.status(status).body(new ErroValidacao(campo, mensagem));
    }
}
