package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um arquivo do prontuario: o PDF do laudo, a foto do raio-X.
 *
 * <p>A entidade guarda o BYTE junto com o metadado, e isso tem uma consequencia
 * que o codigo precisa respeitar: <b>carregar um DocumentoClinico carrega o
 * arquivo inteiro para a memoria</b>. Oito megabytes por linha numa listagem de
 * dez documentos sao oitenta megabytes para desenhar uma lista de nomes.</p>
 *
 * <p>Por isso a listagem NAO usa esta entidade — ela passa por
 * {@code DocumentoClinicoRepository#resumosDoAnimal}, que projeta apenas as
 * colunas de metadado. Esta classe e carregada por id, uma vez, no download.</p>
 *
 * <p>A alternativa idiomatica seria {@code @Basic(fetch = LAZY)} no blob. Ela foi
 * evitada de proposito: o lazy de atributo depende de instrumentacao de bytecode
 * para valer, e falha em silencio quando ela nao acontece — o codigo continuaria
 * correto na aparencia e traria o blob de qualquer jeito. A projecao e explicita
 * e nao tem esse modo de falha.</p>
 */
@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "t_clyvo_documento_clinico")
public class DocumentoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /**
     * O atendimento que gerou o documento, quando existe um.
     *
     * <p>LAZY, e nao EAGER como o resto do projeto: o download carrega esta
     * entidade so para escrever bytes na resposta, e o evento nao entra nela.
     * EAGER aqui traria evento, veterinario, clinica e servico atras de um
     * arquivo que ninguem vai ler.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoClinico evento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enviado_por", nullable = false)
    private Usuario enviadoPor;

    /** A clinica que enviou. Nulo quando quem enviou foi o proprio tutor. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id")
    private Clinica clinica;

    /** O que o tutor le na lista: "Hemograma completo", "Raio-X pata dianteira". */
    @Column(nullable = false, length = 150)
    private String titulo;

    /** O nome original do arquivo, que volta no Content-Disposition do download. */
    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    /**
     * SHA-256 do conteudo, em hexadecimal.
     *
     * <p>Um laudo e prova, e prova sem integridade verificavel e so um arquivo.
     * O hash tambem responde "este documento ja foi enviado?" sem comparar
     * megabytes — e o que barra o reenvio duplicado quando o toque duplo do
     * usuario dispara dois uploads.</p>
     */
    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm = LocalDateTime.now();

    /**
     * O arquivo.
     *
     * <p>O {@code length} nao e decorativo, e nao e sobre validar entrada: <b>e o
     * que decide o TIPO da coluna que o Hibernate espera no MySQL</b>. O dialeto
     * escolhe entre {@code tinyblob}, {@code blob}, {@code mediumblob} e
     * {@code longblob} pelo tamanho declarado, e o padrao de {@code @Column} e
     * 255 — ou seja, {@code tinyblob}.</p>
     *
     * <p>Sem esta linha a aplicacao <b>nao sobe</b> no perfil mysql. O
     * {@code ddl-auto=validate} compara o tipo esperado com o que a V17 criou
     * (LONGBLOB) e aborta o contexto inteiro:</p>
     *
     * <pre>
     * Schema-validation: wrong column type encountered in column [conteudo]
     *   in table [t_clyvo_documento_clinico];
     *   found [longblob (Types#LONGVARBINARY)], but expecting [tinyblob (Types#BLOB)]
     * </pre>
     *
     * <p>Verificado contra o MySQL do docker-compose — e invisivel na suite de
     * testes, que roda em H2 com {@code ddl-auto=none} e nunca compara os tipos.
     * E a mesma familia de problema que ja obrigou o
     * {@code application-mysql.properties} a fixar o tipo JDBC de UUID e de enum.
     * No Oracle o {@code length} e ignorado: BLOB nao tem tamanho declarado.</p>
     */
    @Lob
    @Column(nullable = false, length = TAMANHO_MAXIMO_DA_COLUNA)
    private byte[] conteudo;

    /**
     * Acima dos 16 MB do {@code mediumblob}, para que o MySQL resolva em
     * {@code longblob} — o tipo que a V17 declara.
     *
     * <p>Nao e o limite da feature: quem recusa arquivo grande e o
     * {@code DocumentoClinicoService}, em 8 MB. Este numero so descreve a
     * coluna.</p>
     */
    private static final int TAMANHO_MAXIMO_DA_COLUNA = 64 * 1024 * 1024;
}
