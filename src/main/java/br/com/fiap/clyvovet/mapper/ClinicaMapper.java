package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.clinica.ClinicaPatchRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaRequest;
import br.com.fiap.clyvovet.dto.clinica.ClinicaResponse;
import br.com.fiap.clyvovet.model.Clinica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static br.com.fiap.clyvovet.mapper.AtualizacaoParcial.aplicarSePresente;

@Component
@RequiredArgsConstructor
public class ClinicaMapper {

    private final EnderecoMapper enderecoMapper;

    public Clinica toEntity(ClinicaRequest request) {
        Clinica clinica = new Clinica();
        atualizar(clinica, request);
        return clinica;
    }

    public void atualizar(Clinica clinica, ClinicaRequest request) {
        clinica.setNome(request.getNome());
        clinica.setCnpj(request.getCnpj());
        clinica.setTelefone(request.getTelefone());
        clinica.setEmail(request.getEmail());
        clinica.setEndereco(enderecoMapper.toEntity(request.getEndereco()));
    }

    /** Aplica so os campos presentes no corpo do PATCH. */
    public void aplicarPatch(Clinica clinica, ClinicaPatchRequest patch) {
        aplicarSePresente(patch.getNome(), clinica::setNome);
        aplicarSePresente(patch.getCnpj(), clinica::setCnpj);
        aplicarSePresente(patch.getTelefone(), clinica::setTelefone);
        aplicarSePresente(patch.getEmail(), clinica::setEmail);
        aplicarSePresente(patch.getEndereco(), endereco -> clinica.setEndereco(enderecoMapper.toEntity(endereco)));
    }

    public ClinicaResponse toResponse(Clinica clinica) {
        return new ClinicaResponse(
                clinica.getId(),
                clinica.getNome(),
                clinica.getCnpj(),
                clinica.getTelefone(),
                clinica.getEmail(),
                enderecoMapper.toResponse(clinica.getEndereco())
        );
    }
}
