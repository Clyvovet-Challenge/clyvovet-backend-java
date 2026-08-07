package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Bloqueio de conta apos tentativas malsucedidas.
 *
 * Este teste existe por causa de um bug real: com o login anotado como
 * @Transactional, o rollback disparado pelo BadCredentialsException desfazia o
 * incremento do contador, e o bloqueio nunca chegava a valer. A correcao foi
 * mover a contagem para o ControleTentativasLogin, com REQUIRES_NEW.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BloqueioContaTest {

    private static final String EMAIL = "bloqueio@teste.com";
    private static final String SENHA = "senhaCorreta123";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarUsuario() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
        Usuario usuario = new Usuario();
        usuario.setEmail(EMAIL);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(Perfil.TUTOR);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private int tentarLogin(String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(EMAIL, senha)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("apos 5 falhas a conta bloqueia, mesmo com a senha correta")
    void contaBloqueiaApos5Falhas() throws Exception {
        assertThat(tentarLogin(SENHA)).isEqualTo(200);

        for (int i = 0; i < 5; i++) {
            assertThat(tentarLogin("senhaErrada999")).isEqualTo(401);
        }

        assertThat(tentarLogin(SENHA)).isEqualTo(401);
        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().estaBloqueado()).isTrue();
    }

    @Test
    @DisplayName("a contagem de falhas e persistida, nao perdida no rollback")
    void contagemDeFalhasEPersistida() throws Exception {
        tentarLogin("senhaErrada999");
        tentarLogin("senhaErrada999");

        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getTentativasFalhas()).isEqualTo(2);
    }

    @Test
    @DisplayName("login bem-sucedido zera a contagem acumulada")
    void loginBemSucedidoZeraContagem() throws Exception {
        tentarLogin("senhaErrada999");
        tentarLogin("senhaErrada999");
        assertThat(tentarLogin(SENHA)).isEqualTo(200);

        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getTentativasFalhas()).isZero();
    }

    @Test
    @DisplayName("o bloqueio e por conta e nao afeta outros usuarios")
    void bloqueioNaoAfetaOutrosUsuarios() throws Exception {
        for (int i = 0; i < 5; i++) {
            tentarLogin("senhaErrada999");
        }

        int status = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lucas.santos@email.com\",\"senha\":\"tutor12345\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(200);
    }
}
