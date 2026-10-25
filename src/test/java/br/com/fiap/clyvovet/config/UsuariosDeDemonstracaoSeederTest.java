package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static br.com.fiap.clyvovet.config.UsuariosDeDemonstracaoSeeder.EMAIL_ADMIN;
import static br.com.fiap.clyvovet.config.UsuariosDeDemonstracaoSeeder.EMAIL_ADMIN_DE_CLINICA;
import static br.com.fiap.clyvovet.config.UsuariosDeDemonstracaoSeeder.EMAIL_TUTOR;
import static br.com.fiap.clyvovet.config.UsuariosDeDemonstracaoSeeder.EMAIL_VETERINARIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os usuarios que o professor usa para testar o sistema no ar.
 *
 * <p>O banco de producao nasce com clinicas, veterinarios e tutores vindos das
 * migrations, mas com zero usuarios: o {@code DevDataSeeder} nao roda no perfil
 * {@code mysql}, de proposito. Estes testes provam o que o seeder de demonstracao
 * faz no lugar dele, e principalmente o que ele recusa fazer.</p>
 *
 * <p>Chamam {@code semear(senha)} direto, em vez de depender de uma variavel de
 * ambiente: a leitura da variavel e uma linha, e a regra esta no metodo.</p>
 */
class UsuariosDeDemonstracaoSeederTest extends TesteDeApi {

    private static final String SENHA = "Demo-Teste-2026!";
    private static final List<String> EMAILS =
            List.of(EMAIL_ADMIN, EMAIL_ADMIN_DE_CLINICA, EMAIL_VETERINARIA, EMAIL_TUTOR);

    @Autowired
    private UsuariosDeDemonstracaoSeeder seeder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** A suite compartilha o banco: nao deixa usuario de demonstracao para os outros testes. */
    @AfterEach
    void removerUsuariosDeDemonstracao() {
        EMAILS.forEach(email -> usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete));
    }

    @Test
    @DisplayName("com uma senha valida, cria um usuario de cada perfil, ligado a quem ele representa")
    void criaUmUsuarioDeCadaPerfilComOVinculoCerto() {
        seeder.semear(SENHA);

        Usuario admin = usuario(EMAIL_ADMIN);
        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);

        Usuario gestor = usuario(EMAIL_ADMIN_DE_CLINICA);
        assertThat(gestor.getPerfil()).isEqualTo(Perfil.ADMIN_CLINICA);
        assertThat(gestor.getClinica()).as("o banco recusa ADMIN_CLINICA sem clinica").isNotNull();

        Usuario veterinaria = usuario(EMAIL_VETERINARIA);
        assertThat(veterinaria.getPerfil()).isEqualTo(Perfil.VETERINARIO);
        assertThat(veterinaria.getVeterinario()).isNotNull();

        Usuario tutor = usuario(EMAIL_TUTOR);
        assertThat(tutor.getPerfil()).isEqualTo(Perfil.TUTOR);
        assertThat(tutor.getTutor()).isNotNull();
    }

    @Test
    @DisplayName("a senha nao fica em texto puro no banco")
    void guardaSomenteOHashDaSenha() {
        seeder.semear(SENHA);

        assertThat(usuario(EMAIL_TUTOR).getSenha()).isNotEqualTo(SENHA).startsWith("$2");
    }

    @Test
    @DisplayName("cada usuario entra com a senha e e recusado com outra")
    void todosEntramComASenhaInformada() throws Exception {
        seeder.semear(SENHA);

        for (String email : EMAILS) {
            assertThat(token(email, SENHA)).as("login de " + email).isNotBlank();
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"outra-senha-qualquer\"}".formatted(EMAIL_TUTOR)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o token de cada usuario de demonstracao abre /auth/me com o perfil e o vinculo certos")
    void tokenAbreOsDadosDoProprioUsuario() throws Exception {
        seeder.semear(SENHA);

        buscar("/api/v1/auth/me", token(EMAIL_ADMIN, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL_ADMIN))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
        buscar("/api/v1/auth/me", token(EMAIL_ADMIN_DE_CLINICA, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN_CLINICA"));
        buscar("/api/v1/auth/me", token(EMAIL_VETERINARIA, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("VETERINARIO"))
                .andExpect(jsonPath("$.veterinarioNome").value(org.hamcrest.Matchers.containsString("Camila")));
        buscar("/api/v1/auth/me", token(EMAIL_TUTOR, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("TUTOR"))
                .andExpect(jsonPath("$.tutorNome").value(org.hamcrest.Matchers.containsString("Lucas")));
    }

    @Test
    @DisplayName("o perfil limita o que o token faz: so o ADMIN passa da porta de criar usuarios")
    void perfilLimitaOQueOTokenFaz() throws Exception {
        seeder.semear(SENHA);

        // Corpo vazio de proposito: nenhum usuario e criado. A porta de perfil vem
        // antes da validacao, entao o ADMIN chega ate o 400 e os outros param no 403.
        assertThat(criarUsuarioVazio(token(EMAIL_ADMIN, SENHA))).isEqualTo(400);
        assertThat(criarUsuarioVazio(token(EMAIL_TUTOR, SENHA))).isEqualTo(403);
        assertThat(criarUsuarioVazio(token(EMAIL_VETERINARIA, SENHA))).isEqualTo(403);
    }

    private int criarUsuarioVazio(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("rodar de novo nao duplica nem troca a senha de quem ja existe")
    void ehIdempotente() throws Exception {
        seeder.semear(SENHA);
        long antes = usuarioRepository.count();

        seeder.semear("Outra-Senha-Valida-2027!");

        assertThat(usuarioRepository.count()).isEqualTo(antes);
        assertThat(token(EMAIL_TUTOR, SENHA)).as("a senha original continua valendo").isNotBlank();
    }

    @Test
    @DisplayName("senha curta demais: recusa e nao cria ninguem")
    void recusaSenhaCurta() {
        seeder.semear("curta123");

        assertThat(EMAILS).noneMatch(usuarioRepository::existsByEmail);
    }

    @Test
    @DisplayName("sem senha definida (o estado normal), nao faz nada")
    void semSenhaNaoFazNada() {
        seeder.semear("");
        seeder.semear(null);

        assertThat(EMAILS).noneMatch(usuarioRepository::existsByEmail);
    }

    private Usuario usuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("usuario de demonstracao ausente: " + email));
    }
}
