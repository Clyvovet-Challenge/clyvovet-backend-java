package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.tutor.TutorPatchRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.model.Tutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

@Component
@RequiredArgsConstructor
public class TutorMapper {

    private final EnderecoMapper enderecoMapper;

    public Tutor toEntity(TutorRequest request) {
        Tutor tutor = new Tutor();
        atualizar(tutor, request);
        return tutor;
    }

    public void atualizar(Tutor tutor, TutorRequest request) {
        tutor.setNome(request.getNome());
        tutor.setCpf(request.getCpf());
        tutor.setEmail(request.getEmail());
        tutor.setTelefone(request.getTelefone());
        tutor.setSexo(request.getSexo());
        tutor.setDataNascimento(request.getDataNascimento());
        tutor.setEndereco(enderecoMapper.toEntity(request.getEndereco()));
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Tutor tutor, TutorPatchRequest patch) {
        aplicarSePresente(patch.getNome(), tutor::setNome);
        aplicarSePresente(patch.getCpf(), tutor::setCpf);
        aplicarSePresente(patch.getEmail(), tutor::setEmail);
        aplicarSePresente(patch.getTelefone(), tutor::setTelefone);
        aplicarSePresente(patch.getSexo(), tutor::setSexo);
        aplicarSePresente(patch.getDataNascimento(), tutor::setDataNascimento);
        aplicarSePresente(patch.getEndereco(), endereco -> tutor.setEndereco(enderecoMapper.toEntity(endereco)));
    }

    public TutorResponse toResponse(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getNome(),
                tutor.getEmail(),
                tutor.getTelefone(),
                tutor.getSexo(),
                tutor.getDataNascimento(),
                tutor.getCpf(),
                enderecoMapper.toResponse(tutor.getEndereco())
        );
    }
}
