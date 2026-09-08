package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O pedido do veterinário para alterar o cadastro de um animal.
 *
 * <p>Existe porque a regra do produto é que o veterinário só altera os dados do
 * animal <b>"desde que tenha uma confirmação e autorização do dono"</b>, e esse
 * consentimento não existia. O que existia — {@link AutorizacaoAcesso} — responde
 * outra pergunta: autoriza uma clínica a <i>ler</i> o histórico clínico.</p>
 *
 * <h2>O pedido é um PATCH guardado</h2>
 *
 * <p>Os campos abaixo espelham os editáveis do {@link Animal}, todos anuláveis.
 * Só os preenchidos fazem parte do pedido, exatamente como o corpo de um PATCH.
 * O tutor vê valores concretos — "raça: de Poodle para Poodle Toy" — e não uma
 * permissão abstrata.</p>
 *
 * <h2>Aprova-se o conteúdo, não a permissão</h2>
 *
 * <p>Não há janela em que o veterinário "pode editar". Ele propõe, o tutor aceita
 * ou recusa, e a aprovação aplica os valores na mesma transação. A diferença
 * importa: uma permissão temporária precisa ser vigiada e revogada; um valor
 * aprovado fica auditável para sempre — quem pediu, o que pediu, quem respondeu e
 * quando.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_clyvo_solicitacao_alteracao")
public class SolicitacaoAlteracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private Veterinario veterinario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    /**
     * Obrigatória. É o que o tutor lê antes de decidir, e sem ela o pedido é um
     * "deixa eu mexer" sem contexto — que é justamente o que esta tabela evita.
     */
    @Column(nullable = false, length = 500)
    private String justificativa;

    // ---------------------------------------------------------------
    // Os valores propostos. Nulo = não faz parte deste pedido.
    // ---------------------------------------------------------------

    private String nome;
    private String raca;
    private String especie;
    private String porte;
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private SexoAnimal sexo;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    private String microchip;

    /**
     * INT no banco, e não TINYINT: o converter entrega Integer ao JDBC e o
     * {@code ddl-auto=validate} reprova TINYINT contra INTEGER. É a mesma regra
     * das outras sete colunas booleanas deste schema.
     */
    @Convert(converter = NumericBooleanConverter.class)
    private Boolean castrado;

    @Column(name = "observacoes", length = 1000)
    private String observacao;

    // ---------------------------------------------------------------
    // Resposta
    // ---------------------------------------------------------------

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    /**
     * Quem respondeu. No fluxo normal é o dono, mas o ADMIN da plataforma também
     * pode — e aí é preciso saber qual dos dois foi.
     */
    @Column(name = "respondido_por")
    private UUID respondidoPor;

    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    /** Um pedido só responde uma vez. */
    public boolean estaPendente() {
        return status == StatusSolicitacao.PENDENTE;
    }

    /**
     * Um pedido sem nenhum campo preenchido não altera nada — é ruído na caixa de
     * pedidos do tutor, e aprová-lo seria um "sim" a coisa nenhuma.
     */
    public boolean semNenhumCampo() {
        return nome == null && raca == null && especie == null && porte == null
                && cor == null && sexo == null && dataNascimento == null
                && microchip == null && castrado == null && observacao == null;
    }

    /**
     * Grava os valores propostos no animal.
     *
     * <p>Só os campos preenchidos são tocados — o mesmo contrato do PATCH: o que
     * não veio no pedido continua como está, e nunca é apagado.</p>
     */
    public void aplicarEm(Animal alvo) {
        if (nome != null) alvo.setNome(nome);
        if (raca != null) alvo.setRaca(raca);
        if (especie != null) alvo.setEspecie(especie);
        if (porte != null) alvo.setPorte(porte);
        if (cor != null) alvo.setCor(cor);
        if (sexo != null) alvo.setSexo(sexo);
        if (dataNascimento != null) alvo.setDataNascimento(dataNascimento);
        if (microchip != null) alvo.setMicrochip(microchip);
        if (castrado != null) alvo.setCastrado(castrado);
        if (observacao != null) alvo.setObservacao(observacao);
    }
}
