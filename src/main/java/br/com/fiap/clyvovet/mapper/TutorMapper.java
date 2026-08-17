package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.tutor.TutorRequest;
import br.com.fiap.clyvovet.dto.tutor.TutorResponse;
import br.com.fiap.clyvovet.model.Tutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
