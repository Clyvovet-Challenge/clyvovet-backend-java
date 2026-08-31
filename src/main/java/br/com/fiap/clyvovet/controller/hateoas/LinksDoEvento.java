package br.com.fiap.clyvovet.controller.hateoas;

import br.com.fiap.clyvovet.controller.AgendamentoController;
import br.com.fiap.clyvovet.controller.AnimalController;
import br.com.fiap.clyvovet.controller.EventoClinicoController;
import br.com.fiap.clyvovet.controller.HistoricoController;
import br.com.fiap.clyvovet.controller.RetornoController;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.StatusEvento;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Links de um evento clinico, condicionais ao estado:
 *
 *   AGENDADO   -> cancelar; concluir, se a data ja chegou
 *   REALIZADO  -> concluir, enquanto o prontuario nao foi fechado;
 *                 marcar-retorno, se houver retorno previsto
 *   FALTOU     -> so navegacao
 *   CANCELADO  -> so navegacao
 *
 * Se o link nao veio, a acao nao existe — o cliente nao precisa conhecer a
 * maquina de estados para desabilitar o botao.
 */
@Component
public class LinksDoEvento {

    public EntityModel<EventoClinicoResponse> comLinks(EventoClinicoResponse evento) {
        EntityModel<EventoClinicoResponse> modelo = EntityModel.of(evento);

        modelo.add(linkTo(methodOn(EventoClinicoController.class)
                .buscarPorId(evento.id())).withSelfRel());

        if (evento.animalId() != null) {
            modelo.add(linkTo(methodOn(AnimalController.class)
                    .buscarPorId(evento.animalId())).withRel("animal"));
            modelo.add(linkTo(methodOn(HistoricoController.class)
                    .historico(evento.animalId())).withRel("historico"));
        }

        acoesDoEstado(modelo, evento);
        return modelo;
    }

    /** As transicoes possiveis a partir do estado atual. */
    private void acoesDoEstado(EntityModel<EventoClinicoResponse> modelo, EventoClinicoResponse evento) {
        StatusEvento status = evento.statusEvento();
        if (status == null) {
            return;
        }

        switch (status) {
            case AGENDADO -> {
                modelo.add(linkTo(methodOn(AgendamentoController.class)
                        .cancelar(evento.id(), null)).withRel("cancelar"));
                // Concluir o futuro seria registrar consulta que nao houve (R2).
                if (!evento.data().isAfter(LocalDate.now())) {
                    modelo.add(linkTo(methodOn(RetornoController.class)
                            .concluir(evento.id(), null)).withRel("concluir"));
                }
            }
            case REALIZADO -> {
                // O atendimento lancado retroativamente nasce REALIZADO (R1) e
                // ainda espera peso e desfecho: quem responde se cabe concluir
                // e concluidoEm, nao o status.
                if (evento.concluidoEm() == null) {
                    modelo.add(linkTo(methodOn(RetornoController.class)
                            .concluir(evento.id(), null)).withRel("concluir"));
                }
                if (evento.dataRetornoPrevisto() != null) {
                    modelo.add(linkTo(methodOn(RetornoController.class)
                            .agendarRetorno(evento.id(), null)).withRel("marcar-retorno"));
                }
            }
            // Estados terminais: so navegacao.
            case FALTOU, CANCELADO -> { }
        }
    }
}
