package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.exception.RecursoNaoEncontradoException;
import br.com.fiap.clyvovet.security.SegurancaService;
import br.com.fiap.clyvovet.service.AnimalService;
import br.com.fiap.clyvovet.service.RacaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * "Meus pets": o tutor ve e cadastra os proprios pets pela tela.
 *
 * <p>Nenhuma regra mora aqui. Quem decide o que e um pet valido e o mesmo
 * {@link AnimalService} da API; o controller so traduz o formulario para ele.</p>
 *
 * <p>O dono do pet nunca vem do formulario: e o tutor da sessao. A listagem ja
 * volta recortada pelo tutor logado -- e o mesmo recorte da API --, e a rota
 * {@code /tutor/**} so deixa passar o perfil TUTOR.</p>
 */
@Controller
@RequestMapping("/tutor/pets")
@RequiredArgsConstructor
public class TutorPetController {

    /** Mais do que qualquer tutor tem de pets. */
    private static final Pageable PRIMEIRA_PAGINA = PageRequest.of(0, 50);

    private final AnimalService animalService;
    private final RacaService racaService;
    private final SegurancaService seguranca;

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("pets", animalService.listarTodos(null, null, PRIMEIRA_PAGINA).getContent());
        return "tutor/pets";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("pet", new PetForm());
        model.addAttribute("racas", racaService.listar(null));
        return "tutor/pet-novo";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("pet") PetForm pet, BindingResult erros,
                            Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                animalService.criar(pet.comoPedido(seguranca.recorte().tutorId()));
                redirect.addFlashAttribute("sucesso", "Pet cadastrado.");
                return "redirect:/tutor/pets";
            } catch (RecursoNaoEncontradoException racaDesconhecida) {
                // Formulario adulterado: o id nao e de nenhuma raca do catalogo.
                erros.rejectValue("racaId", "regra", "Escolha uma raça da lista");
            }
        }
        model.addAttribute("racas", racaService.listar(null));
        return "tutor/pet-novo";
    }

    @GetMapping("/{id}/excluir")
    @PreAuthorize("@seguranca.ehDonoOuAdministrador(#id)")
    public String confirmarExclusao(@PathVariable UUID id, Model model) {
        model.addAttribute("pet", animalService.buscarPorId(id));
        return "tutor/pet-excluir";
    }

    /**
     * Exclui o pet, se ele nao tem prontuario.
     *
     * <p>A regra nao esta no service: quem recusa pet com atendimento ou laudo e o
     * banco, por chave estrangeira (ADR 003; a V20 deixou so essas duas). Aqui a
     * recusa vira uma explicacao na propria tela, em vez de uma pagina de erro.</p>
     */
    @PostMapping("/{id}/excluir")
    @PreAuthorize("@seguranca.ehDonoOuAdministrador(#id)")
    public String excluir(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            animalService.deletar(id);
        } catch (DataIntegrityViolationException temProntuario) {
            model.addAttribute("pet", animalService.buscarPorId(id));
            model.addAttribute("recusa", "Este pet tem histórico clínico registrado (atendimentos ou laudos) "
                    + "e não pode ser excluído.");
            return "tutor/pet-excluir";
        }
        redirect.addFlashAttribute("sucesso", "Pet excluído.");
        return "redirect:/tutor/pets";
    }
}
