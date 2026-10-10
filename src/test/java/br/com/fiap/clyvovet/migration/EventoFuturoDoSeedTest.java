package br.com.fiap.clyvovet.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A V21 promete que nenhum evento do seed fica no futuro marcado como REALIZADO.
 *
 * O caso que se viu foi a consulta de 15/12/2026 do Bolinha com a Camila: a V2 a
 * semeou antes de existir status, e a V5 deu 'REALIZADO' por default a tudo que
 * ja estava gravado. O atendimento que ainda nao aconteceu aparecia como feito,
 * e a lista da Camila comecava sem nenhum agendamento. A regra geral, no fim do
 * teste, achou o segundo caso: o exame de 22/12/2026.
 *
 * Roda nos dois conjuntos, porque o erro estava nos dois.
 */
class EventoFuturoDoSeedTest {

    private static final String CONSULTA_DE_DEZEMBRO = "55555555-5555-5555-5555-000000000006";
    private static final String EXAME_DE_DEZEMBRO    = "55555555-5555-5555-5555-000000000009";

    @ParameterizedTest(name = "conjunto {0}")
    @CsvSource({"oracle, Oracle", "mysql, MySQL"})
    void evento_do_seed_no_futuro_esta_agendado(String conjunto, String modo) {
        var ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:" + conjunto + "_" + UUID.randomUUID() + ";MODE=" + modo + ";DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/" + conjunto)
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(ds);

        assertThat(jdbc.queryForObject(
                "SELECT status_evento FROM t_clyvo_evento_clinico WHERE id = ?",
                String.class, CONSULTA_DE_DEZEMBRO)).isEqualTo("AGENDADO");
        assertThat(jdbc.queryForObject(
                "SELECT status_evento FROM t_clyvo_evento_clinico WHERE id = ?",
                String.class, EXAME_DE_DEZEMBRO)).isEqualTo("AGENDADO");

        // e a regra geral, que pega um proximo caso igual: nada no futuro como feito
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_clyvo_evento_clinico "
                        + "WHERE status_evento = 'REALIZADO' AND data_evento > DATE '2026-10-09'",
                Integer.class)).isZero();
    }
}
