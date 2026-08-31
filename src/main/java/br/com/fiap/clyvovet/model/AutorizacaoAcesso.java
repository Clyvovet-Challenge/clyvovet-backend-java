package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O consentimento do tutor para uma clinica ler o historico do animal.
 *
 * Concedida no ato do agendamento, e nao por um pedido separado: o tutor ja
 * esta decidindo onde atender, e a liberacao do historico e parte dessa mesma
 * escolha. Eliminar o ciclo pedir-esperar-aprovar tira uma tela e uma espera do
 * caminho, sem tirar a decisao do tutor.
 *
 * Vive enquanto a relacao viver. Cada atendimento novo empurra validoAte para
 * 2 anos a frente; quem para de frequentar a clinica ve a autorizacao expirar
 * sozinha, sem precisar lembrar de revogar.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "autorizacao_acesso")
public class AutorizacaoAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAutorizacao status = StatusAutorizacao.VIGENTE;

    @Column(name = "concedida_em", nullable = false)
    private LocalDate concedidaEm = LocalDate.now();

    @Column(name = "valido_ate", nullable = false)
    private LocalDate validoAte;

    @Column(name = "revogada_em")
    private LocalDate revogadaEm;

    /** O agendamento que originou o consentimento. Rastro de onde ele veio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_evento_id")
    private EventoClinico origemEvento;

    /**
     * A expiracao e avaliada na leitura, e nao por um job que varre a tabela.
     *
     * Depender de varredura significaria que uma autorizacao vencida continua
     * valendo ate o job rodar — e a janela entre o vencimento e a varredura
     * seria acesso sem consentimento. Aqui ela deixa de valer no instante em
     * que a data passa.
     */
    public boolean vigenteEm(LocalDate data) {
        return status == StatusAutorizacao.VIGENTE && !data.isAfter(validoAte);
    }
}
