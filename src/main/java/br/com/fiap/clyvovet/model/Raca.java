package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.util.UUID;

/**
 * Uma raca do catalogo.
 *
 * POR QUE ESTA TABELA EXISTE
 *
 * `Animal.raca` era texto livre, e o banco acumulou o resultado disso:
 * "Cachorro"/"CAO"/"CANINO" para a mesma especie, "SRD"/"Vira"/"Vira-lata" para
 * a mesma ausencia de raca, "Pastor Alemao" sem acento ao lado de "Pastor
 * Alemao" com. Nada disso e erro de quem digitou -- e o que texto livre produz.
 *
 * Isso ja estava custando: o widget de saude preditiva da API .NET casa raca
 * para sugerir risco de doenca, e como nao havia padronizacao ele compara por
 * substring (`a.Contains(b) || b.Contains(a)`). Uma predisposicao cadastrada
 * como "Terrier" casa com Yorkshire, Bull e Fox Terrier -- racas com
 * predisposicoes diferentes.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_clyvo_raca")
public class Raca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EspecieAnimal especie;

    /** O que o tutor le na tela: "Golden Retriever". */
    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * O que o codigo usa: "golden-retriever". Minusculo, sem acento, hifenizado.
     *
     * E O CAMPO QUE FAZ ESTA TABELA VALER A PENA. Ele e o mesmo identificador em
     * tres lugares -- o arquivo da arte em pixel no app, a predisposicao na API
     * .NET e a busca no cliente. Ninguem precisa normalizar nada, porque quem
     * escolhe do catalogo ja recebe a chave pronta.
     *
     * UNIQUE no banco: duas linhas nao podem disputar o mesmo significado.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String chave;

    /**
     * Pre-preenche o porte no cadastro -- um Golden e grande, um Pug e pequeno,
     * e perguntar isso ao tutor e perguntar o que o catalogo ja sabe.
     *
     * Anulavel porque nem toda raca tem porte previsivel, e "sem raca definida"
     * por definicao nao tem. Os valores seguem o CHECK de `t_clyvo_animal.porte`
     * (PEQUENO, MEDIO, GRANDE) -- se divergirem, o Oracle rejeita o INSERT.
     */
    @Column(name = "porte_tipico", length = 20)
    private String porteTipico;

    /**
     * Raca desativada some do seletor mas continua valendo para quem ja a
     * escolheu. Mesma razao de `Servico.ativo`: apagar a linha levaria junto o
     * significado do que ja foi cadastrado.
     */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean ativo = true;
}
