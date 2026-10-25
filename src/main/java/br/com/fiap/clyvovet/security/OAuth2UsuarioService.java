package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Transforma a conta Google ou GitHub de quem clicou no botao em um usuario do
 * ClyvoVet -- ou recusa (ADR 002 e ADR 009).
 *
 * <p>O e-mail verificado abre a conta que ja existe. Se nao existe, nasce uma
 * conta de <b>TUTOR</b>, e nenhum outro perfil: o veterinario e o administrador
 * continuam sendo criados pela gestao. Nunca se vincula a um cadastro que a
 * clinica ja fez com aquele e-mail -- e a regra X13 do {@code UsuarioService}, e a
 * confirmacao do provedor sozinha nao a autoriza. O provedor so prova que a pessoa
 * e dona do e-mail.</p>
 *
 * <p>E o e-mail precisa vir <b>verificado</b> pelo provedor. Um e-mail nao
 * verificado e so um texto que alguem digitou la: aceita-lo deixaria qualquer
 * pessoa cadastrar o e-mail de um tutor no GitHub e entrar aqui como ele.</p>
 *
 * <p>A regra de quem entra mora em {@link #entrar}. Os dois metodos de carga sao
 * adaptadores: cada provedor entrega o e-mail verificado de um jeito.</p>
 */
@Service
@RequiredArgsConstructor
public class OAuth2UsuarioService {

    /** Codigo do erro que a tela de login traduz em "conta nao encontrada". */
    public static final String CONTA_NAO_ENCONTRADA = "conta_nao_encontrada";

    private static final String EMAILS_DO_GITHUB = "https://api.github.com/user/emails";

    /** Os limites do nome do tutor: os mesmos do cadastro por formulario. */
    private static final int NOME_MINIMO = 3;
    private static final int NOME_MAXIMO = 100;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    private final OidcUserService servicoGoogle = new OidcUserService();
    private final DefaultOAuth2UserService servicoGithub = new DefaultOAuth2UserService();
    private final RestClient restClient = RestClient.create();

    /** Google: o OIDC ja traz o e-mail e a marca de verificado no token de identidade. */
    public OidcUser carregarGoogle(OidcUserRequest requisicao) {
        OidcUser contaGoogle = servicoGoogle.loadUser(requisicao);
        return entrar(contaGoogle, emailVerificadoDoGoogle(contaGoogle));
    }

    /**
     * GitHub: o e-mail do perfil pode ser privado ou nao verificado, entao a
     * fonte e a lista de {@code /user/emails} (escopo {@code user:email}).
     */
    public OAuth2User carregarGithub(OAuth2UserRequest requisicao) {
        OAuth2User contaGithub = servicoGithub.loadUser(requisicao);
        return entrar(contaGithub, emailPrincipalVerificado(emailsDoGithub(requisicao)));
    }

    /**
     * A regra: o e-mail verificado abre a conta que ja existe, ativa e sem
     * bloqueio. Qualquer outro caso e a mesma recusa.
     *
     * <p>A conta bloqueada por senha errada continua bloqueada aqui. Do contrario,
     * o bloqueio que protege o formulario seria contornado por um clique.</p>
     *
     * @param emailVerificado {@code null} quando o provedor nao entregou nenhum
     */
    public UsuarioOAuth2 entrar(OAuth2User contaExterna, String emailVerificado) {
        if (emailVerificado == null) {
            throw recusa();
        }
        Usuario usuario = usuarioRepository.findByEmail(emailVerificado)
                .orElseGet(() -> criarContaDeTutor(contaExterna, emailVerificado));

        if (!usuario.isAtivo() || usuario.estaBloqueado()) {
            throw recusa();
        }
        return new UsuarioOAuth2(usuario, contaExterna);
    }

    /**
     * Primeiro acesso de quem nao tem conta (ADR 009): nasce um TUTOR. Se o e-mail
     * ja e de um tutor que a clinica cadastrou, o {@code UsuarioService} recusa, e a
     * recusa sobe como a mesma de sempre -- nao se vincula ninguem a um cadastro
     * existente so porque o provedor confirmou o e-mail.
     */
    private Usuario criarContaDeTutor(OAuth2User contaExterna, String email) {
        try {
            return usuarioService.registrarPorProvedor(email, nomeParaOTutor(contaExterna, email));
        } catch (RegraDeNegocioException jaExiste) {
            throw recusa();
        }
    }

    /**
     * O nome que o provedor mostra; se nao houver, o login (GitHub); se nao houver,
     * a parte do e-mail antes do arroba. O nome do tutor e a unica coisa que a
     * tabela exige, e a pessoa corrige depois pelo perfil.
     */
    static String nomeParaOTutor(OAuth2User contaExterna, String email) {
        for (String atributo : List.of("name", "login")) {
            Object valor = contaExterna.getAttribute(atributo);
            if (valor instanceof String texto && texto.strip().length() >= NOME_MINIMO) {
                return limitar(texto.strip());
            }
        }
        String parteLocal = email.substring(0, email.indexOf('@'));
        return limitar(parteLocal.length() >= NOME_MINIMO ? parteLocal : email);
    }

    private static String limitar(String nome) {
        return nome.length() > NOME_MAXIMO ? nome.substring(0, NOME_MAXIMO) : nome;
    }

    static String emailVerificadoDoGoogle(OidcUser contaGoogle) {
        return Boolean.TRUE.equals(contaGoogle.getEmailVerified()) ? contaGoogle.getEmail() : null;
    }

    static String emailPrincipalVerificado(List<Map<String, Object>> emails) {
        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                .map(email -> (String) email.get("email"))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> emailsDoGithub(OAuth2UserRequest requisicao) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(EMAILS_DO_GITHUB)
                    .headers(h -> h.setBearerAuth(requisicao.getAccessToken().getTokenValue()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return emails != null ? emails : List.of();
        } catch (RestClientException e) {
            // Sem a lista nao ha e-mail verificado, e sem ele ninguem entra.
            return List.of();
        }
    }

    /**
     * Uma recusa so, para os quatro motivos: a tela nao precisa distinguir, e a
     * pessoa ja provou ser dona do e-mail -- nao ha a quem esconder que ele nao
     * tem conta aqui.
     */
    private static OAuth2AuthenticationException recusa() {
        return new OAuth2AuthenticationException(new OAuth2Error(CONTA_NAO_ENCONTRADA,
                "Nenhuma conta ativa do ClyvoVet usa este e-mail", null));
    }
}
