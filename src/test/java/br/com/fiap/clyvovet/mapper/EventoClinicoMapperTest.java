package br.com.fiap.clyvovet.mapper;

import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoPatchRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoRequest;
import br.com.fiap.clyvovet.dto.eventoClinico.EventoClinicoResponse;
import br.com.fiap.clyvovet.model.Animal;
import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.EventoClinico;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.model.Veterinario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventoClinicoMapperTest {

    private final EventoClinicoMapper mapper = new EventoClinicoMapper();

    private static EventoClinicoRequest request(String descricao, TipoEvento tipo) {
        // O ultimo null e o servicoId, opcional: evento registrado direto pelo
        // veterinario pode nao ter catalogo por tras.
        return new EventoClinicoRequest(LocalDate.of(2026, 3, 10), "14:30", descricao,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, tipo);
    }

    private static RelacionamentosDoEvento relacionamentos() {
        Veterinario veterinario = new Veterinario();
        veterinario.setId(UUID.randomUUID());
        veterinario.setNome("Camila Ferreira");

        Animal animal = new Animal();
        animal.setId(UUID.randomUUID());
        animal.setNome("Bolinha");

        Clinica clinica = new Clinica();
        clinica.setId(UUID.randomUUID());
        clinica.setNome("PetMed Centro");

        return new RelacionamentosDoEvento(veterinario, animal, clinica, null);
    }

    @Test
    @DisplayName("toEntity copia os campos e os tres relacionamentos")
    void toEntityCopiaCampos() {
        RelacionamentosDoEvento relacionamentos = relacionamentos();

        EventoClinico evento = mapper.toEntity(request("Consulta de rotina", TipoEvento.CONSULTA), relacionamentos);

        assertThat(evento.getData()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(evento.getHora()).isEqualTo("14:30");
        assertThat(evento.getDescricao()).isEqualTo("Consulta de rotina");
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.CONSULTA);
        assertThat(evento.getVeterinario()).isSameAs(relacionamentos.veterinario());
        assertThat(evento.getAnimal()).isSameAs(relacionamentos.animal());
        assertThat(evento.getClinica()).isSameAs(relacionamentos.clinica());
    }

    @Test
    @DisplayName("atualizar troca os relacionamentos e preserva o id")
    void atualizarPreservaId() {
        EventoClinico evento = mapper.toEntity(request("Consulta", TipoEvento.CONSULTA), relacionamentos());
        evento.setId(UUID.randomUUID());
        UUID idOriginal = evento.getId();

        RelacionamentosDoEvento outros = relacionamentos();
        mapper.atualizar(evento, request("Retorno", TipoEvento.RETORNO), outros);

        assertThat(evento.getId()).isEqualTo(idOriginal);
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.RETORNO);
        assertThat(evento.getAnimal()).isSameAs(outros.animal());
    }

    @Test
    @DisplayName("resposta desnormaliza id e nome de veterinario, animal e clinica")
    void respostaDesnormalizaRelacionamentos() {
        RelacionamentosDoEvento relacionamentos = relacionamentos();

        EventoClinicoResponse response = mapper.toResponse(
                mapper.toEntity(request("Consulta", TipoEvento.CONSULTA), relacionamentos));

        assertThat(response.veterinarioNome()).isEqualTo("Camila Ferreira");
        assertThat(response.animalNome()).isEqualTo("Bolinha");
        assertThat(response.clinicaNome()).isEqualTo("PetMed Centro");
        assertThat(response.veterinarioId()).isEqualTo(relacionamentos.veterinario().getId());
        assertThat(response.animalId()).isEqualTo(relacionamentos.animal().getId());
        assertThat(response.clinicaId()).isEqualTo(relacionamentos.clinica().getId());
    }

    /**
     * Todo campo que o PATCH aceita precisa chegar na entidade.
     *
     * Escrito por campo, e nao so para o caso que quebrou: o defeito era o
     * servico resolvido pelo service e descartado em silencio pelo mapper --
     * PATCH devolvia 200 e o preco do atendimento seguia o antigo. Um mapper
     * aplica campo a campo e nada acusa o que ficou de fora, entao a garantia
     * so existe se estiver escrita aqui.
     */
    @Test
    @DisplayName("aplicarPatch leva TODOS os campos do patch para a entidade")
    void aplicarPatchNaoPerdeCampo() {
        EventoClinico evento = mapper.toEntity(request("Consulta", TipoEvento.CONSULTA), relacionamentos());

        // Todo campo preenchido: e o ponto do teste.
        EventoClinicoPatchRequest patch = new EventoClinicoPatchRequest(
                LocalDate.of(2026, 7, 1), "09:15", "Descricao nova",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                TipoEvento.VACINA);

        Servico servico = new Servico();
        servico.setId(patch.getServicoId());
        servico.setNome("Vacina antirrabica");

        RelacionamentosDoEvento novos = new RelacionamentosDoEvento(
                relacionamentos().veterinario(), relacionamentos().animal(),
                relacionamentos().clinica(), servico);

        mapper.aplicarPatch(evento, patch, novos);

        assertThat(evento.getData()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(evento.getHora()).isEqualTo("09:15");
        assertThat(evento.getDescricao()).isEqualTo("Descricao nova");
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.VACINA);
        assertThat(evento.getVeterinario()).isSameAs(novos.veterinario());
        assertThat(evento.getAnimal()).isSameAs(novos.animal());
        assertThat(evento.getClinica()).isSameAs(novos.clinica());
        // O que faltava: sem ele o servico decide preco e duracao e o PATCH mente.
        assertThat(evento.getServico()).isSameAs(servico);
    }

    @Test
    @DisplayName("aplicarPatch preserva o que o corpo nao citou")
    void aplicarPatchPreservaOmitido() {
        RelacionamentosDoEvento originais = relacionamentos();
        EventoClinico evento = mapper.toEntity(request("Consulta", TipoEvento.CONSULTA), originais);
        Servico servicoOriginal = new Servico();
        evento.setServico(servicoOriginal);

        // Patch vazio: o mapper entende null como "mantenha o vinculo atual".
        mapper.aplicarPatch(evento, new EventoClinicoPatchRequest(),
                new RelacionamentosDoEvento(null, null, null, null));

        assertThat(evento.getDescricao()).isEqualTo("Consulta");
        assertThat(evento.getAnimal()).isSameAs(originais.animal());
        assertThat(evento.getServico()).isSameAs(servicoOriginal);
    }

    @Test
    @DisplayName("relacionamentos nulos nao derrubam a resposta")
    void relacionamentosNulos() {
        EventoClinico evento = mapper.toEntity(request("Consulta", TipoEvento.CONSULTA),
                new RelacionamentosDoEvento(null, null, null, null));

        EventoClinicoResponse response = mapper.toResponse(evento);

        assertThat(response.veterinarioId()).isNull();
        assertThat(response.veterinarioNome()).isNull();
        assertThat(response.animalId()).isNull();
        assertThat(response.animalNome()).isNull();
        assertThat(response.clinicaId()).isNull();
        assertThat(response.clinicaNome()).isNull();
    }
}
