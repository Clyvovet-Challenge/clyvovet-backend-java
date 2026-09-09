package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.clinica.ClinicaRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaResponse;
import br.com.fiap.clyvovet.model.Clinica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicaMapperTest {

    private final ClinicaMapper mapper = new ClinicaMapper(new EnderecoMapper());

    private static ClinicaRequest request(String nome) {
        return new ClinicaRequest(nome, "12345678000191", "1131000001",
                "contato@clinica.com.br", EnderecoMapperTest.requestCompleto(),
                new BigDecimal("-23.567000"), new BigDecimal("-46.648300"));
    }

    @Test
    @DisplayName("toEntity copia os campos e o endereco")
    void toEntityCopiaCampos() {
        Clinica clinica = mapper.toEntity(request("VetCare Prime"));

        assertThat(clinica.getNome()).isEqualTo("VetCare Prime");
        assertThat(clinica.getCnpj()).isEqualTo("12345678000191");
        assertThat(clinica.getTelefone()).isEqualTo("1131000001");
        assertThat(clinica.getEmail()).isEqualTo("contato@clinica.com.br");
        assertThat(clinica.getEndereco().getCidade()).isEqualTo("Sao Paulo");
        assertThat(clinica.getLatitude()).isEqualByComparingTo("-23.567000");
        assertThat(clinica.getLongitude()).isEqualByComparingTo("-46.648300");
    }

    /**
     * Clinica sem coordenada e caso normal (ver V13), e a ausencia tem que
     * chegar ao app como null. Se virasse zero em algum ponto do caminho, o
     * pino iria para latitude 0, longitude 0 -- Golfo da Guine -- e a tela
     * mostraria uma clinica no meio do Atlantico em vez de nao mostrar nada.
     */
    @Test
    @DisplayName("sem coordenada, a resposta traz null e nao zero")
    void semCoordenadaVemNulo() {
        ClinicaRequest semCoordenada = new ClinicaRequest("PetMed Centro", "23456789000102",
                "1131000002", "contato@petmed.com.br", EnderecoMapperTest.requestCompleto(), null, null);

        ClinicaResponse response = mapper.toResponse(mapper.toEntity(semCoordenada));

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }

    @Test
    @DisplayName("atualizar troca os campos e preserva o id")
    void atualizarPreservaId() {
        Clinica clinica = mapper.toEntity(request("VetCare Prime"));
        clinica.setId(UUID.randomUUID());
        UUID idOriginal = clinica.getId();

        mapper.atualizar(clinica, request("VetCare Premium"));

        assertThat(clinica.getId()).isEqualTo(idOriginal);
        assertThat(clinica.getNome()).isEqualTo("VetCare Premium");
    }

    @Test
    @DisplayName("resposta carrega o endereco convertido")
    void respostaCarregaEndereco() {
        ClinicaResponse response = mapper.toResponse(mapper.toEntity(request("VetCare Prime")));

        assertThat(response.nome()).isEqualTo("VetCare Prime");
        assertThat(response.endereco().logradouro()).isEqualTo("Av. Paulista");
    }

    @Test
    @DisplayName("clinica sem endereco responde com endereco nulo")
    void clinicaSemEndereco() {
        Clinica clinica = new Clinica();
        clinica.setId(UUID.randomUUID());
        clinica.setNome("Cadastro antigo");

        assertThat(mapper.toResponse(clinica).endereco()).isNull();
    }
}
