package br.com.fiap.clyvovet.dto.animal;

import br.com.fiap.clyvovet.model.SexoAnimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalResponse(UUID id, String nome, String raca, String especie, String porte, String cor, SexoAnimal sexo, LocalDate dataNascimento, String observacao, UUID tutorId, String tutorNome,
                             String microchip, Boolean castrado, Boolean resumoDeSegurancaAtivo) {
}