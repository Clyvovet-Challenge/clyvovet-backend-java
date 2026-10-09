package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.dto.agendamento.AgendamentoRequest;
import br.com.fiap.clyvovet.dto.agendamento.CancelamentoRequest;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.service.AgendamentoService;
import br.com.fiap.clyvovet.service.AnimalService;
import br.com.fiap.clyvovet.service.ClinicaService;
import br.com.fiap.clyvovet.service.EventoClinicoService;
import br.com.fiap.clyvovet.service.ServicoService;
import br.com.fiap.clyvovet.service.VeterinarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O fluxo A pela tela: o tutor ve, marca e cancela as proprias consultas.
 *
 * <p>Nenhuma regra mora aqui. Quem decide se o horario esta livre, se o pet e
 * do tutor ou se ainda da para cancelar e o mesmo service da API; o controller
 * so traduz o formulario para ele e a recusa dele para a tela.</p>
 *
 * <p>O caminho do tutor tem tres passos: escolher pet, clinica e servico; ver as
 * vagas da semana; confirmar uma delas. A confirmacao e uma tela propria porque
 * e nela que o tutor decide o consentimento do historico -- e consentimento
 * pre-marcado nao e consentimento.</p>
 */
@Controller
@RequestMapping("/tutor/agendamentos")
@RequiredArgsConstructor
public class TutorAgendamentoController {

    /** Uma semana de vagas por vez: cabe na tela sem rolar demais. */
    private static final int DIAS_DE_VAGAS = 7;

    /** Mais do que qualquer tutor tem de pets ou de agendamentos ativos. */
    private static final Pageable PRIMEIRA_PAGINA = PageRequest.of(0, 50);

    private final AgendamentoService agendamentoService;
    private final AnimalService animalService;
    private final ClinicaService clinicaService;
    private final ServicoService servicoService;
    private final VeterinarioService veterinarioService;
    private final EventoClinicoService eventoClinicoService;

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("agendamentos", agendamentoService.meus(PRIMEIRA_PAGINA).getContent());
        return "tutor/agendamentos";
    }

    /**
     * Os passos 1 e 2 na mesma tela: cada escolha volta como parametro da URL, e
     * a tela mostra o que ja da para mostrar com elas.
     */
    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) UUID animalId,
                       @RequestParam(required = false) UUID clinicaId,
                       @RequestParam(required = false) UUID servicoId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                       Model model) {
        // Os pets ja vem recortados pelo tutor logado: e o mesmo recorte da API.
        model.addAttribute("pets", animalService.listarTodos(null, null, PRIMEIRA_PAGINA).getContent());
        model.addAttribute("clinicas", clinicaService.listarTodos(null, null, null, PRIMEIRA_PAGINA).getContent());
        model.addAttribute("animalId", animalId);
        model.addAttribute("clinicaId", clinicaId);
        model.addAttribute("servicoId", servicoId);

        if (clinicaId != null) {
            model.addAttribute("servicos", servicoService.daClinica(clinicaId, false));
        }
        if (animalId != null && clinicaId != null && servicoId != null) {
            LocalDate inicio = de != null && de.isAfter(LocalDate.now()) ? de : LocalDate.now();
            model.addAttribute("de", inicio);
            model.addAttribute("semanaSeguinte", inicio.plusDays(DIAS_DE_VAGAS));
            model.addAttribute("vagas",
                    agendamentoService.vagas(servicoId, null, inicio, inicio.plusDays(DIAS_DE_VAGAS - 1)));
        }
        return "tutor/agendamento-novo";
    }

    /** Passo 3: a vaga escolhida, com o resumo e a caixa de consentimento. */
    @GetMapping("/confirmar")
    @PreAuthorize("@seguranca.podeAcessarAnimal(#agendamento.animalId)")
    public String confirmar(@ModelAttribute("agendamento") AgendamentoRequest agendamento,
                            @RequestParam UUID clinicaId, Model model) {
        resumir(agendamento, clinicaId, model);
        return "tutor/agendamento-confirmar";
    }

    @PostMapping
    @PreAuthorize("@seguranca.podeAcessarAnimal(#agendamento.animalId)")
    public String agendar(@Valid @ModelAttribute("agendamento") AgendamentoRequest agendamento,
                          BindingResult erros, @RequestParam UUID clinicaId,
                          Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                agendamentoService.agendar(agendamento);
                redirect.addFlashAttribute("sucesso", "Consulta agendada.");
                return "redirect:/tutor/agendamentos";
            } catch (RegraDeNegocioException recusa) {
                Formularios.rejeitar(erros, recusa);
            }
        }
        resumir(agendamento, clinicaId, model);
        return "tutor/agendamento-confirmar";
    }

    @GetMapping("/{id}/cancelar")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String telaDeCancelamento(@PathVariable UUID id, Model model) {
        model.addAttribute("evento", eventoClinicoService.buscarPorId(id));
        model.addAttribute("cancelamento", new CancelamentoRequest());
        return "tutor/agendamento-cancelar";
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String cancelar(@PathVariable UUID id,
                           @Valid @ModelAttribute("cancelamento") CancelamentoRequest cancelamento,
                           BindingResult erros, Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                agendamentoService.cancelar(id, cancelamento.getMotivo());
                redirect.addFlashAttribute("sucesso", "Agendamento cancelado.");
                return "redirect:/tutor/agendamentos";
            } catch (RegraDeNegocioException recusa) {
                Formularios.rejeitar(erros, recusa);
            }
        }
        model.addAttribute("evento", eventoClinicoService.buscarPorId(id));
        return "tutor/agendamento-cancelar";
    }

    /**
     * Os nomes do que o tutor escolheu. O formulario carrega so os ids; mostrar
     * "Bolinha, Consulta de rotina, Dra. Camila" e o que deixa ele conferir antes
     * de confirmar.
     *
     * <p>Os ids vem de campos ocultos e podem chegar vazios num formulario
     * adulterado. Ai o @NotNull ja marcou o erro, e o resumo mostra so o que
     * existe -- buscar por id nulo derrubaria a tela com um 500.</p>
     */
    private void resumir(AgendamentoRequest agendamento, UUID clinicaId, Model model) {
        model.addAttribute("clinicaId", clinicaId);
        if (agendamento.getAnimalId() != null) {
            model.addAttribute("pet", animalService.buscarPorId(agendamento.getAnimalId()));
        }
        if (agendamento.getVeterinarioId() != null) {
            model.addAttribute("veterinario", veterinarioService.buscarPorId(agendamento.getVeterinarioId()));
        }
        model.addAttribute("servico", servicoService.daClinica(clinicaId, false).stream()
                .filter(servico -> servico.id().equals(agendamento.getServicoId()))
                .findFirst()
                .orElse(null));
    }
}
