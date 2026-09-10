package br.com.fiap.clyvovet.repository;

import br.com.fiap.clyvovet.exception.Recurso;
import br.com.fiap.clyvovet.model.DocumentoClinico;
import br.com.fiap.clyvovet.repository.projecao.DocumentoResumo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentoClinicoRepository extends RepositorioBase<DocumentoClinico> {

    /**
     * Os documentos do animal, do mais recente para o mais antigo, SEM o arquivo.
     *
     * <p>Os tres LEFT JOIN sao obrigatorios e nao estilo. A juncao implicita do
     * JPQL ({@code d.clinica.nome} escrito direto) e INNER: o documento que o
     * proprio tutor enviou tem {@code clinica_id} nulo e <b>desapareceria da
     * lista</b> — o dono do prontuario seria o unico a nao ver o que ele mesmo
     * anexou. O mesmo vale para o evento, que e nulo no anexo de fora.</p>
     */
    @Query("""
            SELECT new br.com.fiap.clyvovet.repository.projecao.DocumentoResumo(
                d.id, d.titulo, d.nomeArquivo, d.tipoConteudo, d.tamanhoBytes,
                d.enviadoEm, u.email, c.nome, e.id)
            FROM DocumentoClinico d
            LEFT JOIN d.enviadoPor u
            LEFT JOIN d.clinica c
            LEFT JOIN d.evento e
            WHERE d.animal.id = :animalId
            ORDER BY d.enviadoEm DESC
            """)
    List<DocumentoResumo> resumosDoAnimal(@Param("animalId") UUID animalId);

    /**
     * Este arquivo ja esta no prontuario deste animal?
     *
     * <p>Compara o hash, e nao o nome: dois toques no botao de enviar produzem a
     * mesma requisicao duas vezes, e "Hemograma.pdf" e "Hemograma (1).pdf" sao o
     * mesmo laudo com nomes que o sistema de arquivos inventou.</p>
     */
    boolean existsByAnimalIdAndSha256(UUID animalId, String sha256);

    long countByAnimalId(UUID animalId);

    default DocumentoClinico obterPorId(UUID id) {
        return obterPorId(id, Recurso.DOCUMENTO);
    }
}
