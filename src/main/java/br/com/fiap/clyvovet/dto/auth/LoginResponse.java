package br.com.fiap.clyvovet.dto.auth;

import br.com.fiap.clyvovet.model.Perfil;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiraEmSegundos,
        Perfil perfil
) {
    public static LoginResponse de(String accessToken, String refreshToken, long expiraEm, Perfil perfil) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiraEm, perfil);
    }
}
