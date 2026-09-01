package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
public class EventoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "data_evento")
    private LocalDate data;
    @Column(name = "hora_evento")
    private String hora;
    private String descricao;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinario_id")
    private Veterinario veterinario;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id")
    private Animal animal;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id")
    private Clinica clinica;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento")
    private TipoEvento tipoEvento;

    // ------------------------------------------------------------------
    // Colunas da V5, mapeadas agora.
    //
    // Elas existiam no banco desde a migration V5 e nenhuma linha de Java as
    // enxergava: sem campo na entidade e sem campo nos DTOs, a coluna era
    // inalcancavel pela API. O ddl-auto=validate nao reclama de coluna extra
    // no banco, entao a divergencia nao aparecia em teste nenhum.
    // ------------------------------------------------------------------

    /** Nasce AGENDADO quando o tutor marca; REALIZADO quando o vet registra o que ja houve. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_evento", nullable = false)
    private StatusEvento statusEvento = StatusEvento.AGENDADO;

    @Column(name = "data_retorno_previsto")
    private LocalDate dataRetornoPrevisto;

    /** Liga o RETORNO a consulta que o gerou. Auto-referencia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_origem_id")
    private EventoClinico eventoOrigem;

    /** Peso aferido no atendimento. Alimenta a serie por pet no resumo de seguranca. */
    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    // ------------------------------------------------------------------
    // Colunas da V6
    // ------------------------------------------------------------------

    /** De onde sai o valor cobrado. Nulo nos eventos anteriores ao catalogo. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    /** Nulo enquanto o atendimento nao foi concluido — nao e o mesmo que INDEFINIDO. */
    @Enumerated(EnumType.STRING)
    private Desfecho desfecho;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    /**
     * O unico jeito de nascer um atendimento AGENDADO.
     *
     * Antes, agendar e marcar retorno montavam o agregado a mao, cada um com
     * sua sequencia de setters: os mesmos oito campos, escritos duas vezes. Nada
     * acusava o campo esquecido -- foi assim que o servico sumiu do PATCH, e o
     * preco do atendimento passou a nao acompanhar a correcao.
     *
     * Com a construcao aqui, campo obrigatorio novo vira erro de compilacao nos
     * dois pontos de chamada, e nao uma omissao silenciosa num deles.
     *
     * O que NAO entra: eventoOrigem, peso, desfecho e retorno previsto. Sao do
     * ciclo de vida posterior, nao do nascimento — quem os define e concluir()
     * e agendarRetorno().
     */
    public static EventoClinico agendado(Animal animal, Clinica clinica, Servico servico,
                                         Veterinario veterinario, TipoEvento tipoEvento,
                                         LocalDate data, String hora, String descricao) {
        EventoClinico evento = new EventoClinico();
        evento.setAnimal(animal);
        evento.setClinica(clinica);
        evento.setServico(servico);
        evento.setVeterinario(veterinario);
        evento.setTipoEvento(tipoEvento);
        evento.setData(data);
        evento.setHora(hora);
        evento.setDescricao(descricao);
        evento.setStatusEvento(StatusEvento.AGENDADO);
        return evento;
    }
}