package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.animal.AnimalPatchRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Tutor;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

@Component
public class AnimalMapper {

    /**
     * Criar e atualizar copiam exatamente os mesmos campos. Delegar para
     * {@link #atualizar} deixa a lista existir em um lugar so — antes um campo
     * novo precisava ser lembrado em dois metodos, e no service tambem.
     */
    public Animal toEntity(AnimalRequest request, Tutor tutor) {
        Animal animal = new Animal();
        atualizar(animal, request, tutor);
        return animal;
    }

    public void atualizar(Animal animal, AnimalRequest request, Tutor tutor) {
        animal.setNome(request.getNome());
        animal.setRaca(request.getRaca());
        animal.setEspecie(request.getEspecie());
        animal.setPorte(request.getPorte());
        animal.setCor(request.getCor());
        animal.setSexo(request.getSexo());
        animal.setDataNascimento(request.getDataNascimento());
        animal.setObservacao(request.getObservacao());
        animal.setMicrochip(request.getMicrochip());
        animal.setCastrado(request.getCastrado());
        animal.setTutor(tutor);
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Animal animal, AnimalPatchRequest patch, Tutor tutor) {
        aplicarSePresente(patch.getNome(), animal::setNome);
        aplicarSePresente(patch.getRaca(), animal::setRaca);
        aplicarSePresente(patch.getEspecie(), animal::setEspecie);
        aplicarSePresente(patch.getPorte(), animal::setPorte);
        aplicarSePresente(patch.getCor(), animal::setCor);
        aplicarSePresente(patch.getSexo(), animal::setSexo);
        aplicarSePresente(patch.getDataNascimento(), animal::setDataNascimento);
        aplicarSePresente(patch.getObservacao(), animal::setObservacao);
        aplicarSePresente(patch.getMicrochip(), animal::setMicrochip);
        aplicarSePresente(patch.getCastrado(), animal::setCastrado);
        aplicarSePresente(patch.getResumoDeSegurancaAtivo(), animal::setResumoDeSegurancaAtivo);
        // O tutor ja chega resolvido, ou null quando o patch nao trocou o dono.
        aplicarSePresente(tutor, animal::setTutor);
    }

    public AnimalResponse toResponse(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getNome(),
                animal.getRaca(),
                animal.getEspecie(),
                animal.getPorte(),
                animal.getCor(),
                animal.getSexo(),
                animal.getDataNascimento(),
                animal.getObservacao(),
                Referencias.de(animal.getTutor(), Tutor::getId),
                Referencias.de(animal.getTutor(), Tutor::getNome)
        ,
                animal.getMicrochip(),
                animal.getCastrado(),
                animal.getResumoDeSegurancaAtivo());
    }
}
