package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Sexo;
import br.com.fiap.clyvovet.model.Veterinario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VeterinarioMapperTest {

    private final VeterinarioMapper mapper = new VeterinarioMapper(new EnderecoMapper());

    private static VeterinarioRequest request(String nome, UUID clinicaId) {
        return new VeterinarioRequest("11122233344", nome, LocalDate.of(1985, 3, 15), Sexo.FEMININO,
                "camila@vetcare.com.br", "11990010001", EnderecoMapperTest.requestCompleto(),
                "Clinica Geral", "CRMV-SP 14320", clinicaId);
    }

    private static Clinica clinica(String nome) {
        Clinica clinica = new Clinica();
        clinica.setId(UUID.randomUUID());
        clinica.setNome(nome);
        return clinica;
    }

    @Test
    @DisplayName("toEntity copia os campos e amarra a clinica recebida")
    void toEntityCopiaCampos() {
        Clinica vetcare = clinica("VetCare Prime");

        Veterinario veterinario = mapper.toEntity(request("Camila Ferreira", vetcare.getId()), vetcare);

        assertThat(veterinario.getNome()).isEqualTo("Camila Ferreira");
        assertThat(veterinario.getCpf()).isEqualTo("11122233344");
        assertThat(veterinario.getCrmv()).isEqualTo("CRMV-SP 14320");
        assertThat(veterinario.getEspecialidade()).isEqualTo("Clinica Geral");
        assertThat(veterinario.getSexo()).isEqualTo(Sexo.FEMININO);
        assertThat(veterinario.getEndereco().getCidade()).isEqualTo("Sao Paulo");
        assertThat(veterinario.getClinica()).isSameAs(vetcare);
    }

    @Test
    @DisplayName("atualizar troca a clinica e preserva o id")
    void atualizarPreservaId() {
        Clinica vetcare = clinica("VetCare Prime");
        Veterinario veterinario = mapper.toEntity(request("Camila Ferreira", vetcare.getId()), vetcare);
        veterinario.setId(UUID.randomUUID());
        UUID idOriginal = veterinario.getId();

        Clinica petmed = clinica("PetMed Centro");
        mapper.atualizar(veterinario, request("Camila F. Ferreira", petmed.getId()), petmed);

        assertThat(veterinario.getId()).isEqualTo(idOriginal);
        assertThat(veterinario.getNome()).isEqualTo("Camila F. Ferreira");
        assertThat(veterinario.getClinica()).isSameAs(petmed);
    }

    @Test
    @DisplayName("resposta traz id e nome da clinica")
    void respostaTrazDadosDaClinica() {
        Clinica vetcare = clinica("VetCare Prime");

        VeterinarioResponse response = mapper.toResponse(
                mapper.toEntity(request("Camila Ferreira", vetcare.getId()), vetcare));

        assertThat(response.clinicaId()).isEqualTo(vetcare.getId());
        assertThat(response.clinicaNome()).isEqualTo("VetCare Prime");
    }

    @Test
    @DisplayName("veterinario sem clinica responde com os campos da clinica nulos")
    void veterinarioSemClinica() {
        VeterinarioResponse response = mapper.toResponse(mapper.toEntity(request("Sem clinica", null), null));

        assertThat(response.clinicaId()).isNull();
        assertThat(response.clinicaNome()).isNull();
    }
}
