package br.com.fiap.clyvovet.controller.hateoas;

import br.com.fiap.clyvovet.controller.AnimalController;
import br.com.fiap.clyvovet.controller.HistoricoController;
import br.com.fiap.clyvovet.controller.TutorController;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Os links de um animal.
 *
 * O caminho que interessa e "historico": ele e o que liga o cadastro do pet ao
 * objeto que de fato importa clinicamente, e que ate aqui so era alcancavel por
 * quem ja soubesse que a rota existia.
 *
 * "acessos" so aparece para quem pode ve-lo. E a mesma regra do
 * SegurancaService, aplicada aqui para que o link nao prometa uma porta que
 * responderia 403 — um link que o cliente nao pode seguir e pior que link
 * nenhum: ele desenha um botao que so falha depois do clique.
 */
@Component
public class LinksDoAnimal {

    private final br.com.fiap.clyvovet.security.SegurancaService seguranca;

    public LinksDoAnimal(br.com.fiap.clyvovet.security.SegurancaService seguranca) {
        this.seguranca = seguranca;
    }

    public EntityModel<AnimalResponse> comLinks(AnimalResponse animal) {
        EntityModel<AnimalResponse> modelo = EntityModel.of(animal);

        modelo.add(linkTo(methodOn(AnimalController.class)
                .buscarPorId(animal.id())).withSelfRel());
        modelo.add(linkTo(methodOn(HistoricoController.class)
                .historico(animal.id())).withRel("historico"));

        if (animal.tutorId() != null) {
            modelo.add(linkTo(methodOn(TutorController.class)
                    .buscarPorId(animal.tutorId())).withRel("tutor"));
        }
        if (seguranca.ehDonoOuAdministrador(animal.id())) {
            modelo.add(linkTo(methodOn(HistoricoController.class)
                    .acessos(animal.id())).withRel("acessos"));
        }
        return modelo;
    }
}
