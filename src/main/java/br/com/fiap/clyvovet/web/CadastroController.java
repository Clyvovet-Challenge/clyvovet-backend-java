package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * A tela publica "Criar conta".
 *
 * <p>Usa o mesmo {@link UsuarioService#registrar} do {@code POST
 * /api/v1/auth/registrar}: uma regra de cadastro so, com duas portas. Por isso o
 * perfil e sempre TUTOR e o tutor nasce junto com o usuario.</p>
 *
 * <p>No sucesso, a pessoa vai para o login com um aviso, e nao entra direto. Entrar
 * exige passar pelo mesmo caminho de qualquer outro acesso (bloqueio por tentativas,
 * sessao, destino pos-login), e duplica-lo aqui abriria uma segunda porta de entrada
 * para manter igual.</p>
 */
@Controller
@RequiredArgsConstructor
public class CadastroController {

    private final UsuarioService usuarioService;

    @GetMapping("/cadastro")
    public String tela(Model model) {
        model.addAttribute("cadastro", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String criar(@Valid @ModelAttribute("cadastro") CadastroForm cadastro,
                        BindingResult erros) {
        if (!erros.hasFieldErrors("senha") && !erros.hasFieldErrors("confirmacaoDaSenha")
                && !cadastro.senhasConferem()) {
            erros.rejectValue("confirmacaoDaSenha", "regra", "As senhas não conferem");
        }
        if (!erros.hasErrors()) {
            try {
                usuarioService.registrar(cadastro.comoRegistro());
                return "redirect:/login?conta-criada";
            } catch (RegraDeNegocioException recusa) {
                Formularios.rejeitar(erros, recusa);
            }
        }
        return "cadastro";
    }
}
