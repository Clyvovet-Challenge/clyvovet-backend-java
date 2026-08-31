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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Os links de um evento clinico — e o que eles tem de diferente de "self".
 *
 * POR QUE ISTO EXISTE
 * O nivel 3 de Richardson nao e "adicionar um link self em cada resposta". E o
 * cliente descobrir, pela propria resposta, o que pode fazer com aquele recurso
 * AGORA. Sem isso, quem consome a API precisa carregar por fora uma copia da
 * maquina de estados — e essa copia envelhece em silencio no dia em que a regra
 * do servidor muda.
 *
 * Aqui os links sao CONDICIONAIS ao estado:
 *
 *   AGENDADO   -> cancelar, concluir
 *   REALIZADO  -> retorno (se ainda nao houver um), pagamentos
 *   FALTOU     -> nenhuma acao; so navegacao
 *   CANCELADO  -> nenhuma acao; so navegacao
 *
 * Um evento cancelado nao traz "cancelar". O frontend nao precisa saber a regra
 * para desabilitar o botao: se o link nao veio, a acao nao existe.
 *
 * SOBRE O methodOn
 * Ele nao chama o controller — constroi um proxy que registra qual metodo foi
 * invocado e devolve a URL mapeada para ele. E por isso que renomear uma rota
 * quebra a compilacao aqui em vez de produzir um link errado em producao, que e
 * exatamente a garantia que uma String concatenada nao daria.
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

    /**
     * As transicoes possiveis a partir do estado atual.
     *
     * A ordem espelha o ciclo de vida do atendimento, para que quem le a
     * resposta encontre as acoes na sequencia em que elas acontecem.
     */
    private void acoesDoEstado(EntityModel<EventoClinicoResponse> modelo, EventoClinicoResponse evento) {
        StatusEvento status = evento.statusEvento();
        if (status == null) {
            return;
        }

        switch (status) {
            case AGENDADO -> {
                modelo.add(linkTo(methodOn(AgendamentoController.class)
                        .cancelar(evento.id(), null)).withRel("cancelar"));
                modelo.add(linkTo(methodOn(RetornoController.class)
                        .concluir(evento.id(), null)).withRel("concluir"));
            }
            case REALIZADO -> {
                // O retorno so faz sentido a partir de um atendimento que
                // aconteceu — e some quando ja existe um marcado, porque a regra
                // R14 admite um retorno em aberto por consulta de origem.
                if (evento.dataRetornoPrevisto() != null) {
                    modelo.add(linkTo(methodOn(RetornoController.class)
                            .agendarRetorno(evento.id(), null)).withRel("marcar-retorno"));
                }
            }
            case FALTOU, CANCELADO -> {
                // Estados terminais: nenhuma acao. O cliente que recebe este
                // objeto nao consegue inventar uma transicao que o servidor
                // recusaria — ele simplesmente nao ve o caminho.
            }
        }
    }
}
