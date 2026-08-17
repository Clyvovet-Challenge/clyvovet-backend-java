package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.auth.UsuarioResponse;
import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.model.Veterinario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    private static Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("pessoa@clyvovet.com");
        usuario.setSenha("$2a$10$hashBCryptQueNaoPodeVazar");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    @Test
    @DisplayName("usuario de tutor expoe o vinculo com o tutor")
    void usuarioDeTutor() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setNome("Lucas M. Santos");
        Usuario usuario = usuario(Perfil.TUTOR);
        usuario.setTutor(tutor);

        UsuarioResponse response = mapper.toResponse(usuario);

        assertThat(response.perfil()).isEqualTo(Perfil.TUTOR);
        assertThat(response.ativo()).isTrue();
        assertThat(response.tutorId()).isEqualTo(tutor.getId());
        assertThat(response.tutorNome()).isEqualTo("Lucas M. Santos");
        assertThat(response.veterinarioId()).isNull();
        assertThat(response.veterinarioNome()).isNull();
    }

    @Test
    @DisplayName("usuario de veterinario expoe o vinculo com o veterinario")
    void usuarioDeVeterinario() {
        Veterinario veterinario = new Veterinario();
        veterinario.setId(UUID.randomUUID());
        veterinario.setNome("Camila Ferreira");
        Usuario usuario = usuario(Perfil.VETERINARIO);
        usuario.setVeterinario(veterinario);

        UsuarioResponse response = mapper.toResponse(usuario);

        assertThat(response.veterinarioId()).isEqualTo(veterinario.getId());
        assertThat(response.veterinarioNome()).isEqualTo("Camila Ferreira");
        assertThat(response.tutorId()).isNull();
    }

    @Test
    @DisplayName("admin sem vinculo nenhum nao estoura")
    void adminSemVinculo() {
        UsuarioResponse response = mapper.toResponse(usuario(Perfil.ADMIN));

        assertThat(response.tutorId()).isNull();
        assertThat(response.tutorNome()).isNull();
        assertThat(response.veterinarioId()).isNull();
        assertThat(response.veterinarioNome()).isNull();
    }

    /**
     * A senha nao e omitida por disciplina de quem escreve o mapper: ela nao
     * existe no record de resposta. Este teste falha se alguem a acrescentar.
     */
    @Test
    @DisplayName("a resposta nao tem campo de senha")
    void respostaNaoTemSenha() {
        assertThat(Arrays.stream(UsuarioResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("senha", "password");
    }
}
