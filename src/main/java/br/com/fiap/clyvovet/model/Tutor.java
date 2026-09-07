package br.com.fiap.clyvovet.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "t_clyvo_tutor")
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String cpf;
    private String nome;
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private Sexo sexo;
    private String email;
    private String telefone;
    @Embedded
    private Endereco endereco;
}