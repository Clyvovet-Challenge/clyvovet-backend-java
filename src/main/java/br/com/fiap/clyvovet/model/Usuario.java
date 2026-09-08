package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Identidade de quem faz login, separada das entidades de dominio.
 *
 * O vinculo e opcional e, na pratica, mutuamente exclusivo — um por perfil:
 * TUTOR aponta para tutor, VETERINARIO para veterinario, ADMIN_CLINICA para
 * clinica, e o ADMIN da plataforma para nenhum. E o vinculo com Tutor que
 * viabiliza a regra de ownership — um tutor so enxerga os proprios pets.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_clyvo_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt. Nunca exposto em nenhum DTO de resposta. */
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    /** Mapeado como NUMBER(1) para funcionar igual em Oracle e H2. */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veterinario_id")
    private Veterinario veterinario;

    /**
     * A clinica que este usuario ADMINISTRA.
     *
     * <p>Nao confundir com a clinica ONDE ele trabalha: essa continua vindo por
     * {@code veterinario.clinica}. Um veterinario atende numa clinica; o
     * ADMIN_CLINICA responde por ela. Sao alcances diferentes, e por isso sao
     * dois caminhos.</p>
     *
     * <p>Fica nulo em todos os outros perfis. O banco cobra a coerencia:
     * {@code chk_usuario_clinica} recusa ADMIN_CLINICA sem clinica.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinica_id")
    private Clinica clinica;

    public boolean estaBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }
}
