package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.pagamento.PagamentoRequest;
import br.com.fiap.clyvovet.dto.pagamento.PagamentoResponse;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.FormaPagamento;
import br.com.fiap.clyvovet.model.Pagamento;
import br.com.fiap.clyvovet.model.StatusPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PagamentoMapperTest {

    private final PagamentoMapper mapper = new PagamentoMapper();

    private static PagamentoRequest request(FormaPagamento forma, StatusPagamento status, String valor) {
        return new PagamentoRequest(
                forma,
                new BigDecimal(valor),
                LocalDate.of(2026, 3, 10),
                "Consulta",
                "pago na recepcao",
                UUID.randomUUID(),
                status);
    }

    private static EventoClinico evento() {
        EventoClinico evento = new EventoClinico();
        evento.setId(UUID.randomUUID());
        return evento;
    }

    @Test
    @DisplayName("toEntity copia os campos e amarra o evento recebido")
    void toEntityCopiaCampos() {
        EventoClinico evento = evento();

        Pagamento pagamento = mapper.toEntity(request(FormaPagamento.PIX, StatusPagamento.PAGO, "250.75"), evento);

        assertThat(pagamento.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(pagamento.getValor()).isEqualByComparingTo("250.75");
        assertThat(pagamento.getDataPagamento()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(pagamento.getDescricao()).isEqualTo("Consulta");
        assertThat(pagamento.getObservacao()).isEqualTo("pago na recepcao");
        assertThat(pagamento.getStatusPagamento()).isEqualTo(StatusPagamento.PAGO);
        assertThat(pagamento.getEventoClinico()).isSameAs(evento);
    }

    @Test
    @DisplayName("atualizar troca forma, valor e status e preserva o id")
    void atualizarPreservaId() {
        Pagamento pagamento = mapper.toEntity(request(FormaPagamento.PIX, StatusPagamento.PAGO, "250.75"), evento());
        pagamento.setId(UUID.randomUUID());
        UUID idOriginal = pagamento.getId();

        EventoClinico outroEvento = evento();
        mapper.atualizar(pagamento, request(FormaPagamento.BOLETO, StatusPagamento.PENDENTE, "300.00"), outroEvento);

        assertThat(pagamento.getId()).isEqualTo(idOriginal);
        assertThat(pagamento.getFormaPagamento()).isEqualTo(FormaPagamento.BOLETO);
        assertThat(pagamento.getValor()).isEqualByComparingTo("300.00");
        assertThat(pagamento.getStatusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(pagamento.getEventoClinico()).isSameAs(outroEvento);
    }

    @Test
    @DisplayName("resposta traz o id do evento clinico")
    void respostaTrazIdDoEvento() {
        EventoClinico evento = evento();

        PagamentoResponse response = mapper.toResponse(
                mapper.toEntity(request(FormaPagamento.CARTAO, StatusPagamento.PAGO, "99.90"), evento));

        assertThat(response.eventoClinicoId()).isEqualTo(evento.getId());
        assertThat(response.formaPagamento()).isEqualTo(FormaPagamento.CARTAO);
        assertThat(response.valor()).isEqualByComparingTo("99.90");
    }

    @Test
    @DisplayName("pagamento sem evento responde com o id do evento nulo")
    void pagamentoSemEvento() {
        Pagamento pagamento = mapper.toEntity(request(FormaPagamento.DINHEIRO, StatusPagamento.PENDENTE, "10.00"), null);

        assertThat(mapper.toResponse(pagamento).eventoClinicoId()).isNull();
    }
}
