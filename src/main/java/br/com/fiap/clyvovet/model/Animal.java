package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
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
}