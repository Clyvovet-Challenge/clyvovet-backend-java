package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.servico.ServicoRequest;
import br.com.fiap.clyvovet.dto.servico.ServicoResponse;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Servico;
import org.springframework.stereotype.Component;

@Component
public class ServicoMapper {

    public Servico toEntity(ServicoRequest request, Clinica clinica) {
        Servico servico = new Servico();
        servico.setClinica(clinica);
        aplicar(servico, request);
        servico.setAtivo(true);
        return servico;
    }

    /** Nao mexe em clinica nem em ativo: servico nao muda de dono, e desativar e outra operacao. */
    public void atualizar(Servico servico, ServicoRequest request) {
        aplicar(servico, request);
    }

    private void aplicar(Servico servico, ServicoRequest request) {
        servico.setNome(request.getNome());
        servico.setTipoEvento(request.getTipoEvento());
        servico.setPreco(request.getPreco());
        servico.setDuracaoMinutos(request.getDuracaoMinutos());
    }

    public ServicoResponse toResponse(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                Referencias.de(servico.getClinica(), Clinica::getId),
                Referencias.de(servico.getClinica(), Clinica::getNome),
                servico.getNome(),
                servico.getTipoEvento(),
                servico.getPreco(),
                servico.getDuracaoMinutos(),
                servico.isAtivo());
    }
}
