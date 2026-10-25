package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cria, no banco de entrega, os usuarios com que o professor testa o sistema.
 *
 * <h2>O problema</h2>
 *
 * <p>O banco de producao nasce com clinicas, veterinarios e tutores vindos das
 * migrations, mas sem nenhum usuario: o {@link DevDataSeeder} nao roda no perfil
 * {@code mysql}, de proposito, e {@code /auth/registrar} so cria TUTOR. Quem abre o
 * app no ar so consegue entrar como um tutor novo, sem pets e sem agenda, e nunca
 * como veterinario nem como administrador.</p>
 *
 * <h2>Por que nao reaproveitar o DevDataSeeder</h2>
 *
 * <p>Ele tem senhas fixas e publicas no codigo ({@code admin12345}). Ligar o perfil
 * {@code local} na Render poria um ADMIN com senha conhecida na internet. Este
 * seeder faz o oposto: a senha <b>vem do ambiente</b> ({@code CLYVOVET_DEMO_SENHA}),
 * entra na Render como segredo, e quem a conhece e quem recebeu do grupo.</p>
 *
 * <h2>Comportamento</h2>
 *
 * <ul>
 *   <li>Sem a variavel: <b>nao faz nada</b>. E o estado normal em desenvolvimento.</li>
 *   <li>Senha com menos de {@value #TAMANHO_MINIMO_DA_SENHA} caracteres: recusa e avisa.
 *       Estas contas ficam na internet, entao o minimo e maior que o do cadastro.</li>
 *   <li>Idempotente: o que ja existe nao e recriado, e a senha de quem existe nao muda.</li>
 *   <li>A senha nunca vai para o log.</li>
 * </ul>
 *
 * <p>Os usuarios ficam ligados a linhas que a V2 ja semeia (a VetCare, a Camila e o
 * Lucas, que tem pets), para que os fluxos tenham dado de verdade para mostrar.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UsuariosDeDemonstracaoSeeder {

    public static final String EMAIL_ADMIN = "demo.admin@clyvovet.com";
    public static final String EMAIL_ADMIN_DE_CLINICA = "demo.gestor@clyvovet.com";
    public static final String EMAIL_VETERINARIA = "demo.veterinaria@clyvovet.com";
    public static final String EMAIL_TUTOR = "demo.tutor@clyvovet.com";

    static final int TAMANHO_MINIMO_DA_SENHA = 12;

    // IDs fixos vindos da migration V2__seed_inicial.sql
    private static final UUID CLINICA_VETCARE = UUID.fromString("11111111-1111-1111-1111-000000000001");
    private static final UUID TUTOR_LUCAS = UUID.fromString("22222222-2222-2222-2222-000000000001");
    private static final UUID VET_CAMILA = UUID.fromString("33333333-3333-3333-3333-000000000001");

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${clyvovet.demo.senha:}")
    private String senhaDoAmbiente;

    @Bean
    public ApplicationRunner semearUsuariosDeDemonstracao() {
        return args -> semear(senhaDoAmbiente);
    }

    /** Cria os quatro usuarios de demonstracao com a senha informada, se ela servir. */
    public void semear(String senha) {
        if (senha == null || senha.isBlank()) {
            log.debug("clyvovet.demo.senha nao definida - nenhum usuario de demonstracao criado.");
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_DA_SENHA) {
            // Nao derruba o boot: uma variavel mal preenchida nao pode tirar o app do ar.
            log.error("clyvovet.demo.senha tem menos de {} caracteres - usuarios de demonstracao NAO criados.",
                    TAMANHO_MINIMO_DA_SENHA);
            return;
        }

        salvarSeAusente(EMAIL_ADMIN, senha, Perfil.ADMIN, usuario -> { });
        salvarSeAusente(EMAIL_ADMIN_DE_CLINICA, senha, Perfil.ADMIN_CLINICA,
                usuario -> clinicaRepository.findById(CLINICA_VETCARE).ifPresent(usuario::setClinica));
        salvarSeAusente(EMAIL_VETERINARIA, senha, Perfil.VETERINARIO,
                usuario -> veterinarioRepository.findById(VET_CAMILA).ifPresent(usuario::setVeterinario));
        salvarSeAusente(EMAIL_TUTOR, senha, Perfil.TUTOR,
                usuario -> tutorRepository.findById(TUTOR_LUCAS).ifPresent(usuario::setTutor));

        log.warn("Usuarios de demonstracao disponiveis: {}, {}, {}, {}. Remova CLYVOVET_DEMO_SENHA "
                        + "do ambiente quando a avaliacao terminar.",
                EMAIL_ADMIN, EMAIL_ADMIN_DE_CLINICA, EMAIL_VETERINARIA, EMAIL_TUTOR);
    }

    private void salvarSeAusente(String email, String senha, Perfil perfil, Consumer<Usuario> vincular) {
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        Usuario usuario = Usuario.novo(email, passwordEncoder.encode(senha), perfil);
        vincular.accept(usuario);
        usuarioRepository.save(usuario);
    }
}
