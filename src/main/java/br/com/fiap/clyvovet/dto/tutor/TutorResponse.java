package br.com.fiap.clyvovet.dto.tutor;

import br.com.fiap.clyvovet.dto.endereco.EnderecoResponse;
import br.com.fiap.clyvovet.model.Sexo;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O campo sexo e o enum, e nao String: o mapper precisava chamar toString() na
 * entidade, o que estourava NullPointerException em cadastro sem sexo. O JSON
 * resultante e o mesmo — o Jackson ja serializa o enum pelo nome.
 */
public record TutorResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        Sexo sexo,
        LocalDate dataNascimento,
        String cpf,
        EnderecoResponse endereco
) {
}
