package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.auth.UsuarioResponse;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.model.Veterinario;
import org.springframework.stereotype.Component;

/**
 * O mapeamento de usuario morava dentro do AuthService, unico caso em que a
 * conversao nao estava no pacote mapper. Fora do lugar, ninguem o encontrava
 * para reaproveitar.
 *
 * A senha nao aparece aqui — nem o hash. Um DTO de resposta que nunca conhece
 * o campo e mais confiavel do que lembrar de omiti-lo caso a caso.
 */
@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                Referencias.de(usuario.getTutor(), Tutor::getId),
                Referencias.de(usuario.getTutor(), Tutor::getNome),
                Referencias.de(usuario.getVeterinario(), Veterinario::getId),
                Referencias.de(usuario.getVeterinario(), Veterinario::getNome));
    }
}
