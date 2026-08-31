package br.com.fiap.clyvovet.dto.autorizacao;

import br.com.fiap.clyvovet.model.StatusAutorizacao;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma autorizacao, como o tutor a ve.
 *
 * status e vigente sao coisas diferentes, e ambos aparecem: uma autorizacao
 * pode estar VIGENTE e ja ter passado da validade. A expiracao e avaliada na
 * leitura, e nao por um job — depender de varredura significaria que uma
 * autorizacao vencida continua valendo ate o job rodar, e a janela entre o
 * vencimento e a varredura seria acesso sem consentimento.
 */
public record AutorizacaoResponse(
        UUID id,
        UUID animalId,
        String animalNome,
        UUID clinicaId,
        String clinicaNome,
        StatusAutorizacao status,
        LocalDate concedidaEm,
        LocalDate validoAte,
        LocalDate revogadaEm,
        boolean vigente
) {}
