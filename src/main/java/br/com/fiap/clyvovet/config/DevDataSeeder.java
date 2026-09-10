package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
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
 * Ativo nos perfis dev, h2, oracle e local. Idempotente — nada e recriado se
 * ja existe.
 *
 * O perfil local entrou em 10/09/2026. O stack do Docker de desenvolvimento sobe
 * a API com SPRING_PROFILES_ACTIVE=mysql, para falar com o MySQL 8 do compose --
 * e mysql NAO esta nesta lista, de proposito. O efeito colateral era que o banco
 * local subia sem usuario nenhum: as credenciais que os READMEs documentam
 * devolviam 401, e a unica saida era registrar um tutor a mao pelo endpoint
 * publico -- que nao cria veterinario nem admin.
 *
 * A correcao mantem a separacao: o compose passou a ativar "mysql,local", onde
 * mysql traz a conexao e local traz a semeadura. Producao continua ativando
 * apenas mysql, e continua sem receber usuario de desenvolvimento.
 *
 * O perfil oracle entrou na lista em 30/08/2026, quando o Oracle da FIAP virou
 * o banco de teste do projeto: sem estes usuarios a suite nao consegue fazer
 * login, e quase todo teste falharia com 401 por um motivo que nao e o dele.
 *
 * Note que producao (perfil mysql) continua FORA da lista, que e o ponto: o
 * banco de entrega nao deve receber usuario de desenvolvimento.
 */
@Slf4j
@Configuration
@Profile({"dev", "h2", "oracle", "local"})
@RequiredArgsConstructor
public class DevDataSeeder {

    // IDs fixos vindos da migration V2__seed_inicial.sql
    private static final UUID TUTOR_LUCAS = UUID.fromString("22222222-2222-2222-2222-000000000001");
    private static final UUID TUTOR_MARIA = UUID.fromString("22222222-2222-2222-2222-000000000002");
    private static final UUID VET_CAMILA  = UUID.fromString("33333333-3333-3333-3333-000000000001");
    private static final UUID CLINICA_VETCARE = UUID.fromString("11111111-1111-1111-1111-000000000001");

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner semearUsuariosDeDesenvolvimento() {
        return args -> {
            criarAdmin("admin@clyvovet.com", "admin12345");
            criarVeterinario("camila.ferreira@vetcare.com.br", "vet12345", VET_CAMILA);
            // Sem ele, um dos quatro aplicativos do app fica inalcancavel em
            // desenvolvimento: o perfil existe, mas nao ha por onde entrar nele sem
            // criar o usuario a mao pelo ADMIN. Vive na VetCare, que e a clinica da
            // Camila -- de proposito, para que os dois perfis se cruzem na mesma casa.
            criarAdminDeClinica("gestor.vetcare@clyvovet.com", "gestor12345", CLINICA_VETCARE);
            // Dois tutores com pets distintos: e o que permite exercitar o
            // isolamento por dono sem precisar cadastrar nada a mao.
            criarTutor("lucas.santos@email.com", "tutor12345", TUTOR_LUCAS);
            criarTutor("maria.oliveira@email.com", "tutor12345", TUTOR_MARIA);
            log.info("Usuarios de desenvolvimento disponiveis: admin@clyvovet.com, "
                    + "gestor.vetcare@clyvovet.com, camila.ferreira@vetcare.com.br, "
                    + "lucas.santos@email.com, maria.oliveira@email.com");
        };
    }

    private void criarAdmin(String email, String senha) {
        salvarSeAusente(email, senha, Perfil.ADMIN, usuario -> { });
    }

    private void criarVeterinario(String email, String senha, UUID veterinarioId) {
        salvarSeAusente(email, senha, Perfil.VETERINARIO,
                usuario -> veterinarioRepository.findById(veterinarioId).ifPresent(usuario::setVeterinario));
    }

    /**
     * O administrador do estabelecimento.
     *
     * <p>E o unico perfil cujo vinculo o BANCO cobra: {@code chk_usuario_clinica}
     * recusa ADMIN_CLINICA sem clinica. Se a V2 nao tiver rodado, o
     * {@code ifPresent} deixa o usuario sem vinculo e o insert falha na constraint —
     * ruidoso, e nao silencioso, que e o comportamento certo aqui.</p>
     */
    private void criarAdminDeClinica(String email, String senha, UUID clinicaId) {
        salvarSeAusente(email, senha, Perfil.ADMIN_CLINICA,
                usuario -> clinicaRepository.findById(clinicaId).ifPresent(usuario::setClinica));
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
