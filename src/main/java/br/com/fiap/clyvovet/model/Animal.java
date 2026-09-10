package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_clyvo_animal")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nome;
    /**
     * O que o tutor ve. Com `racaDoCatalogo` preenchido, e copia do catalogo;
     * sem ele, e o texto que o tutor digitou.
     *
     * Continua String -- e nao virou so a FK -- porque e o que o widget de saude
     * preditiva da API .NET le, e o que o AnimalResponse ja entrega ao app.
     * Trocar o tipo quebraria os dois sem ganho nenhum.
     */
    private String raca;
    private String especie;
    /**
     * 'PEQUENO', 'MEDIO' ou 'GRANDE'. Ver {@link #setPorte(String)}: a caixa
     * alta nao e convencao, e o que o CHECK do banco exige.
     */
    private String porte;
    private String cor;

    /**
     * A raca no catalogo, quando ela esta la.
     *
     * ANULAVEL DE PROPOSITO: sao 200 e poucas racas de cachorro e o catalogo
     * lista as comuns, entao "outra raca" precisa continuar existindo. Nulo aqui
     * significa "o tutor digitou algo que o catalogo nao cobre", e nesse caso o
     * texto em `raca` e a unica informacao que resta -- "Labrador misto" diz algo
     * que "Labrador" nao diz.
     *
     * E daqui que sai a `chave` que o app usa para escolher a arte em pixel do
     * animal, e que a .NET vai usar para casar predisposicao sem adivinhar.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raca_id")
    private Raca racaDoCatalogo;
    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private SexoAnimal sexo;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Column(name = "observacoes")
    private String observacao;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    /**
     * Numero do microchip, padrao ISO 11784/11785 (15 digitos).
     *
     * IDENTIFICA, NAO AUTORIZA. Ele esta impresso na carteira de vacinacao e no
     * contrato de adocao, e qualquer leitor de pet shop ou canil o le — como
     * senha nao valeria nada. O que credencia a leitura do resumo de seguranca
     * e a autenticacao do veterinario; o chip so diz de qual animal se trata.
     */
    @Column(unique = true)
    private String microchip;

    /** Compoe o resumo de seguranca. Nulo = nao informado, que nao e o mesmo que nao. */
    @Convert(converter = NumericBooleanConverter.class)
    private Boolean castrado;

    /**
     * O tutor pode desligar o resumo de seguranca (nivel 1).
     *
     * Nasce LIGADO: o valor do resumo esta justamente em estar disponivel na
     * emergencia, e um opt-in silencioso significaria que quase ninguem o teria
     * quando precisasse. Mas forcar seria paternalista — o dado e do tutor —,
     * entao desligar e possivel, com aviso do que se perde.
     */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "resumo_seguranca_ativo")
    private Boolean resumoDeSegurancaAtivo = Boolean.TRUE;

    /**
     * O porte, sempre em caixa alta.
     *
     * <h2>Por que a normalizacao mora AQUI, e nao no mapper</h2>
     *
     * <p>O {@code chk_animal_porte} da V1 compara com 'PEQUENO', 'MEDIO' e
     * 'GRANDE', e o CHECK do <b>Oracle e sensivel a caixa</b>. O formulario do
     * app manda "Pequeno". Normalizar e o que faz o mesmo cadastro passar nos
     * dois bancos.</p>
     *
     * <p>Isto ficava no {@code AnimalMapper}, e cobria apenas os caminhos que
     * passam por ele — o POST e o PATCH. <b>O terceiro caminho escapava:</b> a
     * solicitacao de alteracao aprovada chama {@code aplicarEm}, que escreve
     * direto na entidade. Verificado contra o MySQL do docker-compose: um pedido
     * com {@code "porte":"Pequeno"} aprovado pelo tutor gravava {@code 'Pequeno'}
     * num animal que era {@code 'PEQUENO'}.</p>
     *
     * <p>No MySQL o dano fica escondido — a colacao padrao e {@code _ai_ci}, e o
     * CHECK aceita. <b>No Oracle a aprovacao falharia</b> com violacao de
     * constraint, e o tutor leria um erro de banco ao aceitar uma alteracao que o
     * veterinario pediu. Um bug que so aparece no banco que a suite nao usa.</p>
     *
     * <p>No setter, nenhum caminho de escrita pode esquecer: quem grava porte
     * grava normalizado, hoje e no proximo campo que alguem ligar a ele.</p>
     */
    public void setPorte(String porte) {
        this.porte = porteNormalizado(porte);
    }

    /**
     * A normalizacao, exposta para quem PROPOE um porte sem ser o animal.
     *
     * <p>Existe por causa da {@code SolicitacaoAlteracao}: se o pedido guardasse
     * "Pequeno" cru, o diff que o tutor aprova mostraria
     * {@code porte: PEQUENO -> Pequeno} — o mesmo porte apresentado como
     * mudanca.</p>
     */
    public static String porteNormalizado(String porte) {
        return porte == null ? null : porte.trim().toUpperCase();
    }
}
