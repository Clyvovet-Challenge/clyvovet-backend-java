package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Veterinario;
import org.springframework.stereotype.Component;

@Component
public class EventoClinicoMapper {

    public EventoClinico toEntity(EventoClinicoRequest request, RelacionamentosDoEvento relacionamentos) {
        EventoClinico evento = new EventoClinico();
        atualizar(evento, request, relacionamentos);
        return evento;
    }

    public void atualizar(EventoClinico evento, EventoClinicoRequest request, RelacionamentosDoEvento relacionamentos) {
        evento.setData(request.getData());
        evento.setHora(request.getHora());
        evento.setDescricao(request.getDescricao());
        evento.setTipoEvento(request.getTipoEvento());
        evento.setVeterinario(relacionamentos.veterinario());
        evento.setAnimal(relacionamentos.animal());
        evento.setClinica(relacionamentos.clinica());
    }

    public EventoClinicoResponse toResponse(EventoClinico evento) {
        return new EventoClinicoResponse(
                evento.getId(),
                evento.getData(),
                evento.getHora(),
                evento.getDescricao(),
                evento.getTipoEvento(),
                Referencias.de(evento.getVeterinario(), Veterinario::getId),
                Referencias.de(evento.getVeterinario(), Veterinario::getNome),
                Referencias.de(evento.getAnimal(), Animal::getId),
                Referencias.de(evento.getAnimal(), Animal::getNome),
                Referencias.de(evento.getClinica(), Clinica::getId),
                Referencias.de(evento.getClinica(), Clinica::getNome)
        );
    }
}
