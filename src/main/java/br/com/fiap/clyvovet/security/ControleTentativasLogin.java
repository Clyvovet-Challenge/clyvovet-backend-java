package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Politica de bloqueio de conta por tentativas malsucedidas.
 *
 * Vive num bean proprio, e nao dentro do AuthService, por um motivo concreto:
 * o registro da falha precisa ser COMMITADO mesmo quando o login termina em
 * excecao. Se o incremento acontecesse na mesma transacao do login, o rollback
 * disparado pelo BadCredentialsException desfaria a contagem e o bloqueio nunca
 * chegaria a valer — o contador voltaria a zero a cada tentativa.
 *
 * Dai o Propagation.REQUIRES_NEW: abre uma transacao separada, que commita
 * independentemente do destino da transacao que a chamou. Como o Spring aplica
 * @Transactional via proxy, isso so funciona a partir de outro bean; se estes
 * metodos fossem privados do AuthService, a anotacao seria ignorada.
 *
 * Complementa o RateLimitFilter: este limita tentativas POR CONTA, aquele
 * limita volume POR IP.
 */
@Component
@RequiredArgsConstructor
public class ControleTentativasLogin {

    private final UsuarioRepository usuarioRepository;

    @Value("${clyvovet.seguranca.max-tentativas-login:5}")
    private int maxTentativas;

    @Value("${clyvovet.seguranca.bloqueio-minutos:15}")
    private int bloqueioMinutos;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(Usuario usuario) {
        usuarioRepository.findById(usuario.getId()).ifPresent(atual -> {
            atual.setTentativasFalhas(atual.getTentativasFalhas() + 1);
            if (atual.getTentativasFalhas() >= maxTentativas) {
                atual.setBloqueadoAte(LocalDateTime.now().plusMinutes(bloqueioMinutos));
                atual.setTentativasFalhas(0);
            }
            usuarioRepository.save(atual);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarSucesso(Usuario usuario) {
        if (usuario.getTentativasFalhas() == 0 && usuario.getBloqueadoAte() == null) {
            return;
        }
        usuarioRepository.findById(usuario.getId()).ifPresent(atual -> {
            atual.setTentativasFalhas(0);
            atual.setBloqueadoAte(null);
            usuarioRepository.save(atual);
        });
    }
}
