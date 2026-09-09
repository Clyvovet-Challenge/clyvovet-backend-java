package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_clyvo_clinica")
public class Clinica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nome;
    private String cnpj;
    private String telefone;
    private String email;
    @Embedded
    private Endereco endereco;

    /**
     * Coordenada para o mapa do tutor (ver V13).
     *
     * MORA AQUI, E NAO EM Endereco.
     *
     * Endereco e @Embeddable e Tutor e Veterinario tambem o embutem. Por a
     * coordenada la criaria coluna em t_clyvo_tutor e t_clyvo_veterinario
     * tambem -- e o Hibernate passaria a exigi-la nas tres tabelas, derrubando
     * o contexto inteiro se a migracao mexesse so numa. Aconteceu: 278 testes
     * de contexto cairam de uma vez com "Column V1_0.LATITUDE not found" em
     * t_clyvo_veterinario.
     *
     * E, mesmo que funcionasse, expor a coordenada da CASA do tutor numa
     * resposta de API e o oposto do que se quer. Quem tem endereco que o
     * publico precisa achar no mapa e o estabelecimento.
     *
     * Nula e valida: clinica sem coordenada nao aparece no mapa, mas continua
     * na lista e continua agendavel.
     *
     * BigDecimal, e nao Double -- coordenada e decimal exato, e a coluna e
     * DECIMAL(9,6)/NUMBER(9,6).
     */
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

}
