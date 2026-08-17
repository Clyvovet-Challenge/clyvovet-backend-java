package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.pagamento.PagamentoRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoMapper {

    public Pagamento toEntity(PagamentoRequest request, EventoClinico eventoClinico) {
        Pagamento pagamento = new Pagamento();
        atualizar(pagamento, request, eventoClinico);
        return pagamento;
    }

    public void atualizar(Pagamento pagamento, PagamentoRequest request, EventoClinico eventoClinico) {
        pagamento.setFormaPagamento(request.getFormaPagamento());
        pagamento.setValor(request.getValor());
        pagamento.setDataPagamento(request.getDataPagamento());
        pagamento.setDescricao(request.getDescricao());
        pagamento.setObservacao(request.getObservacao());
        pagamento.setStatusPagamento(request.getStatusPagamento());
        pagamento.setEventoClinico(eventoClinico);
    }

    public PagamentoResponse toResponse(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getFormaPagamento(),
                pagamento.getValor(),
                pagamento.getDataPagamento(),
                pagamento.getDescricao(),
                pagamento.getObservacao(),
                Referencias.de(pagamento.getEventoClinico(), EventoClinico::getId),
                pagamento.getStatusPagamento()
        );
    }
}
