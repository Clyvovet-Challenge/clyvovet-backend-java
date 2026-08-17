package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.endereco.EnderecoRequest;
import br.com.fiap.clyvovet.dto.endereco.EnderecoResponse;
import br.com.fiap.clyvovet.model.Endereco;
import org.springframework.stereotype.Component;

@Component
public class EnderecoMapper {

    public Endereco toEntity(EnderecoRequest request) {
        if (request == null) {
            return null;
        }
        Endereco endereco = new Endereco();
        endereco.setLogradouro(request.getLogradouro());
        endereco.setNumero(request.getNumero());
        endereco.setBairro(request.getBairro());
        endereco.setCidade(request.getCidade());
        endereco.setEstado(request.getEstado());
        endereco.setCep(request.getCep());
        endereco.setComplemento(request.getComplemento());
        return endereco;
    }

    /**
     * Endereco e um @Embedded opcional: quando todas as colunas estao nulas, o
     * Hibernate devolve null no lugar do objeto. Sem esta guarda, um cadastro
     * antigo sem endereco derrubaria a listagem inteira com NullPointerException.
     */
    public EnderecoResponse toResponse(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoResponse(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                endereco.getCep()
        );
    }
}
