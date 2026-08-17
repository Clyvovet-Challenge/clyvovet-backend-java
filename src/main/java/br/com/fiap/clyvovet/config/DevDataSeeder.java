package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cria os usuarios de desenvolvimento.
 *
 * Os hashes de senha sao gerados aqui, em tempo de execucao, em vez de irem
 * numa migration: hash de credencial nao deve ser versionado, e uma migration
 * com senha fixa acabaria aplicada tambem no banco de entrega.
 *
 * Ativo apenas nos perfis dev e h2. Idempotente — nada e recriado se ja existe.
 */
@Slf4j
@Configuration
@Profile({"dev", "h2"})
@RequiredArgsConstructor
public class DevDataSeeder {

    // IDs fixos vindos da migration V2__seed_inicial.sql
    private static final UUID TUTOR_LUCAS = UUID.fromString("22222222-2222-2222-2222-000000000001");
    private static final UUID TUTOR_MARIA = UUID.fromString("22222222-2222-2222-2222-000000000002");
    private static final UUID VET_CAMILA  = UUID.fromString("33333333-3333-3333-3333-000000000001");

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner semearUsuariosDeDesenvolvimento() {
        return args -> {
            criarAdmin("admin@clyvovet.com", "admin12345");
            criarVeterinario("camila.ferreira@vetcare.com.br", "vet12345", VET_CAMILA);
            // Dois tutores com pets distintos: e o que permite exercitar o
            // isolamento por dono sem precisar cadastrar nada a mao.
            criarTutor("lucas.santos@email.com", "tutor12345", TUTOR_LUCAS);
            criarTutor("maria.oliveira@email.com", "tutor12345", TUTOR_MARIA);
            log.info("Usuarios de desenvolvimento disponiveis: admin@clyvovet.com, "
                    + "camila.ferreira@vetcare.com.br, lucas.santos@email.com, maria.oliveira@email.com");
        };
    }

    private void criarAdmin(String email, String senha) {
        salvarSeAusente(email, senha, Perfil.ADMIN, usuario -> { });
    }

    private void criarVeterinario(String email, String senha, UUID veterinarioId) {
        salvarSeAusente(email, senha, Perfil.VETERINARIO,
                usuario -> veterinarioRepository.findById(veterinarioId).ifPresent(usuario::setVeterinario));
    }

    private void criarTutor(String email, String senha, UUID tutorId) {
        salvarSeAusente(email, senha, Perfil.TUTOR,
                usuario -> tutorRepository.findById(tutorId).ifPresent(usuario::setTutor));
    }

    private void salvarSeAusente(String email, String senha, Perfil perfil, Consumer<Usuario> vincular) {
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        vincular.accept(usuario);
        usuarioRepository.save(usuario);
    }
}
