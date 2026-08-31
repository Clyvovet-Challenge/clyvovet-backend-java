package br.com.fiap.clyvovet.dto.historico;

import br.com.fiap.clyvovet.model.NivelAcesso;
import br.com.fiap.clyvovet.model.SexoAnimal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O historico clinico, montado conforme o nivel que o solicitante alcanca.
 *
 * O campo nivelDeAcesso vem na resposta de proposito: quem consome precisa
 * saber se esta vendo a linha do tempo inteira ou so a fatia da propria
 * clinica. Sem ele, uma lista curta seria indistinguivel de um animal com
 * pouco historico — e o veterinario tiraria conclusao clinica de uma ausencia
 * que e de permissao, nao de fato.
 */
public record HistoricoResponse(
        UUID animalId,
        String nome,
        String especie,
        String raca,
        String porte,
        SexoAnimal sexo,
        LocalDate dataNascimento,
        Integer idadeEmMeses,
        String microchip,
        Boolean castrado,
        NivelAcesso nivelDeAcesso,
        List<AlertaResponse> alertas,
        List<PesoResponse> serieDePeso,
        List<VacinaResponse> vacinas,
        List<LinhaDoTempoResponse> linhaDoTempo,
        String tutorNome,
        String tutorTelefone
) {}
