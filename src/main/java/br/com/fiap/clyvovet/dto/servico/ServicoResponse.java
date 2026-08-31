package br.com.fiap.clyvovet.dto.servico;

import br.com.fiap.clyvovet.model.TipoEvento;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        UUID clinicaId,
        String clinicaNome,
        String nome,
        TipoEvento tipoEvento,
        BigDecimal preco,
        Integer duracaoMinutos,
        boolean ativo
) {}
