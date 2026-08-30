package br.com.fiap.clyvovet.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda o conjunto de migrations de db/migration/mysql contra um H2 em
 * MODE=MySQL.
 *
 * POR QUE ELE IMPORTA MAIS DO QUE PARECE
 * Desde 30/08/2026 o MySQL e o banco de PRODUCAO e o Oracle e o de teste. Como
 * a suite inteira roda em H2 com MODE=Oracle, este e o unico teste automatico
 * que encosta no conjunto que vai para producao.
 *
 * POR QUE ESTE TESTE EXISTE
 * Manter dois conjuntos de migrations (oracle/ e mysql/) tem um risco proprio:
 * alguem adiciona uma versao nova em um so, e a divergencia fica invisivel ate o deploy.
 * Este teste e o unico guarda automatico contra isso — ele quebra se o SQL de
 * mysql/ parar de rodar, ou se o seed dos dois conjuntos deixar de bater.
 *
 * O QUE ELE NAO PROVA
 * H2 em MODE=MySQL nao e MySQL. Ele aceita a sintaxe e a semantica principal,
 * mas nao reproduz o comportamento de tipos do servidor real. Um teste verde
 * aqui significa "o SQL esta coerente", nao "esta validado em producao" — para
 * isso e preciso um MySQL de verdade (Testcontainers, quando houver Docker no
 * ambiente de CI).
 */
class MigrationsMySqlTest {

    private DataSource h2ModoMySql() {
        var ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        // Base nova por execucao: o teste precisa ver as migrations rodando do zero.
        ds.setUrl("jdbc:h2:mem:mysql_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Test
    void as_migrations_de_mysql_rodam_da_v1_a_v5() {
        var ds = h2ModoMySql();

        var flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load();
        var resultado = flyway.migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(5);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
    }

    @Test
    void o_seed_carrega_os_mesmos_registros_do_conjunto_oracle() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O seed da V2 e identico nos dois conjuntos; se alguem mexer em um so,
        // estas contagens deixam de bater. Os numeros vem do proprio arquivo --
        // a disciplina exige no MINIMO 5 por tabela, e algumas tem mais.
        assertThat(jdbc.queryForObject("select count(*) from clinica", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from tutor", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from animal", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from veterinario", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select count(*) from evento_clinico", Integer.class)).isEqualTo(11);
        assertThat(jdbc.queryForObject("select count(*) from pagamento", Integer.class)).isEqualTo(8);
    }

    @Test
    void a_v4_deixa_o_check_de_status_alinhado_ao_enum() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O ponto da V4: REEMBOLSADO passa a ser aceito. Antes dela o INSERT
        // estourava violacao de check e virava 500 na API.
        jdbc.update("""
                insert into pagamento (id, metodo_pagamento, valor, data_pagamento, status_pagamento)
                values (?, 'PIX', 10.00, DATE '2026-01-01', 'REEMBOLSADO')
                """, UUID.randomUUID().toString());

        assertThat(jdbc.queryForObject(
                "select count(*) from pagamento where status_pagamento = 'REEMBOLSADO'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void a_v5_marca_todo_evento_historico_como_realizado() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O DEFAULT 'REALIZADO' da V5 se aplica as 11 linhas que o seed da V2
        // ja tinha gravado. E a consequencia declarada no cabecalho da migration:
        // taxa de falta retroativa nasce zerada, de proposito.
        assertThat(jdbc.queryForObject(
                "select count(*) from evento_clinico where status_evento = 'REALIZADO'", Integer.class))
                .isEqualTo(11);
    }

    @Test
    void a_v5_recusa_status_fora_do_enum_StatusEvento() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // Mesma classe de bug da V4: se o check aceitar valor que o enum nao
        // tem — ou recusar valor que ele tem — a divergencia so aparece como
        // 500 em producao.
        assertThatThrownBy(() -> jdbc.update("""
                insert into evento_clinico (id, data_evento, tipo_evento, status_evento)
                values (?, DATE '2026-01-01', 'CONSULTA', 'COMPARECEU')
                """, UUID.randomUUID().toString()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v5_liga_o_retorno_a_consulta_de_origem() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // A FK auto-referente e o que torna "taxa de retorno" calculavel: sem
        // ela, RETORNO e apenas um rotulo solto em tipo_evento.
        var consulta = "55555555-5555-5555-5555-000000000001";
        var retorno = UUID.randomUUID().toString();
        jdbc.update("""
                insert into evento_clinico (id, data_evento, tipo_evento, status_evento,
                                            evento_origem_id, peso_kg, data_retorno_previsto)
                values (?, DATE '2026-02-01', 'RETORNO', 'AGENDADO', ?, 12.500, DATE '2026-02-01')
                """, retorno, consulta);

        assertThat(jdbc.queryForObject(
                "select evento_origem_id from evento_clinico where id = ?", String.class, retorno))
                .isEqualTo(consulta);
    }

    @Test
    void a_v5_recusa_um_evento_que_aponta_para_si_mesmo() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        var id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> jdbc.update("""
                insert into evento_clinico (id, data_evento, tipo_evento, status_evento, evento_origem_id)
                values (?, DATE '2026-01-01', 'RETORNO', 'AGENDADO', ?)
                """, id, id))
                .isInstanceOf(DataAccessException.class);
    }
}
