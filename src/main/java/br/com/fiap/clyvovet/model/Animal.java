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
}