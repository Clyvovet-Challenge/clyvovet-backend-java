package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrada das telas: o login e a pagina inicial de cada perfil.
 *
 * <p>Fica no pacote {@code web}, e nao em {@code controller}, de proposito: o
 * {@code WebConfig} prefixa {@code /api/v1} so nos controllers daquele pacote.
 * Aqui as rotas ficam na raiz, que e onde a cadeia web as espera.</p>
 */
@Controller
public class InicioController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Cada perfil tem a sua area, e a raiz so decide para qual ir. O ADMIN entra
     * na do veterinario porque a cadeia web ja o deixa passar ali -- e a mesma
     * visao ampla que ele tem na API.
     */
    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return switch (usuario.getUsuario().getPerfil()) {
            case TUTOR -> "redirect:/tutor";
            case VETERINARIO, ADMIN -> "redirect:/veterinario";
            // A gestao da clinica nao tem tela nesta versao: segue pelo app.
            case ADMIN_CLINICA -> "inicio";
        };
    }

    @GetMapping("/tutor")
    public String areaDoTutor() {
        return "tutor/inicio";
    }

    @GetMapping("/veterinario")
    public String areaDoVeterinario() {
        return "veterinario/inicio";
    }
}
