package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Quem entra pelas telas com uma conta Google ou GitHub (ADR 002).
 *
 * <p>A regra e a do {@code UsuarioService} (X13): o e-mail do provedor so abre
 * uma conta que ja existe, nunca cria nem vincula outra. E o e-mail precisa vir
 * verificado pelo provedor -- senao qualquer um cadastraria o e-mail alheio la
 * e entraria aqui como o dono dele.</p>
 */
class OAuth2UsuarioServiceTest extends TesteDeApi {

    private static final String EMAIL_PROPRIO = "oauth2.web@teste.com";

    @Autowired OAuth2UsuarioService oauth2UsuarioService;
    @Autowired UsuarioRepository usuarioRepository;

    @BeforeEach
    void criarUsuario() {
        usuarioRepository.findByEmail(EMAIL_PROPRIO).ifPresent(usuarioRepository::delete);
        usuarioRepository.save(Usuario.novo(EMAIL_PROPRIO, "sem-senha-usada-aqui", Perfil.TUTOR));
    }

    private static OAuth2User contaGithub() {
        return new DefaultOAuth2User(List.of(), Map.of("login", "lucas-gh", "id", 42), "login");
    }

    @Test
    @DisplayName("e-mail verificado de uma conta existente entra como o usuario dela")
    void emailConhecidoEntraComoOUsuario() {
        UsuarioOAuth2 usuario = oauth2UsuarioService.entrar(contaGithub(), LUCAS);

        assertThat(usuario.getUsername()).isEqualTo(LUCAS);
        assertThat(usuario.getTutorId()).isNotNull();
        assertThat(usuario.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_TUTOR");
        // A conta externa continua acessivel: o Spring le o nome e os atributos dela.
        assertThat(usuario.getAttributes()).containsEntry("login", "lucas-gh");
    }

    @Test
    @DisplayName("e-mail sem conta no ClyvoVet e recusado, sem criar conta nova")
    void emailDesconhecidoERecusado() {
        long antes = usuarioRepository.count();

        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaGithub(), "ninguem@nada.com"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(OAuth2UsuarioService.CONTA_NAO_ENCONTRADA);

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("sem e-mail verificado pelo provedor, ninguem entra")
    void semEmailVerificadoERecusado() {
        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaGithub(), null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("conta inativa e recusada mesmo com o e-mail certo")
    void contaInativaERecusada() {
        Usuario inativo = usuarioRepository.findByEmail(EMAIL_PROPRIO).orElseThrow();
        inativo.setAtivo(false);
        usuarioRepository.save(inativo);

        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaGithub(), EMAIL_PROPRIO))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("conta bloqueada por senha errada continua bloqueada pelo Google ou GitHub")
    void contaBloqueadaERecusada() {
        Usuario bloqueado = usuarioRepository.findByEmail(EMAIL_PROPRIO).orElseThrow();
        bloqueado.setBloqueadoAte(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(bloqueado);

        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaGithub(), EMAIL_PROPRIO))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("do Google, so vale o e-mail que ele marca como verificado")
    void googleSoEntregaEmailVerificado() {
        assertThat(OAuth2UsuarioService.emailVerificadoDoGoogle(contaGoogle(LUCAS, true))).isEqualTo(LUCAS);
        assertThat(OAuth2UsuarioService.emailVerificadoDoGoogle(contaGoogle(LUCAS, false))).isNull();
    }

    @Test
    @DisplayName("do GitHub, so vale o e-mail principal e verificado da lista")
    void githubSoEntregaEmailPrincipalVerificado() {
        List<Map<String, Object>> emails = List.of(
                Map.of("email", "secundario@email.com", "primary", false, "verified", true),
                Map.of("email", LUCAS, "primary", true, "verified", true));

        assertThat(OAuth2UsuarioService.emailPrincipalVerificado(emails)).isEqualTo(LUCAS);
    }

    @Test
    @DisplayName("do GitHub, o e-mail principal ainda nao verificado nao vale")
    void githubRecusaPrincipalNaoVerificado() {
        List<Map<String, Object>> emails = List.of(
                Map.of("email", LUCAS, "primary", true, "verified", false));

        assertThat(OAuth2UsuarioService.emailPrincipalVerificado(emails)).isNull();
    }

    private static DefaultOidcUser contaGoogle(String email, boolean verificado) {
        OidcIdToken token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "123", "email", email, "email_verified", verificado));
        return new DefaultOidcUser(List.of(), token);
    }
}
