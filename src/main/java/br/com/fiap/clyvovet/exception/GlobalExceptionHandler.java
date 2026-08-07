package br.com.fiap.clyvovet.exception;

import br.com.fiap.clyvovet.dto.exception.ErroValidacao;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErroValidacao>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ErroValidacao> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErroValidacao(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(erros);
    }

    // Captura EntityNotFoundException e retorna 404 com mensagem legível
    // Sem isso, o Spring retorna 500 por padrão quando a entidade não é encontrada
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErroValidacao> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroValidacao("id", ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroValidacao> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroValidacao(ex.getCampo(), ex.getMessage()));
    }

    /**
     * Unicidade de CPF, CNPJ, CRMV e e-mail existe apenas como constraint no
     * banco. Sem este handler, uma duplicata sobe como 500 carregando o SQL e
     * o nome da constraint na resposta — o que expõe a estrutura interna.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroValidacao> handleIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroValidacao("registro",
                        "Registro duplicado ou em uso por outro cadastro."));
    }

    /**
     * Falha de login. A mensagem é a genérica definida no AuthService: distinguir
     * "senha errada" de "e-mail inexistente" permitiria enumerar a base.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroValidacao> handleCredenciais(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroValidacao("credenciais", ex.getMessage()));
    }
}
