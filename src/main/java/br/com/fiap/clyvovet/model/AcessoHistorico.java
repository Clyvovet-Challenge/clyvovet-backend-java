package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Registro de que alguem leu o historico de um animal.
 *
 * UMA LINHA POR (usuario, animal, dia), com contador — e nao uma por
 * requisicao. O veterinario abre a tela varias vezes durante a consulta e o
 * front repagina; auditar cada GET encheria a tabela de ruido e a tornaria
 * ilegivel justamente para quem ela existe: o tutor. O que interessa a ele e
 * "a Dra. Camila leu o historico do Thor em 12/09", nao quantas vezes rolou a
 * pagina.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "acesso_historico")
public class AcessoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id")
    private Clinica clinica;

    @Column(nullable = false)
    private LocalDate dia = LocalDate.now();

    /** 1 = resumo de seguranca, 2 = historico completo. */
    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false)
    private Integer vezes = 1;

    /** Quebra de vidro: acesso sem consentimento, com motivo obrigatorio. */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean emergencial = false;

    @Column(length = 500)
    private String motivo;
}
