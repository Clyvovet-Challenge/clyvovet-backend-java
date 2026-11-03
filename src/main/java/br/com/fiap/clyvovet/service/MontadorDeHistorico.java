package br.com.fiap.clyvovet.service;

import br.com.fiap.clyvovet.dto.historico.*;
import br.com.fiap.clyvovet.model.*;
import br.com.fiap.clyvovet.repository.AlertaClinicoRepository;
import br.com.fiap.clyvovet.repository.EventoClinicoRepository;
import br.com.fiap.clyvovet.security.SegurancaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * O que o historico e o resumo de seguranca MOSTRAM, dado o nivel ja decidido.
 *
 * <p>Morava no {@code HistoricoService}, junto com a resolucao de nivel, o teto
 * diario e a auditoria. Aqui fica so a montagem: quem pode ver continua sendo
 * decidido la, antes de chegar aqui.</p>
 */
@Component
@RequiredArgsConstructor
class MontadorDeHistorico {

    private final EventoClinicoRepository eventoRepository;
    private final AlertaClinicoRepository alertaRepository;
    private final SegurancaService seguranca;


    ResumoDeSegurancaResponse resumo(Animal animal) {
        List<EventoClinico> eventos = eventoRepository.findByAnimalIdOrderByDataAsc(animal.getId());

        return new ResumoDeSegurancaResponse(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getPorte(),
                idadeEmMeses(animal.getDataNascimento()),
                animal.getCastrado(),
                ultimoPeso(eventos),
                alertas(animal.getId()),
                vacinas(eventos),
                // Do tutor, so o telefone: para a emergencia basta conseguir ligar.
                animal.getTutor() != null ? animal.getTutor().getTelefone() : null);
    }

    HistoricoResponse historico(Animal animal, NivelAcesso nivel) {
        List<EventoClinico> eventos = eventoRepository.findByAnimalIdOrderByDataAsc(animal.getId());
        boolean completo = nivel == NivelAcesso.COMPLETO;

        UUID minhaClinica = seguranca.clinicaDoUsuario();

        List<LinhaDoTempoResponse> linha = eventos.stream()
                // Sem nivel 2, so os atendimentos da propria clinica (C0b).
                .filter(e -> completo || (minhaClinica != null && e.getClinica() != null
                        && minhaClinica.equals(e.getClinica().getId())))
                .map(e -> new LinhaDoTempoResponse(
                        e.getId(),
                        e.getData(),
                        e.getTipoEvento(),
                        e.getStatusEvento(),
                        e.getDescricao(),
                        e.getPesoKg(),
                        completo ? e.getDesfecho() : null,
                        e.getClinica() != null ? e.getClinica().getNome() : null,
                        minhaClinica != null && e.getClinica() != null
                                && minhaClinica.equals(e.getClinica().getId())))
                .toList();

        return new HistoricoResponse(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getPorte(),
                animal.getSexo(),
                animal.getDataNascimento(),
                idadeEmMeses(animal.getDataNascimento()),
                animal.getMicrochip(),
                animal.getCastrado(),
                nivel,
                alertas(animal.getId()),
                serieDePeso(eventos),
                vacinas(eventos),
                linha,
                completo && animal.getTutor() != null ? animal.getTutor().getNome() : null,
                animal.getTutor() != null ? animal.getTutor().getTelefone() : null);
    }

    private List<AlertaResponse> alertas(UUID animalId) {
        return alertaRepository.findByAnimalIdAndAtivoTrueOrderByTipoAsc(animalId).stream()
                .map(a -> new AlertaResponse(a.getId(), a.getTipo(), a.getDescricao(),
                        a.getOrigem(), a.getRegistradoEm()))
                .toList();
    }

    /**
     * Derivado dos eventos, nunca digitado a parte: um resumo mantido a mao
     * envelhece, e leva a revacinar sem necessidade ou a nao vacinar.
     */
    private List<VacinaResponse> vacinas(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEvento.VACINA)
                .filter(e -> e.getStatusEvento() == StatusEvento.REALIZADO)
                .map(e -> new VacinaResponse(e.getData(), e.getDescricao()))
                .toList();
    }

    private List<PesoResponse> serieDePeso(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getPesoKg() != null)
                .map(e -> new PesoResponse(e.getData(), e.getPesoKg()))
                .toList();
    }

    private java.math.BigDecimal ultimoPeso(List<EventoClinico> eventos) {
        return eventos.stream()
                .filter(e -> e.getPesoKg() != null)
                .reduce((primeiro, ultimo) -> ultimo)
                .map(EventoClinico::getPesoKg)
                .orElse(null);
    }

    private Integer idadeEmMeses(LocalDate nascimento) {
        if (nascimento == null) {
            return null;
        }
        Period p = Period.between(nascimento, LocalDate.now());
        return p.getYears() * 12 + p.getMonths();
    }
}
