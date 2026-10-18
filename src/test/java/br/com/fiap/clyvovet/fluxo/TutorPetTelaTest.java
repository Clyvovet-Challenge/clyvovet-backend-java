package br.com.fiap.clyvovet.fluxo;

import br.com.fiap.clyvovet.dto.raca.RacaResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.EspecieAnimal;
import br.com.fiap.clyvovet.repository.AnimalRepository;
import br.com.fiap.clyvovet.repository.TutorRepository;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.service.RacaService;
import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeTela;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * A tela "Meus pets": o tutor ve e cadastra os proprios pets no navegador.
 *
 * <p>As regras de acesso sao as do {@code AnimalService} e da API. O que estes
 * testes fixam e a parte que so a tela tem: o dono do pet NAO vem do formulario
 * (e sempre o tutor logado), a raca vem do catalogo e uma recusa volta como
 * mensagem ao lado do campo.</p>
 */
class TutorPetTelaTest extends TesteDeTela {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RacaService racaService;

    private final List<String> emailsCriados = new ArrayList<>();

    /** A suite compartilha o banco: apaga os pets criados antes do dono deles. */
    @AfterEach
    void apagarOQueFoiCriado() {
        animalRepository.findAll().stream()
                .filter(animal -> animal.getNome().startsWith("Teste-tela-"))
                .forEach(animalRepository::delete);
        for (String email : emailsCriados) {
            usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete);
            tutorRepository.findAll().stream()
                    .filter(tutor -> email.equals(tutor.getEmail()))
                    .forEach(tutorRepository::delete);
        }
    }

    private String nomeNovo() {
        return "Teste-tela-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private RacaResponse umaRacaDeCao() {
        return racaService.listar(EspecieAnimal.CAO).get(0);
    }

    private Animal pet(String nome) {
        return animalRepository.findAll().stream()
                .filter(animal -> nome.equals(animal.getNome())).findFirst().orElseThrow();
    }

    private ResultActions cadastrar(String quem, String nome, String... extras) throws Exception {
        // Os extras SUBSTITUEM o padrao do mesmo campo: o mesmo parametro duas vezes
        // chegaria como lista, e o Spring usaria o primeiro valor.
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nome", nome);
        campos.put("racaId", umaRacaDeCao().id().toString());
        campos.put("porte", "MEDIO");
        campos.put("cor", "Caramelo");
        campos.put("sexo", "MACHO");
        campos.put("dataNascimento", "2022-03-12");
        for (int i = 0; i < extras.length; i += 2) {
            campos.put(extras[i], extras[i + 1]);
        }
        List<String> paresEmOrdem = new ArrayList<>();
        campos.forEach((campo, valor) -> {
            paresEmOrdem.add(campo);
            paresEmOrdem.add(valor);
        });
        return enviarFormulario("/tutor/pets", comoUsuario(quem), paresEmOrdem.toArray(String[]::new));
    }

    @Test
    @DisplayName("a lista mostra so os pets do proprio tutor")
    void listaSoOsPetsDoTutor() throws Exception {
        abrir("/tutor/pets", LUCAS)
                .andExpect(status().isOk())
                .andExpect(view().name("tutor/pets"))
                .andExpect(content().string(containsString("Bolinha")))
                .andExpect(content().string(not(containsString("Mimi"))));
    }

    @Test
    @DisplayName("o formulario traz a lista de racas do catalogo, com 'sem raca definida'")
    void formularioTrazOCatalogo() throws Exception {
        abrir("/tutor/pets/novo", LUCAS)
                .andExpect(status().isOk())
                .andExpect(view().name("tutor/pet-novo"))
                .andExpect(content().string(containsString("name=\"racaId\"")))
                .andExpect(content().string(containsString("Sem raça definida")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("name=\"tutorId\""))));
    }

    @Test
    @DisplayName("cadastro valido grava o pet no tutor logado e volta a lista com a confirmacao")
    void cadastraNoTutorLogado() throws Exception {
        String nome = nomeNovo();

        cadastrar(LUCAS, nome)
                .andExpect(redirectedUrl("/tutor/pets"))
                .andExpect(flash().attributeExists("sucesso"));

        Animal gravado = pet(nome);
        assertThat(gravado.getTutor().getId().toString()).isEqualTo(SeedV2.TUTOR_LUCAS);
        assertThat(gravado.getRacaDoCatalogo().getId()).isEqualTo(umaRacaDeCao().id());
        assertThat(gravado.getEspecie()).isEqualTo(EspecieAnimal.CAO.rotulo());
    }

    @Test
    @DisplayName("um tutorId forjado no formulario e ignorado: o dono e sempre quem esta logado")
    void tutorIdForjadoEIgnorado() throws Exception {
        String nome = nomeNovo();

        cadastrar(LUCAS, nome, "tutorId", SeedV2.TUTOR_MARIA)
                .andExpect(redirectedUrl("/tutor/pets"));

        assertThat(pet(nome).getTutor().getId().toString()).isEqualTo(SeedV2.TUTOR_LUCAS);
    }

    @Test
    @DisplayName("dados invalidos voltam ao formulario com o erro ao lado do campo, sem gravar")
    void dadosInvalidosMostramErroNoCampo() throws Exception {
        cadastrar(LUCAS, "Al")
                .andExpect(status().isOk())
                .andExpect(view().name("tutor/pet-novo"))
                .andExpect(model().attributeHasFieldErrors("pet", "nome"));
    }

    @Test
    @DisplayName("raca que nao existe no catalogo volta como erro no campo raca, e nao como 500")
    void racaInexistenteViraErroNoCampo() throws Exception {
        enviarFormulario("/tutor/pets", comoUsuario(LUCAS),
                "nome", nomeNovo(),
                "racaId", UUID.randomUUID().toString(),
                "porte", "MEDIO",
                "cor", "Caramelo",
                "sexo", "MACHO",
                "dataNascimento", "2022-03-12")
                .andExpect(status().isOk())
                .andExpect(view().name("tutor/pet-novo"))
                .andExpect(model().attributeHasFieldErrors("pet", "racaId"));
    }

    @Test
    @DisplayName("data de nascimento no futuro e recusada no campo data")
    void nascimentoNoFuturoERecusado() throws Exception {
        cadastrar(LUCAS, nomeNovo(), "dataNascimento", "2999-01-01")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("pet", "dataNascimento"));
    }

    @Test
    @DisplayName("veterinario nao entra na area do tutor")
    void veterinarioNaoEntra() throws Exception {
        abrir("/tutor/pets", VETERINARIA).andExpect(status().isForbidden());
        abrir("/tutor/pets/novo", VETERINARIA).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("quem nao tem pet ve o convite para cadastrar o primeiro, na lista e no agendamento")
    void tutorNovoVeOConvite() throws Exception {
        String email = "sem-pet-" + UUID.randomUUID() + "@exemplo.com";
        emailsCriados.add(email);
        enviarFormulario("/cadastro",
                "nome", "Tutor Sem Pet", "email", email,
                "senha", "Senha-Forte-2026", "confirmacaoDaSenha", "Senha-Forte-2026")
                .andExpect(redirectedUrl("/login?conta-criada"));

        abrir("/tutor/pets", email)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/tutor/pets/novo")));
        abrir("/tutor/agendamentos/novo", email)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastre seu primeiro pet")))
                .andExpect(content().string(containsString("/tutor/pets/novo")));
    }

    @Test
    @DisplayName("a home do tutor leva a Meus pets")
    void homeTemLinkParaMeusPets() throws Exception {
        abrir("/tutor", LUCAS)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/tutor/pets")));
    }
}
