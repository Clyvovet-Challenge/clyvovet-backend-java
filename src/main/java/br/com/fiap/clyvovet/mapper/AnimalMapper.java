package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.animal.AnimalPatchRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Raca;
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
    public Animal toEntity(AnimalRequest request, Tutor tutor, Raca raca) {
        Animal animal = new Animal();
        atualizar(animal, request, tutor, raca);
        return animal;
    }

    public void atualizar(Animal animal, AnimalRequest request, Tutor tutor, Raca raca) {
        animal.setNome(request.getNome());
        animal.setRaca(request.getRaca());
        animal.setEspecie(request.getEspecie());
        animal.setPorte(emMaiuscula(request.getPorte()));
        animal.setCor(request.getCor());
        animal.setSexo(request.getSexo());
        animal.setDataNascimento(request.getDataNascimento());
        animal.setObservacao(request.getObservacao());
        animal.setMicrochip(request.getMicrochip());
        animal.setCastrado(request.getCastrado());
        animal.setTutor(tutor);
        aplicarCatalogo(animal, raca);
    }

    /**
     * O catalogo ganha do texto.
     *
     * Quando a raca vem do catalogo, tres campos deixam de ser opiniao de quem
     * preencheu o formulario: o NOME da raca, a ESPECIE e -- so quando o pedido
     * nao trouxe porte -- o PORTE TIPICO.
     *
     * E isso que faz "Cachorro"/"CAO"/"CANINO" pararem de conviver na mesma
     * coluna: nao por validacao que recusa, mas por derivacao que uniformiza.
     *
     * O porte so e preenchido se veio vazio porque ele e uma sugestao, nao uma
     * verdade: existe Poodle grande, e quem conhece o animal e o tutor.
     */
    /**
     * O CHECK do banco compara com 'PEQUENO'/'MEDIO'/'GRANDE' em maiuscula, e a
     * do Oracle e sensivel a caixa. O formulario do app manda "Pequeno".
     * Normalizar aqui e o que faz o mesmo cadastro passar nos dois bancos.
     */
    private static String emMaiuscula(String texto) {
        return texto == null ? null : texto.trim().toUpperCase();
    }

    private void aplicarCatalogo(Animal animal, Raca raca) {
        animal.setRacaDoCatalogo(raca);
        if (raca == null) return;

        animal.setRaca(raca.getNome());
        animal.setEspecie(raca.getEspecie().rotulo());
        if (animal.getPorte() == null || animal.getPorte().isBlank()) {
            animal.setPorte(raca.getPorteTipico());
        }
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Animal animal, AnimalPatchRequest patch, Tutor tutor, Raca raca) {
        aplicarSePresente(patch.getNome(), animal::setNome);
        aplicarSePresente(patch.getRaca(), animal::setRaca);
        aplicarSePresente(patch.getEspecie(), animal::setEspecie);
        aplicarSePresente(emMaiuscula(patch.getPorte()), animal::setPorte);
        aplicarSePresente(patch.getCor(), animal::setCor);
        aplicarSePresente(patch.getSexo(), animal::setSexo);
        aplicarSePresente(patch.getDataNascimento(), animal::setDataNascimento);
        aplicarSePresente(patch.getObservacao(), animal::setObservacao);
        aplicarSePresente(patch.getMicrochip(), animal::setMicrochip);
        aplicarSePresente(patch.getCastrado(), animal::setCastrado);
        aplicarSePresente(patch.getResumoDeSegurancaAtivo(), animal::setResumoDeSegurancaAtivo);
        // O tutor ja chega resolvido, ou null quando o patch nao trocou o dono.
        aplicarSePresente(tutor, animal::setTutor);
        // Idem a raca: null aqui significa "o patch nao mexeu na raca", e nao
        // "apague a raca do catalogo". Por isso passa por aplicarSePresente e
        // nao pelo aplicarCatalogo, que trata null como ausencia deliberada.
        if (raca != null) aplicarCatalogo(animal, raca);
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
                animal.getResumoDeSegurancaAtivo(),
                animal.getRacaDoCatalogo() == null ? null : animal.getRacaDoCatalogo().getId(),
                animal.getRacaDoCatalogo() == null ? null : animal.getRacaDoCatalogo().getChave());
    }
}
