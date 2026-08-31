package br.com.fiap.clyvovet.controller.hateoas;

import br.com.fiap.clyvovet.controller.AnimalController;
import br.com.fiap.clyvovet.controller.HistoricoController;
import br.com.fiap.clyvovet.controller.TutorController;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.security.SegurancaService;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Links de um animal. "acessos" so aparece para quem pode segui-lo: um link que
 * responderia 403 desenha um botao que falha depois do clique.
 */
@Component
public class LinksDoAnimal {

    private final SegurancaService seguranca;

    public LinksDoAnimal(SegurancaService seguranca) {
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
