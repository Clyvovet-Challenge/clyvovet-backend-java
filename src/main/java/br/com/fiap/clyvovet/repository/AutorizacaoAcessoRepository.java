package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.AutorizacaoAcesso;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutorizacaoAcessoRepository extends RepositorioBase<AutorizacaoAcesso> {

    /**
     * A autorizacao daquela clinica sobre aquele animal, se existir.
     *
     * Devolve independente do status: quem chama decide se ela ainda vale
     * (AutorizacaoAcesso.vigenteEm). Filtrar por VIGENTE aqui esconderia a
     * linha revogada — e e justamente ela que precisa ser encontrada para ser
     * reativada num agendamento novo, em vez de duplicada.
     */
    Optional<AutorizacaoAcesso> findByAnimalIdAndClinicaId(UUID animalId, UUID clinicaId);

    /** O que o tutor ve quando pergunta "quem tem acesso aos meus animais?". */
    @Query("""
            SELECT a FROM AutorizacaoAcesso a
            WHERE a.animal.tutor.id = :tutorId
            ORDER BY a.animal.nome, a.clinica.nome
            """)
    List<AutorizacaoAcesso> doTutor(@Param("tutorId") UUID tutorId);

    default AutorizacaoAcesso obterPorId(UUID id) {
        return obterPorId(id, Recurso.AUTORIZACAO);
    }
}
