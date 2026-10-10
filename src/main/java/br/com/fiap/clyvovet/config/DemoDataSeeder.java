package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Clinica;
import br.com.fiap.clyvovet.model.DiaSemana;
import br.com.fiap.clyvovet.model.DisponibilidadeVeterinario;
import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.model.TipoEvento;
import br.com.fiap.clyvovet.model.Veterinario;
import br.com.fiap.clyvovet.repository.ClinicaRepository;
import br.com.fiap.clyvovet.repository.DisponibilidadeVeterinarioRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Deixa a VetCare Prime pronta para a demonstracao: catalogo de servicos e a
 * agenda da Camila Ferreira.
 *
 * POR QUE UM PERFIL PROPRIO, E NAO A V11 NEM O DevDataSeeder
 * A V11 explica por que a VetCare ficou sem catalogo: ela e a clinica da suite
 * de testes, e os testes de agendamento montam a grade da Camila e contam vagas
 * exatas. Semear ali quebrou 26 testes. O DevDataSeeder tambem nao serve,
 * porque a suite roda no perfil dev.
 *
 * O perfil demo so liga quando alguem pede: SPRING_PROFILES_ACTIVE=dev,demo
 * (ou mysql,local,demo no Docker de desenvolvimento). Nem a suite nem a
 * producao o ativam. E o mesmo codigo, com o cenario montado so na noite da
 * apresentacao.
 *
 * Idempotente: cada bloco so roda se a clinica ainda nao tiver catalogo, ou a
 * Camila ainda nao tiver agenda. Subir duas vezes nao duplica nada.
 */
@Slf4j
@Configuration
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder {

    // IDs fixos vindos da migration V2__seed_inicial.sql
    private static final UUID CLINICA_VETCARE = UUID.fromString("11111111-1111-1111-1111-000000000001");
    private static final UUID VET_CAMILA = UUID.fromString("33333333-3333-3333-3333-000000000001");

    /** Mesma vigencia da V11: vale de agora em diante, sem data para expirar. */
    private static final LocalDate VIGENCIA_INICIO = LocalDate.of(2026, 1, 1);

    private final ClinicaRepository clinicaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final ServicoRepository servicoRepository;
    private final DisponibilidadeVeterinarioRepository disponibilidadeRepository;

    @Bean
    public ApplicationRunner semearDemonstracao() {
        return args -> {
            semearCatalogoDaVetcare();
            semearAgendaDaCamila();
            log.info("Perfil demo: VetCare Prime com catalogo e a Camila com agenda de segunda a sabado");
        };
    }

    private void semearCatalogoDaVetcare() {
        if (!servicoRepository.daClinica(CLINICA_VETCARE).isEmpty()) {
            return;
        }
        Clinica vetcare = clinicaRepository.findById(CLINICA_VETCARE).orElseThrow();
        // Precos e duracoes na mesma faixa das clinicas da V11.
        salvarServico(vetcare, "Consulta clinica geral", TipoEvento.CONSULTA, "180.00", 30);
        salvarServico(vetcare, "Retorno de consulta", TipoEvento.RETORNO, "0.00", 20);
        salvarServico(vetcare, "Vacinacao", TipoEvento.VACINA, "95.00", 15);
    }

    private void semearAgendaDaCamila() {
        if (!disponibilidadeRepository.findByVeterinarioIdOrderByDiaSemanaAscHoraInicioAsc(VET_CAMILA).isEmpty()) {
            return;
        }
        Veterinario camila = veterinarioRepository.findById(VET_CAMILA).orElseThrow();
        for (DiaSemana dia : EnumSet.range(DiaSemana.SEGUNDA, DiaSemana.SABADO)) {
            salvarFaixa(camila, dia, "09:00", "12:00");
            salvarFaixa(camila, dia, "14:00", "18:00");
        }
    }

    private void salvarServico(Clinica clinica, String nome, TipoEvento tipo, String preco, int duracaoMinutos) {
        servicoRepository.save(new Servico(null, clinica, nome, tipo, new BigDecimal(preco), duracaoMinutos, true));
    }

    private void salvarFaixa(Veterinario veterinario, DiaSemana dia, String inicio, String fim) {
        disponibilidadeRepository.save(
                new DisponibilidadeVeterinario(null, veterinario, dia, inicio, fim, VIGENCIA_INICIO, null));
    }
}
