package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um servico do catalogo de uma clinica.
 *
 * E a peca que responde "esta clinica oferece este servico?" — pergunta que o
 * agendamento pelo tutor precisa fazer e que ate aqui nao tinha contra o que
 * ser respondida.
 *
 * O preco mora aqui, e nao no evento clinico, e isso resolve de brinde um furo
 * antigo do fluxo de cobranca: nao existia valor no atendimento para comparar
 * com o pagamento recebido. Agora existe, e vem do catalogo.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    @Column(nullable = false)
    private BigDecimal preco;

    /** Quanto tempo o servico ocupa na agenda. Sustenta a deteccao de colisao. */
    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    /**
     * Servico desativado nao pode ser agendado, mas continua referenciado pelos
     * atendimentos passados. Por isso e uma bandeira, e nao um DELETE: apagar a
     * linha levaria junto o preco historico de tudo que ja foi cobrado por ela.
     */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean ativo = true;
}
