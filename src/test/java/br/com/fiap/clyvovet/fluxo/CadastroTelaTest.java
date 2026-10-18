package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * A tela publica "Criar conta": qualquer pessoa se cadastra como tutor, sem
 * depender de a clinica criar a conta antes.
 *
 * <p>E a mesma regra do {@code POST /api/v1/auth/registrar} -- o perfil e sempre
 * TUTOR e o registro cria o tutor junto --, so que por formulario, com sessao e
 * CSRF. O que estes testes fixam e o que a tela NAO pode deixar acontecer:
 * escolher o proprio perfil, aceitar senha fraca ou duplicar um e-mail.</p>
 */
class CadastroTelaTest extends TesteDeTela {

    private static final String SENHA = "Senha-Forte-2026";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TutorRepository tutorRepository;

    private final List<String> emailsCriados = new ArrayList<>();

    /** A suite compartilha o banco: apaga o usuario e depois o tutor, que ele referencia. */
    @AfterEach
    void apagarContasCriadas() {
        for (String email : emailsCriados) {
            usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete);
            tutorRepository.findAll().stream()
                    .filter(tutor -> email.equals(tutor.getEmail()))
                    .forEach(tutorRepository::delete);
        }
    }

    private String emailNovo() {
        String email = "novo-" + UUID.randomUUID() + "@exemplo.com";
        emailsCriados.add(email);
        return email;
    }

    @Test
    @DisplayName("a tela abre sem login, com os campos e o token CSRF")
    void telaAbreSemLogin() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(content().string(containsString("name=\"nome\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"senha\"")))
                .andExpect(content().string(containsString("name=\"confirmacaoDaSenha\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @DisplayName("a tela de login leva a tela de criar conta")
    void loginTemLinkParaCadastro() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/cadastro\"")));
    }

    @Test
    @DisplayName("cadastro valido cria um tutor, mesmo que o formulario tente pedir ADMIN")
    void cadastroValidoCriaTutor() throws Exception {
        String email = emailNovo();

        enviarFormulario("/cadastro",
                "nome", "Professor Avaliador",
                "email", email,
                "senha", SENHA,
                "confirmacaoDaSenha", SENHA,
                "perfil", "ADMIN")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?conta-criada"));

        Usuario criado = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(criado.getPerfil()).as("o perfil nunca vem do formulario").isEqualTo(Perfil.TUTOR);
        assertThat(criado.getTutor()).isNotNull();
        assertThat(criado.getTutor().getNome()).isEqualTo("Professor Avaliador");
        assertThat(token(email, SENHA)).as("a conta nova entra com a senha escolhida").isNotBlank();
    }

    @Test
    @DisplayName("senhas diferentes: volta ao formulario com o erro e nao cria conta")
    void recusaSenhasDiferentes() throws Exception {
        String email = emailNovo();

        enviarFormulario("/cadastro",
                "nome", "Professor Avaliador", "email", email,
                "senha", SENHA, "confirmacaoDaSenha", SENHA + "x")
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("cadastro", "confirmacaoDaSenha"));

        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
    }

    @Test
    @DisplayName("senha curta demais: recusa e nao cria conta")
    void recusaSenhaCurta() throws Exception {
        String email = emailNovo();

        enviarFormulario("/cadastro",
                "nome", "Professor Avaliador", "email", email,
                "senha", "curta", "confirmacaoDaSenha", "curta")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastro", "senha"));

        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
    }

    @Test
    @DisplayName("e-mail invalido e nome curto demais sao recusados")
    void recusaEmailInvalidoENomeCurto() throws Exception {
        enviarFormulario("/cadastro",
                "nome", "Jo", "email", "isto-nao-e-um-email",
                "senha", SENHA, "confirmacaoDaSenha", SENHA)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastro", "email", "nome"));
    }

    @Test
    @DisplayName("e-mail que ja tem conta: recusa com erro no campo e nao duplica")
    void recusaEmailJaCadastrado() throws Exception {
        long antes = usuarioRepository.count();

        enviarFormulario("/cadastro",
                "nome", "Outra Pessoa", "email", LUCAS,
                "senha", SENHA, "confirmacaoDaSenha", SENHA)
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("cadastro", "email"));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("sem o token CSRF o cadastro e recusado com 403")
    void semCsrfEhRecusado() throws Exception {
        String email = emailNovo();

        mockMvc.perform(post("/cadastro")
                        .param("nome", "Professor Avaliador")
                        .param("email", email)
                        .param("senha", SENHA)
                        .param("confirmacaoDaSenha", SENHA))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
    }
}
