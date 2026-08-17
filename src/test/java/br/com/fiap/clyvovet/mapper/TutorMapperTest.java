package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.model.Sexo;
import br.com.fiap.clyvovet.model.Tutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TutorMapperTest {

    private final TutorMapper mapper = new TutorMapper(new EnderecoMapper());

    private static TutorRequest request(String nome, Sexo sexo) {
        return new TutorRequest(nome, "12345678901", "tutor@email.com", "11999998888",
                sexo, LocalDate.of(1990, 5, 10), EnderecoMapperTest.requestCompleto());
    }

    @Test
    @DisplayName("toEntity copia os campos e o endereco")
    void toEntityCopiaCampos() {
        Tutor tutor = mapper.toEntity(request("Lucas", Sexo.MASCULINO));

        assertThat(tutor.getNome()).isEqualTo("Lucas");
        assertThat(tutor.getCpf()).isEqualTo("12345678901");
        assertThat(tutor.getEmail()).isEqualTo("tutor@email.com");
        assertThat(tutor.getTelefone()).isEqualTo("11999998888");
        assertThat(tutor.getSexo()).isEqualTo(Sexo.MASCULINO);
        assertThat(tutor.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 10));
        assertThat(tutor.getEndereco().getCidade()).isEqualTo("Sao Paulo");
    }

    @Test
    @DisplayName("atualizar troca os campos e preserva o id")
    void atualizarPreservaId() {
        Tutor tutor = mapper.toEntity(request("Lucas", Sexo.MASCULINO));
        tutor.setId(UUID.randomUUID());
        UUID idOriginal = tutor.getId();

        mapper.atualizar(tutor, request("Lucas Editado", Sexo.OUTRO));

        assertThat(tutor.getId()).isEqualTo(idOriginal);
        assertThat(tutor.getNome()).isEqualTo("Lucas Editado");
        assertThat(tutor.getSexo()).isEqualTo(Sexo.OUTRO);
    }

    @Test
    @DisplayName("resposta expoe o sexo como enum")
    void respostaExpoeSexoComoEnum() {
        TutorResponse response = mapper.toResponse(mapper.toEntity(request("Lucas", Sexo.FEMININO)));

        assertThat(response.sexo()).isEqualTo(Sexo.FEMININO);
        assertThat(response.endereco().cidade()).isEqualTo("Sao Paulo");
    }

    /**
     * Registro inserido direto no banco pode vir sem genero e sem endereco. O
     * mapper chamava getSexo().toString() e enderecoToResponse sem guarda —
     * eram dois NullPointerException a caminho de um 500 na listagem.
     */
    @Test
    @DisplayName("tutor sem sexo e sem endereco nao derruba a resposta")
    void tutorIncompletoNaoEstoura() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setNome("Cadastro antigo");

        assertThatCode(() -> mapper.toResponse(tutor)).doesNotThrowAnyException();

        TutorResponse response = mapper.toResponse(tutor);
        assertThat(response.sexo()).isNull();
        assertThat(response.endereco()).isNull();
    }
}
