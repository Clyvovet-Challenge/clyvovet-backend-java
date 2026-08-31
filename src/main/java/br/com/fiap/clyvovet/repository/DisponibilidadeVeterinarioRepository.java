package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.DiaSemana;
import br.com.fiap.clyvovet.model.DisponibilidadeVeterinario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DisponibilidadeVeterinarioRepository extends RepositorioBase<DisponibilidadeVeterinario> {

    /**
     * As faixas do veterinario naquele dia da semana que estao vigentes na data.
     *
     * O filtro de vigencia entra na consulta, e nao em memoria, porque a grade
     * acumula versoes: quando o horario muda, a linha antiga ganha vigencia_fim
     * e continua na tabela para nao invalidar retroativamente o que ja foi
     * agendado sob ela.
     */
    @Query("""
            SELECT d FROM DisponibilidadeVeterinario d
            WHERE d.veterinario.id = :veterinarioId
              AND d.diaSemana = :diaSemana
              AND d.vigenciaInicio <= :data
              AND (d.vigenciaFim IS NULL OR d.vigenciaFim >= :data)
            ORDER BY d.horaInicio
            """)
    List<DisponibilidadeVeterinario> vigentesEm(
            @Param("veterinarioId") UUID veterinarioId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("data") LocalDate data);

    List<DisponibilidadeVeterinario> findByVeterinarioIdOrderByDiaSemanaAscHoraInicioAsc(UUID veterinarioId);

    default DisponibilidadeVeterinario obterPorId(UUID id) {
        return obterPorId(id, Recurso.DISPONIBILIDADE);
    }
}
