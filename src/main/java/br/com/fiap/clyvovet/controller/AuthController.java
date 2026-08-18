package br.com.fiap.clyvovet.controller;

import br.com.fiap.clyvovet.dto.auth.LoginRequest;
import br.com.fiap.clyvovet.dto.auth.LoginResponse;
import br.com.fiap.clyvovet.dto.auth.RefreshRequest;
import br.com.fiap.clyvovet.dto.auth.RegistroRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioRequest;
import br.com.fiap.clyvovet.dto.auth.UsuarioResponse;
import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import br.com.fiap.clyvovet.service.AuthService;
import br.com.fiap.clyvovet.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * As rotas de /auth atendem a dois assuntos — autenticar e cadastrar — e por
 * isso conversam com dois services. O agrupamento aqui e de URL; a divisao de
 * responsabilidade esta na camada de baixo.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Login, renovacao de token e gestao de usuarios")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter access token e refresh token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar o access token a partir de um refresh token valido")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revogar o refresh token informado")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registrar")
    @Operation(summary = "Auto-cadastro de tutor. O perfil e sempre TUTOR")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @PostMapping("/usuarios")
    @Operation(summary = "Cadastrar usuario com perfil arbitrario (restrito a ADMIN)")
    public ResponseEntity<UsuarioResponse> criarUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuario autenticado")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.buscarPorId(usuario.getId()));
    }
}
