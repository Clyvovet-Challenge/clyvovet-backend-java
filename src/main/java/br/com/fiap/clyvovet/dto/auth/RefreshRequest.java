package br.com.fiap.clyvovet.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RefreshRequest {

    @NotBlank(message = "Refresh token e obrigatorio")
    private String refreshToken;
}
