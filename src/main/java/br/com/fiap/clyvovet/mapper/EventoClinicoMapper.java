package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoPatchRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.model.Veterinario;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

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
        evento.setServico(relacionamentos.servico());
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(EventoClinico evento, EventoClinicoPatchRequest patch, RelacionamentosDoEvento relacionamentos) {
        aplicarSePresente(patch.getData(), evento::setData);
        aplicarSePresente(patch.getHora(), evento::setHora);
        aplicarSePresente(patch.getDescricao(), evento::setDescricao);
        aplicarSePresente(patch.getTipoEvento(), evento::setTipoEvento);
        // Cada relacionamento vem resolvido ou null, conforme o patch o citou.
        aplicarSePresente(relacionamentos.veterinario(), evento::setVeterinario);
        aplicarSePresente(relacionamentos.animal(), evento::setAnimal);
        aplicarSePresente(relacionamentos.clinica(), evento::setClinica);
        aplicarSePresente(relacionamentos.servico(), evento::setServico);
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
                Referencias.de(evento.getClinica(), Clinica::getNome),
                evento.getStatusEvento(),
                evento.getDataRetornoPrevisto(),
                Referencias.de(evento.getEventoOrigem(), EventoClinico::getId),
                evento.getPesoKg(),
                Referencias.de(evento.getServico(), Servico::getId),
                Referencias.de(evento.getServico(), Servico::getNome),
                // O valor vem do catalogo, e nao de uma coluna no evento: e a
                // mesma fonte que o fluxo de cobranca usa para saber quanto o
                // atendimento custou. Duplicar aqui abriria espaco para os dois
                // divergirem.
                Referencias.de(evento.getServico(), Servico::getPreco),
                evento.getDesfecho(),
                evento.getMotivoCancelamento()
        );
    }
}
