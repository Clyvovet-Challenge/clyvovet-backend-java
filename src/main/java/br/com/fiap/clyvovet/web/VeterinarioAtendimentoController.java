package br.com.fiap.clyvovet.web;

import br.com.fiap.clyvovet.dto.eventoClinico.ConclusaoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.ConclusaoResponse;
import br.com.fiap.clyvovet.dto.eventoClinico.RetornoRequest;
import br.com.fiap.clyvovet.exception.RegraDeNegocioException;
import br.com.fiap.clyvovet.model.Desfecho;
import br.com.fiap.clyvovet.service.EventoClinicoService;
import br.com.fiap.clyvovet.service.RetornoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * O fluxo R pela tela: a veterinaria fecha o atendimento, marca o retorno e ve
 * quem nao voltou.
 *
 * <p>Como no {@link TutorAgendamentoController}, nenhuma regra mora aqui. Quem
 * recusa concluir o futuro, o retorno antes da consulta ou o horario sem grade e
 * o {@link RetornoService}, o mesmo da API; e quem recorta pela clinica da
 * veterinaria e o proprio service, via {@code SegurancaService.recorte()}.</p>
 */
@Controller
@RequestMapping("/veterinario")
@RequiredArgsConstructor
public class VeterinarioAtendimentoController {

    /**
     * Uma semana para tras e uma para frente: o que ainda falta concluir e o que
     * esta para chegar. Mais que isso vira lista de rolar, e o historico
     * completo ja tem a sua tela no app.
     */
    private static final int DIAS_DA_JANELA = 7;

    private static final Pageable ATENDIMENTOS_DA_JANELA =
            PageRequest.of(0, 100, Sort.by("data", "hora"));

    private final EventoClinicoService eventoClinicoService;
    private final RetornoService retornoService;

    @GetMapping("/atendimentos")
    public String atendimentos(Model model) {
        LocalDate hoje = LocalDate.now();
        model.addAttribute("hoje", hoje);
        model.addAttribute("atendimentos", eventoClinicoService.listarTodos(null, null, null, null,
                hoje.minusDays(DIAS_DA_JANELA), hoje.plusDays(DIAS_DA_JANELA), ATENDIMENTOS_DA_JANELA).getContent());
        return "veterinario/atendimentos";
    }

    @GetMapping("/atendimentos/{id}/concluir")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String telaDeConclusao(@PathVariable UUID id, Model model) {
        model.addAttribute("conclusao", new ConclusaoRequest());
        return mostrarConclusao(id, model);
    }

    @PostMapping("/atendimentos/{id}/concluir")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String concluir(@PathVariable UUID id,
                           @Valid @ModelAttribute("conclusao") ConclusaoRequest conclusao,
                           BindingResult erros, Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                ConclusaoResponse concluido = retornoService.concluir(id, conclusao);
                redirect.addFlashAttribute("sucesso", "Atendimento concluído.");
                // O aviso de peso (R7) nao bloqueia, mas precisa ser visto: e o
                // mesmo motivo de ele voltar no corpo da resposta da API.
                redirect.addFlashAttribute("aviso", concluido.aviso());
                return "redirect:/veterinario/atendimentos";
            } catch (RegraDeNegocioException recusa) {
                Formularios.rejeitar(erros, recusa);
            }
        }
        return mostrarConclusao(id, model);
    }

    @GetMapping("/atendimentos/{id}/retorno")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String telaDeRetorno(@PathVariable UUID id, Model model) {
        model.addAttribute("retorno", new RetornoRequest());
        return mostrarRetorno(id, model);
    }

    @PostMapping("/atendimentos/{id}/retorno")
    @PreAuthorize("@seguranca.podeAcessarEvento(#id)")
    public String marcarRetorno(@PathVariable UUID id,
                                @Valid @ModelAttribute("retorno") RetornoRequest retorno,
                                BindingResult erros, Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                retornoService.agendarRetorno(id, retorno);
                redirect.addFlashAttribute("sucesso", "Retorno marcado.");
                return "redirect:/veterinario/atendimentos";
            } catch (RegraDeNegocioException recusa) {
                Formularios.rejeitar(erros, recusa);
            }
        }
        return mostrarRetorno(id, model);
    }

    /** A lista sobre a qual a clinica age: cada linha vira uma ligacao. */
    @GetMapping("/retornos-vencidos")
    public String retornosVencidos(Model model) {
        model.addAttribute("vencidos", retornoService.vencidos(null, null));
        return "veterinario/retornos-vencidos";
    }

    private String mostrarConclusao(UUID id, Model model) {
        model.addAttribute("evento", eventoClinicoService.buscarPorId(id));
        model.addAttribute("desfechos", Desfecho.values());
        return "veterinario/atendimento-concluir";
    }

    private String mostrarRetorno(UUID id, Model model) {
        model.addAttribute("evento", eventoClinicoService.buscarPorId(id));
        return "veterinario/atendimento-retorno";
    }
}
