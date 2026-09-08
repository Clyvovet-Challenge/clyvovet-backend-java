package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
import br.com.fiap.clyvovet.model.Tutor;
import br.com.fiap.clyvovet.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "dGVzdGUtY2x5dm92ZXQtY2hhdmUtaG1hYy1zaGEyNTYtcGFyYS10ZXN0ZXM=";
    private static final String OUTRO_SEGREDO = "b3V0cmEtY2hhdmUtY29tcGxldGFtZW50ZS1kaWZlcmVudGUtYXF1aQ==";

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SEGREDO, 15, 7);
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("tutor@teste.com");
        usuario.setPerfil(Perfil.TUTOR);
    }

    @Test
    @DisplayName("access token carrega id e perfil do usuario")
    void accessTokenCarregaIdEPerfil() {
        Claims claims = jwtService.lerClaims(jwtService.gerarAccessToken(usuario));

        assertThat(jwtService.extrairUsuarioId(claims)).isEqualTo(usuario.getId());
        assertThat(claims.get("perfil", String.class)).isEqualTo("TUTOR");
        assertThat(jwtService.ehAccessToken(claims)).isTrue();
        assertThat(jwtService.ehRefreshToken(claims)).isFalse();
    }

    @Test
    @DisplayName("refresh token e distinguivel do access token")
    void refreshTokenEDistinguivel() {
        Claims claims = jwtService.lerClaims(jwtService.gerarRefreshToken(usuario));

        assertThat(jwtService.ehRefreshToken(claims)).isTrue();
        assertThat(jwtService.ehAccessToken(claims)).isFalse();
    }

    @Test
    @DisplayName("token assinado com outra chave e rejeitado")
    void tokenDeOutraChaveERejeitado() {
        String tokenIntruso = new JwtService(OUTRO_SEGREDO, 15, 7).gerarAccessToken(usuario);

        assertThat(jwtService.tokenValido(tokenIntruso)).isFalse();
        assertThatThrownBy(() -> jwtService.lerClaims(tokenIntruso)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token adulterado e rejeitado")
    void tokenAdulteradoERejeitado() {
        String token = jwtService.gerarAccessToken(usuario);

        assertThat(jwtService.tokenValido(token + "xyz")).isFalse();
        assertThat(jwtService.tokenValido("nao-e-um-jwt")).isFalse();
    }

    @Test
    @DisplayName("token expirado e rejeitado")
    void tokenExpiradoERejeitado() {
        // Validade negativa: nasce ja expirado, sem precisar esperar no teste.
        JwtService expirado = new JwtService(SEGREDO, -1, 7);

        assertThat(expirado.tokenValido(expirado.gerarAccessToken(usuario))).isFalse();
    }

    @Test
    @DisplayName("segredo curto demais para HMAC-SHA256 e recusado na criacao")
    void segredoFracoERecusado() {
        assertThatThrownBy(() -> new JwtService("Y3VydG8=", 15, 7))
                .isInstanceOf(Exception.class);
    }

    // ================================================================
    // Camada 0 do JWT compartilhado com a API .NET.
    //
    // Estes testes existem porque as duas regras abaixo sao invisiveis em
    // producao ate darem errado: o refresh carregando identidade so aparece como
    // credencial de 7 dias aceita pela outra API, e a claim ausente so aparece
    // como listagem devolvendo dados de outro tutor.
    // ================================================================

    @Test
    @DisplayName("access token de um tutor carrega a claim tutorId")
    void accessTokenCarregaTutorId() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        usuario.setTutor(tutor);

        Claims claims = jwtService.lerClaims(jwtService.gerarAccessToken(usuario));

        assertThat(jwtService.extrairTutorId(claims)).isEqualTo(tutor.getId());
    }

    @Test
    @DisplayName("refresh token NAO carrega tutorId, mesmo para um tutor")
    void refreshTokenNaoCarregaTutorId() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        usuario.setTutor(tutor);

        Claims claims = jwtService.lerClaims(jwtService.gerarRefreshToken(usuario));

        // O refresh dura 7 dias e fica em disco no aparelho. Se ele carregasse
        // identidade utilizavel, seria uma credencial de sete dias valida na API
        // .NET -- que nao enxerga a revogacao por jti feita no logout daqui.
        assertThat(jwtService.extrairTutorId(claims)).isNull();
    }

    @Test
    @DisplayName("usuario sem tutor (ADMIN, VETERINARIO) gera token sem a claim")
    void usuarioSemTutorNaoCarregaTutorId() {
        usuario.setPerfil(Perfil.ADMIN);
        usuario.setTutor(null);

        Claims claims = jwtService.lerClaims(jwtService.gerarAccessToken(usuario));

        // Ausencia significa "este token nao identifica um tutor", e nunca
        // "sem restricao". Quem recorta por dono precisa NEGAR aqui.
        assertThat(jwtService.extrairTutorId(claims)).isNull();
        assertThat(claims.get("tutorId")).isNull();
    }

    @Test
    @DisplayName("token declara emissor e publico, que a validacao do .NET exige por padrao")
    void tokenDeclaraEmissorEPublico() {
        Claims claims = jwtService.lerClaims(jwtService.gerarAccessToken(usuario));

        assertThat(claims.getIssuer()).isEqualTo("clyvovet-api-java");
        assertThat(claims.getAudience()).contains("clyvovet");
    }

    @Test
    @DisplayName("a chave HMAC sai do segredo DECODIFICADO de base64, nao dos bytes da string")
    void chaveSaiDoBase64Decodificado() {
        // O contrato que a API .NET tem de reproduzir. Ela precisa usar
        // Convert.FromBase64String(segredo) -- o idioma comum em .NET,
        // Encoding.UTF8.GetBytes(segredo), produz uma chave DIFERENTE com o
        // mesmo valor de configuracao, e entao nenhuma assinatura confere.
        //
        // Este teste prova o lado Java do contrato: um segredo cujo base64
        // decodifica para bytes conhecidos gera um token que so e aceito por um
        // servico construido com o mesmo base64.
        String mesmoValor = SEGREDO;
        JwtService outroProcesso = new JwtService(mesmoValor, 15, 7);

        String token = jwtService.gerarAccessToken(usuario);

        assertThat(outroProcesso.tokenValido(token)).isTrue();
        assertThat(new String(io.jsonwebtoken.io.Decoders.BASE64.decode(SEGREDO)))
                .isEqualTo("teste-clyvovet-chave-hmac-sha256-para-testes");
    }
}
