package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * O par do {@code GlobalExceptionHandler} para as telas.
 *
 * <p>La, o desvio vira JSON; aqui, vira a pagina {@code error.html}. So o 404
 * precisa de tratamento proprio: a negativa de acesso (403) ja chega a pagina
 * pela cadeia de seguranca, e a recusa de regra de negocio e tratada no proprio
 * controller, que a mostra ao lado do campo do formulario.</p>
 */
@ControllerAdvice(basePackageClasses = ErroWebHandler.class)
public class ErroWebHandler {

    @ExceptionHandler({RecursoNaoEncontradoException.class, EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(Model model) {
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return "error";
    }
}
