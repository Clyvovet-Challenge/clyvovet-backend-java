package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.animal.AnimalRequest;
import br.com.fiap.clyvovet.dto.animal.AnimalResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.SexoAnimal;
import br.com.fiap.clyvovet.model.Tutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalMapperTest {

    private final AnimalMapper mapper = new AnimalMapper();

    private static AnimalRequest request(String nome, UUID tutorId) {
        return new AnimalRequest(nome, "Vira-lata", "CAO", "MEDIO", "Caramelo",
                SexoAnimal.MACHO, LocalDate.of(2020, 1, 15), "sem observacoes", tutorId,
                // microchip e castrado entraram na V6: o chip identifica o
                // animal no balcao, e a castracao compoe o resumo de seguranca.
                null, null);
    }

    private static Tutor tutor(String nome) {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setNome(nome);
        return tutor;
    }

    @Test
    @DisplayName("toEntity copia os campos e amarra o tutor recebido")
    void toEntityCopiaCampos() {
        Tutor lucas = tutor("Lucas");

        Animal animal = mapper.toEntity(request("Bolinha", lucas.getId()), lucas);

        assertThat(animal.getNome()).isEqualTo("Bolinha");
        assertThat(animal.getRaca()).isEqualTo("Vira-lata");
        assertThat(animal.getEspecie()).isEqualTo("CAO");
        assertThat(animal.getPorte()).isEqualTo("MEDIO");
        assertThat(animal.getCor()).isEqualTo("Caramelo");
        assertThat(animal.getSexo()).isEqualTo(SexoAnimal.MACHO);
        assertThat(animal.getDataNascimento()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(animal.getObservacao()).isEqualTo("sem observacoes");
        assertThat(animal.getTutor()).isSameAs(lucas);
    }

    /**
     * O id nao pode vir do request — quem o define e o banco. Se atualizar
     * mexesse nele, um PUT criaria um registro novo em vez de alterar o atual.
     */
    @Test
    @DisplayName("atualizar altera os campos e preserva o id da entidade")
    void atualizarPreservaId() {
        Tutor lucas = tutor("Lucas");
        Animal animal = mapper.toEntity(request("Bolinha", lucas.getId()), lucas);
        animal.setId(UUID.randomUUID());
        UUID idOriginal = animal.getId();

        Tutor maria = tutor("Maria");
        mapper.atualizar(animal, request("Bolinha Editado", maria.getId()), maria);

        assertThat(animal.getId()).isEqualTo(idOriginal);
        assertThat(animal.getNome()).isEqualTo("Bolinha Editado");
        assertThat(animal.getTutor()).isSameAs(maria);
    }

    @Test
    @DisplayName("resposta traz id e nome do tutor")
    void respostaTrazDadosDoTutor() {
        Tutor lucas = tutor("Lucas");
        Animal animal = mapper.toEntity(request("Bolinha", lucas.getId()), lucas);

        AnimalResponse response = mapper.toResponse(animal);

        assertThat(response.tutorId()).isEqualTo(lucas.getId());
        assertThat(response.tutorNome()).isEqualTo("Lucas");
        assertThat(response.nome()).isEqualTo("Bolinha");
    }

    @Test
    @DisplayName("animal sem tutor responde com os campos do tutor nulos")
    void animalSemTutor() {
        Animal animal = mapper.toEntity(request("Sem dono", null), null);

        AnimalResponse response = mapper.toResponse(animal);

        assertThat(response.tutorId()).isNull();
        assertThat(response.tutorNome()).isNull();
    }
}
