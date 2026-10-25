package br.com.fiap.clyvovet.config;

import br.com.fiap.clyvovet.model.Servico;
import br.com.fiap.clyvovet.repository.DisponibilidadeVeterinarioRepository;
import br.com.fiap.clyvovet.repository.ServicoRepository;
import br.com.fiap.clyvovet.service.AgendamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O perfil demo deixa a VetCare pronta para a demonstracao: o tutor agenda com
 * a Camila sem cadastrar nada antes.
 *
 * POR QUE O BANCO DESTE TESTE E OUTRO
 * O H2 da suite e "clyvovetdb" com DB_CLOSE_DELAY=-1: um banco em memoria
 * compartilhado por todos os contextos Spring da mesma JVM. Se este teste o
 * usasse, a grade da Camila vazaria para os testes que contam vagas exatas da
 * VetCare -- justamente o motivo de este seed nao estar na V11. O nome
 * aleatorio da a este contexto um banco so dele.
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:demo_${random.uuid};MODE=Oracle;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"dev", "demo"})
class DemoDataSeederTest {

    private static final UUID CLINICA_VETCARE = UUID.fromString("11111111-1111-1111-1111-000000000001");
    private static final UUID VET_CAMILA = UUID.fromString("33333333-3333-3333-3333-000000000001");

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private DisponibilidadeVeterinarioRepository disponibilidadeRepository;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    @Qualifier("semearDemonstracao")
    private ApplicationRunner semearDemonstracao;

    @Test
    void vetcare_ganha_catalogo_de_servicos() {
        assertThat(servicoRepository.ativosDaClinica(CLINICA_VETCARE))
                .extracting(Servico::getNome)
                .containsExactlyInAnyOrder("Consulta clinica geral", "Retorno de consulta", "Vacinacao");
    }

    @Test
    void camila_atende_de_segunda_a_sabado() {
        assertThat(disponibilidadeRepository.findByVeterinarioIdOrderByDiaSemanaAscHoraInicioAsc(VET_CAMILA))
                .hasSize(12);
    }

    @Test
    void tutor_encontra_vagas_com_a_camila() {
        Servico consulta = servicoRepository.ativosDaClinica(CLINICA_VETCARE).stream()
                .filter(s -> s.getNome().equals("Consulta clinica geral"))
                .findFirst().orElseThrow();
        LocalDate segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        assertThat(agendamentoService.vagas(consulta.getId(), VET_CAMILA, segunda, segunda.plusDays(5)))
                .isNotEmpty();
    }

    @Test
    void rodar_de_novo_nao_duplica() throws Exception {
        semearDemonstracao.run(new DefaultApplicationArguments());

        assertThat(servicoRepository.ativosDaClinica(CLINICA_VETCARE)).hasSize(3);
        assertThat(disponibilidadeRepository.findByVeterinarioIdOrderByDiaSemanaAscHoraInicioAsc(VET_CAMILA))
                .hasSize(12);
    }
}
