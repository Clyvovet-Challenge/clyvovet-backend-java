package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * `racaId` e `racaChave` sao NULOS quando o animal tem uma raca que o catalogo
 * nao cobre -- e ai `raca` (texto) e a unica informacao que existe. Quem
 * consome precisa tratar isso: e o caminho de "Labrador misto", que diz algo
 * que "Labrador" nao diz.
 *
 * `racaChave` e o identificador estavel ("golden-retriever"). E por ele que o
 * app escolhe a arte em pixel do animal, sem precisar normalizar nome.
 */
public record AnimalResponse(UUID id, String nome, String raca, String especie, String porte, String cor, SexoAnimal sexo, LocalDate dataNascimento, String observacao, UUID tutorId, String tutorNome,
                             String microchip, Boolean castrado, Boolean resumoDeSegurancaAtivo,
                             UUID racaId, String racaChave) {
}