package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.AlertaClinico;

import java.util.List;
import java.util.UUID;

public interface AlertaClinicoRepository extends RepositorioBase<AlertaClinico> {

    /** O que entra no resumo de seguranca: so o que ainda vale. */
    List<AlertaClinico> findByAnimalIdAndAtivoTrueOrderByTipoAsc(UUID animalId);

    default AlertaClinico obterPorId(UUID id) {
        return obterPorId(id, Recurso.ALERTA_CLINICO);
    }
}
