package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Quem entra pelas telas com uma conta Google ou GitHub (ADR 002 e ADR 009).
 *
 * <p>O e-mail do provedor abre a conta que ja existe. Quando nao existe, e SO se
 * o provedor garante que o e-mail e de quem clicou, o login cria uma conta de
 * TUTOR -- o perfil de menor poder, nunca outro. Nunca vincula a um cadastro que a
 * clinica ja fez com aquele e-mail: isso continua sendo a regra X13 do
 * {@code UsuarioService}.</p>
 */
class OAuth2UsuarioServiceTest extends TesteDeApi {

    private static final String EMAIL_PROPRIO = "oauth2.web@teste.com";

    @Autowired OAuth2UsuarioService oauth2UsuarioService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired TutorRepository tutorRepository;

    private final List<String> emailsNovos = new ArrayList<>();

    @BeforeEach
    void criarUsuario() {
        usuarioRepository.findByEmail(EMAIL_PROPRIO).ifPresent(usuarioRepository::delete);
        usuarioRepository.save(Usuario.novo(EMAIL_PROPRIO, "sem-senha-usada-aqui", Perfil.TUTOR));
    }

    /** A suite compartilha o banco: apaga o usuario e depois o tutor, que ele referencia. */
    @AfterEach
    void apagarContasCriadasPeloLoginSocial() {
        for (String email : emailsNovos) {
            usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete);
            tutorRepository.findAll().stream()
                    .filter(tutor -> email.equals(tutor.getEmail()))
                    .forEach(tutorRepository::delete);
        }
    }

    private String emailNovo() {
        String email = "social-" + UUID.randomUUID() + "@exemplo.com";
        emailsNovos.add(email);
        return email;
    }

    private static OAuth2User contaComNome(String nome) {
        return new DefaultOAuth2User(List.of(), Map.of("login", "ana-gh", "name", nome, "id", 7), "login");
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
    @DisplayName("e-mail verificado sem conta cria um TUTOR com o nome do provedor e entra")
    void emailVerificadoSemContaCriaUmTutor() {
        String email = emailNovo();

        UsuarioOAuth2 usuario = oauth2UsuarioService.entrar(contaComNome("Ana Souza"), email);

        assertThat(usuario.getUsername()).isEqualTo(email);
        assertThat(usuario.getTutorId()).isNotNull();
        assertThat(usuario.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_TUTOR");

        Usuario criado = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(criado.getPerfil()).as("conta criada por provedor e sempre TUTOR").isEqualTo(Perfil.TUTOR);
        assertThat(criado.getTutor().getNome()).isEqualTo("Ana Souza");
        assertThat(criado.getSenha()).as("so o hash de uma senha que ninguem conhece").startsWith("$2");
    }

    @Test
    @DisplayName("sem nome no provedor, usa o login; sem login, usa o e-mail")
    void nomeCaiParaOLoginEDepoisParaOEmail() {
        String comLogin = emailNovo();
        oauth2UsuarioService.entrar(contaGithub(), comLogin);
        assertThat(usuarioRepository.findByEmail(comLogin).orElseThrow().getTutor().getNome()).isEqualTo("lucas-gh");

        String semNada = emailNovo();
        OAuth2User soComId = new DefaultOAuth2User(List.of(), Map.of("id", 9), "id");
        oauth2UsuarioService.entrar(soComId, semNada);
        assertThat(usuarioRepository.findByEmail(semNada).orElseThrow().getTutor().getNome())
                .isEqualTo(semNada.substring(0, semNada.indexOf('@')));
    }

    @Test
    @DisplayName("entrar de novo com o mesmo e-mail abre a conta criada, sem duplicar")
    void entrarDeNovoNaoDuplica() {
        String email = emailNovo();
        oauth2UsuarioService.entrar(contaComNome("Ana Souza"), email);
        long depoisDoPrimeiro = usuarioRepository.count();

        UsuarioOAuth2 segunda = oauth2UsuarioService.entrar(contaComNome("Outro Nome"), email);

        assertThat(usuarioRepository.count()).isEqualTo(depoisDoPrimeiro);
        assertThat(segunda.getUsername()).isEqualTo(email);
        assertThat(usuarioRepository.findByEmail(email).orElseThrow().getTutor().getNome())
                .as("o nome da primeira vez nao e trocado pelo provedor").isEqualTo("Ana Souza");
    }

    @Test
    @DisplayName("e-mail que a clinica ja cadastrou como tutor, sem usuario, nao vira conta sozinho")
    void tutorPreCadastradoNaoViraContaAutomaticamente() {
        String email = emailNovo();
        Tutor preCadastrado = new Tutor();
        preCadastrado.setNome("Cadastrado pela clinica");
        preCadastrado.setEmail(email);
        tutorRepository.save(preCadastrado);
        long antes = usuarioRepository.count();

        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaComNome("Quem Clicou"), email))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(OAuth2UsuarioService.CONTA_NAO_ENCONTRADA);

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("sem e-mail verificado pelo provedor, ninguem entra e nada e criado")
    void semEmailVerificadoERecusado() {
        long antes = usuarioRepository.count();

        assertThatThrownBy(() -> oauth2UsuarioService.entrar(contaGithub(), null))
                .isInstanceOf(OAuth2AuthenticationException.class);

        assertThat(usuarioRepository.count()).isEqualTo(antes);
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
