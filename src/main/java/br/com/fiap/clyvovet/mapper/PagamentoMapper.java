package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.pagamento.PagamentoPatchRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Pagamento;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

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
        // O status NAO vem do corpo (P14): nasce PENDENTE na entidade e muda
        // por /confirmar e /estornar. No PUT, nao mexer nele preserva o que a
        // transicao ja gravou.
        pagamento.setEventoClinico(eventoClinico);
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Pagamento pagamento, PagamentoPatchRequest patch, EventoClinico eventoClinico) {
        aplicarSePresente(patch.getFormaPagamento(), pagamento::setFormaPagamento);
        aplicarSePresente(patch.getValor(), pagamento::setValor);
        aplicarSePresente(patch.getDataPagamento(), pagamento::setDataPagamento);
        aplicarSePresente(patch.getDescricao(), pagamento::setDescricao);
        aplicarSePresente(patch.getObservacao(), pagamento::setObservacao);
        aplicarSePresente(eventoClinico, pagamento::setEventoClinico);
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
