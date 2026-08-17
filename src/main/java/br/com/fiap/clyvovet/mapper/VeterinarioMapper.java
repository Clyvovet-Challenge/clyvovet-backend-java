package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Veterinario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VeterinarioMapper {

    private final EnderecoMapper enderecoMapper;

    public Veterinario toEntity(VeterinarioRequest request, Clinica clinica) {
        Veterinario veterinario = new Veterinario();
        atualizar(veterinario, request, clinica);
        return veterinario;
    }

    public void atualizar(Veterinario veterinario, VeterinarioRequest request, Clinica clinica) {
        veterinario.setNome(request.getNome());
        veterinario.setCpf(request.getCpf());
        veterinario.setCrmv(request.getCrmv());
        veterinario.setEspecialidade(request.getEspecialidade());
        veterinario.setTelefone(request.getTelefone());
        veterinario.setEmail(request.getEmail());
        veterinario.setDataNascimento(request.getDataNascimento());
        veterinario.setSexo(request.getSexo());
        veterinario.setEndereco(enderecoMapper.toEntity(request.getEndereco()));
        veterinario.setClinica(clinica);
    }

    public VeterinarioResponse toResponse(Veterinario veterinario) {
        return new VeterinarioResponse(
                veterinario.getId(),
                veterinario.getNome(),
                veterinario.getCpf(),
                veterinario.getTelefone(),
                veterinario.getEmail(),
                veterinario.getCrmv(),
                veterinario.getEspecialidade(),
                veterinario.getDataNascimento(),
                veterinario.getSexo(),
                enderecoMapper.toResponse(veterinario.getEndereco()),
                Referencias.de(veterinario.getClinica(), Clinica::getId),
                Referencias.de(veterinario.getClinica(), Clinica::getNome)
        );
    }
}
