package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

/**
 * O que as telas fazem com uma recusa do service.
 *
 * <p>A {@link RegraDeNegocioException} ja diz qual campo causou a recusa -- e o
 * mesmo campo que a API devolve no JSON. Na tela, a mensagem vai para o lado
 * desse campo. Quando o campo nao existe no formulario ("statusEvento", por
 * exemplo), ela vira um erro geral, mostrado no topo.</p>
 */
final class Formularios {

    private Formularios() {
    }

    static void rejeitar(BindingResult erros, RegraDeNegocioException recusa) {
        if (new BeanWrapperImpl(erros.getTarget()).isReadableProperty(recusa.getCampo())) {
            erros.rejectValue(recusa.getCampo(), "regra", recusa.getMessage());
        } else {
            erros.reject("regra", recusa.getMessage());
        }
    }
}
