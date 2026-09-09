package br.com.fiap.clyvovet.dto.clinica;

import br.com.fiap.clyvovet.dto.endereco.EnderecoResponse;

import java.util.UUID;

/**
 * latitude e longitude podem vir nulas -- clinica cadastrada sem coordenada e
 * caso normal. O app filtra as nulas antes de desenhar o mapa em vez de assumir
 * zero, que cairia no Golfo da Guine.
 */
public record ClinicaResponse (UUID id, String nome, String cnpj, String telefone, String email, EnderecoResponse endereco,
                               java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
}
