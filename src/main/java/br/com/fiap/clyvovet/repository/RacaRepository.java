package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.model.EspecieAnimal;
import br.com.fiap.clyvovet.model.Raca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RacaRepository extends JpaRepository<Raca, UUID> {

    /** O seletor do cadastro: so o que esta ativo, em ordem alfabetica. */
    List<Raca> findByAtivoTrueOrderByNomeAsc();

    List<Raca> findByEspecieAndAtivoTrueOrderByNomeAsc(EspecieAnimal especie);

    /** Busca pela chave estavel -- o caminho que a arte e a .NET usam. */
    Optional<Raca> findByChave(String chave);
}
