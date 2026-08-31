package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.Servico;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ServicoRepository extends RepositorioBase<Servico> {

    /** Catalogo visivel ao tutor: so o que a clinica realmente oferece hoje. */
    @Query("SELECT s FROM Servico s WHERE s.clinica.id = :clinicaId AND s.ativo = true ORDER BY s.nome")
    List<Servico> ativosDaClinica(@Param("clinicaId") UUID clinicaId);

    default Servico obterPorId(UUID id) {
        return obterPorId(id, Recurso.SERVICO);
    }
}
