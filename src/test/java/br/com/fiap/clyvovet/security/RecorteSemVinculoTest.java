package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Usuario;
import br.com.fiap.clyvovet.repository.UsuarioRepository;
import br.com.fiap.clyvovet.support.SeedV2;
import br.com.fiap.clyvovet.support.TesteDeApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O usuario que existe, autentica, e nao esta ligado a ninguem.
 *
 * O recorte desse usuario era (null, null), que e exatamente o do ADMIN: nulo
 * significa "sem recorte nesta dimensao" na consulta. O cadastro incompleto nao
 * tirava acesso — DAVA. Hoje a falta de vinculo vira um id que nenhuma linha
 * carrega, e a mesma clausula devolve pagina vazia.
 *
 * A LINHA E GRAVADA DIRETO NO REPOSITORIO, de proposito. POST /auth/usuarios
 * agora recusa perfil sem vinculo — {@code CicloDeSessaoTest} cobre essa metade
 * — e e justamente por isso que a porta da frente nao serve aqui: o que este
 * teste mede e o que acontece quando a linha existe MESMO ASSIM, vinda de uma
 * migration, de uma correcao manual no banco ou de um bug futuro. As duas
 * defesas sao independentes, e esta e a que vale por ultimo.
 */
class RecorteSemVinculoTest extends TesteDeApi {

    private static final String SENHA = "senha12345";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("veterinário sem clínica vinculada não lê atendimento nenhum")
    void veterinarioSemClinicaNaoLeAtendimentos() throws Exception {
        String orfao = tokenSemVinculo(Perfil.VETERINARIO);

        // A comparacao com o ADMIN e o que da sentido ao zero: sem ela, uma
        // base vazia faria o teste passar sozinho.
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos", tokenAdmin()))).isPositive();
        assertThat(totalDe(buscar("/api/v1/eventos-clinicos", orfao))).isZero();
    }

    @Test
    @DisplayName("veterinário sem clínica vinculada não registra atendimento em clínica alguma")
    void veterinarioSemClinicaNaoRegistraAtendimento() throws Exception {
        String orfao = tokenSemVinculo(Perfil.VETERINARIO);

        // Este era o lado de ESCRITA do mesmo buraco: a guarda de clinica
        // propria comparava com null, e null passava por qualquer clinica.
        criar("/api/v1/eventos-clinicos", orfao, """
                {"data":"%s","hora":"10:00","descricao":"Atendimento sem lastro",
                 "tipoEvento":"CONSULTA","veterinarioId":"%s","animalId":"%s","clinicaId":"%s"}"""
                .formatted(LocalDate.now().minusDays(1), SeedV2.VET_RAFAEL_DA_PETMED,
                        SeedV2.ANIMAL_MIMI_DA_MARIA, SeedV2.CLINICA_PETMED))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("tutor sem tutor vinculado não lê animal nenhum")
    void tutorSemVinculoNaoLeAnimais() throws Exception {
        String orfao = tokenSemVinculo(Perfil.TUTOR);

        // A outra dimensao do mesmo record. O cadastro do animal e nivel 0 para
        // o corpo clinico, mas o TUTOR so alcanca os proprios — e sem vinculo
        // ele nao tem "proprios".
        assertThat(totalDe(buscar("/api/v1/animais", tokenAdmin()))).isPositive();
        assertThat(totalDe(buscar("/api/v1/animais", orfao))).isZero();
    }

    // ------------------------------------------------------------------

    /**
     * Grava um usuario do perfil pedido, sem tutor nem veterinario, e devolve o
     * token dele.
     *
     * E-mail unico porque nao ha DELETE de usuario para limpar depois, e a
     * suite roda tambem contra um banco que persiste entre execucoes.
     */
    private String tokenSemVinculo(Perfil perfil) throws Exception {
        String email = "sem-vinculo-" + UUID.randomUUID() + "@teste.com";

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);

        return token(email, SENHA);
    }
}
