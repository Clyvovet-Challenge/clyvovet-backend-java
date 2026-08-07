package br.com.fiap.clyvovet.security;

import br.com.fiap.clyvovet.model.Perfil;
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
}
