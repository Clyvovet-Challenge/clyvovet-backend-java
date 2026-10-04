package br.com.fiap.clyvovet.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    @DisplayName("usuario novo nasce ativo, sem bloqueio e sem vinculo")
    void novoUsuario() {
        Usuario usuario = Usuario.novo("pessoa@clyvovet.com", "$2a$10$hashBCrypt", Perfil.TUTOR);

        assertThat(usuario.getId()).isNull();
        assertThat(usuario.getEmail()).isEqualTo("pessoa@clyvovet.com");
        assertThat(usuario.getSenha()).isEqualTo("$2a$10$hashBCrypt");
        assertThat(usuario.getPerfil()).isEqualTo(Perfil.TUTOR);
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getTentativasFalhas()).isZero();
        assertThat(usuario.getBloqueadoAte()).isNull();
        assertThat(usuario.getTutor()).isNull();
        assertThat(usuario.getVeterinario()).isNull();
        assertThat(usuario.getClinica()).isNull();
    }
}
