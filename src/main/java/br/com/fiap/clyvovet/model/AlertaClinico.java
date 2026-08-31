package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Alergia, condicao cronica, medicacao continua ou aviso critico de um animal.
 *
 * E o conteudo do NIVEL 1 do fluxo C: o que qualquer veterinario autenticado
 * alcanca pelo numero do microchip, sem consentimento previo, porque e o que
 * decide um atendimento de urgencia em animal que ele nunca viu.
 *
 * A estrutura e deliberada. O resumo de seguranca precisa ser derivado de
 * dados estruturados, nunca de texto livre digitado a parte: um campo
 * "observacoes" mantido a mao envelhece, e um resumo de alergias desatualizado
 * e pior que nenhum. Por isso alerta e uma tabela, e nao uma coluna em animal.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "alerta_clinico")
public class AlertaClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemAlerta origem;

    @Column(name = "registrado_em", nullable = false)
    private LocalDate registradoEm = LocalDate.now();

    /**
     * Alerta resolvido some do resumo mas fica no historico. Uma alergia
     * registrada por engano precisa deixar de aparecer sem que o registro de
     * que ela foi registrada — e por quem — desapareca junto.
     */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean ativo = true;
}
