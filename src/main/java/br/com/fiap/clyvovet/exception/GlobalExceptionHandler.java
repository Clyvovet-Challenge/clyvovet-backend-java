package br.com.fiap.clyvovet.exception;

import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroValidacao> handleCredenciais(BadCredentialsException ex) {
        return respostaDe(HttpStatus.UNAUTHORIZED, "credenciais", ex.getMessage());
    }

    private ResponseEntity<ErroValidacao> respostaDe(HttpStatus status, String campo, String mensagem) {
        return ResponseEntity.status(status).body(new ErroValidacao(campo, mensagem));
    }
}
