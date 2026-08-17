package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import br.com.fiap.clyvovet.dto.endereco.EnderecoResponse;
import br.com.fiap.clyvovet.model.Endereco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnderecoMapperTest {

    private final EnderecoMapper mapper = new EnderecoMapper();

    static EnderecoRequest requestCompleto() {
        return new EnderecoRequest("Av. Paulista", "1000", "Bela Vista", "Sao Paulo", "SP", "01310100", "conj. 51");
    }

    @Test
    @DisplayName("leva todos os campos do request para a entidade")
    void copiaTodosOsCampos() {
        Endereco endereco = mapper.toEntity(requestCompleto());

        assertThat(endereco.getLogradouro()).isEqualTo("Av. Paulista");
        assertThat(endereco.getNumero()).isEqualTo("1000");
        assertThat(endereco.getBairro()).isEqualTo("Bela Vista");
        assertThat(endereco.getCidade()).isEqualTo("Sao Paulo");
        assertThat(endereco.getEstado()).isEqualTo("SP");
        assertThat(endereco.getCep()).isEqualTo("01310100");
        assertThat(endereco.getComplemento()).isEqualTo("conj. 51");
    }

    @Test
    @DisplayName("ida e volta preserva o conteudo")
    void idaEVoltaPreservaConteudo() {
        EnderecoResponse response = mapper.toResponse(mapper.toEntity(requestCompleto()));

        assertThat(response).isEqualTo(new EnderecoResponse(
                "Av. Paulista", "1000", "conj. 51", "Bela Vista", "Sao Paulo", "SP", "01310100"));
    }

    /**
     * Endereco e um @Embeddable: com todas as colunas nulas o Hibernate devolve
     * null no lugar do objeto. Antes do null-guard isso derrubava a listagem
     * inteira com NullPointerException.
     */
    @Test
    @DisplayName("endereco nulo vira resposta nula, sem estourar")
    void enderecoNuloNaoEstoura() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
