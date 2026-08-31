package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private FormaPagamento formaPagamento;
    private BigDecimal valor;
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
    private String descricao;
    @Column(name = "notas")
    private String observacao;
    @ManyToOne
    @JoinColumn(name = "evento_id")
    private EventoClinico eventoClinico;
    /**
     * Todo pagamento nasce PENDENTE. As transicoes sao acoes proprias --
     * /confirmar e /estornar --, onde P2, P4, P7 e P11 sao verificadas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento")
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;
}