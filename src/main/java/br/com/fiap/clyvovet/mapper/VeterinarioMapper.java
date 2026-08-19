package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.veterinario.VeterinarioPatchRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioRequest;
import br.com.fiap.clyvovet.dto.veterinario.VeterinarioResponse;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.Veterinario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

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

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Veterinario veterinario, VeterinarioPatchRequest patch, Clinica clinica) {
        aplicarSePresente(patch.getNome(), veterinario::setNome);
        aplicarSePresente(patch.getCpf(), veterinario::setCpf);
        aplicarSePresente(patch.getCrmv(), veterinario::setCrmv);
        aplicarSePresente(patch.getEspecialidade(), veterinario::setEspecialidade);
        aplicarSePresente(patch.getTelefone(), veterinario::setTelefone);
        aplicarSePresente(patch.getEmail(), veterinario::setEmail);
        aplicarSePresente(patch.getDataNascimento(), veterinario::setDataNascimento);
        aplicarSePresente(patch.getSexo(), veterinario::setSexo);
        aplicarSePresente(patch.getEndereco(), endereco -> veterinario.setEndereco(enderecoMapper.toEntity(endereco)));
        aplicarSePresente(clinica, veterinario::setClinica);
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
