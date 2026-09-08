package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria o primeiro ADMIN da plataforma, a partir do ambiente.
 *
 * <h2>Por que isto precisa existir</h2>
 *
 * <p>O {@link DevDataSeeder} não roda no perfil {@code mysql}, e a decisão está
 * certa: banco de entrega não deve receber usuário de desenvolvimento com senha
 * conhecida. Só que a consequência era um beco sem saída — {@code POST
 * /auth/usuarios} exige {@code hasRole(ADMIN)}, e {@code /auth/registrar} só cria
 * TUTOR. Sem nenhum ADMIN no banco, <b>ninguém consegue criar o primeiro</b>.</p>
 *
 * <p>Verificado contra a pilha local: 5 tutores, 7 veterinários e 5 clínicas vindos
 * das migrations, e {@code t_clyvo_usuario} com <b>zero linhas</b>. Na prática, todo
 * fluxo de veterinário e de administração ficava impossível de exercitar — inclusive
 * na gravação do vídeo.</p>
 *
 * <h2>Por que do ambiente, e não de uma migration</h2>
 *
 * <p>Hash de credencial não se versiona. Uma migration com senha fixa vale para
 * todo mundo que clonar o repositório, e vale para sempre. Vindo do ambiente, a
 * senha entra como App Setting na Azure — mesmo caminho de {@code JWT_SECRET} e
 * {@code DB_PASSWORD}, que a régua de DevOps cobra que fiquem fora do código.</p>
 *
 * <h2>Comportamento</h2>
 *
 * <ul>
 *   <li>Sem as duas variáveis definidas: <b>não faz nada</b>. Não é erro — é o
 *       estado normal em desenvolvimento, onde o {@code DevDataSeeder} já cuidou.</li>
 *   <li>Já existe algum ADMIN: não faz nada. É idempotente, e roda a cada boot.</li>
 *   <li>Caso contrário: cria, e avisa no log para trocar a senha.</li>
 * </ul>
 *
 * <p>A senha nunca é registrada em log — só o e-mail.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminInicialSeeder {

    /** Mesmo mínimo que o cadastro de usuário exige. */
    private static final int TAMANHO_MINIMO_DA_SENHA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${clyvovet.admin.email:}")
    private String email;

    @Value("${clyvovet.admin.senha:}")
    private String senha;

    @Bean
    public ApplicationRunner semearAdminInicial() {
        return args -> {
            if (email.isBlank() || senha.isBlank()) {
                log.debug("clyvovet.admin.email/senha não definidos — nenhum ADMIN inicial criado.");
                return;
            }

            if (senha.length() < TAMANHO_MINIMO_DA_SENHA) {
                // Não derruba o boot: uma senha curta numa variável de ambiente não
                // pode impedir a aplicação de subir. Só não cria o usuário.
                log.error("clyvovet.admin.senha tem menos de {} caracteres — ADMIN inicial NÃO criado.",
                        TAMANHO_MINIMO_DA_SENHA);
                return;
            }

            if (usuarioRepository.existsByPerfil(Perfil.ADMIN)) {
                log.debug("Já existe ADMIN na base — nada a fazer.");
                return;
            }

            if (usuarioRepository.existsByEmail(email)) {
                // O e-mail já pertence a um usuário de outro perfil. Promover por
                // variável de ambiente seria escalação silenciosa de privilégio.
                log.error("Já existe usuário com o e-mail {} e ele não é ADMIN — "
                        + "ADMIN inicial NÃO criado. Use outro e-mail.", email);
                return;
            }

            Usuario admin = new Usuario();
            admin.setEmail(email);
            admin.setSenha(passwordEncoder.encode(senha));
            admin.setPerfil(Perfil.ADMIN);
            admin.setAtivo(true);
            usuarioRepository.save(admin);

            log.warn("ADMIN inicial criado: {}. Troque a senha no primeiro acesso e "
                    + "remova a variável do ambiente depois.", email);
        };
    }
}
