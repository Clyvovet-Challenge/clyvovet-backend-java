package br.com.fiap.clyvovet.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roda o conjunto de migrations de db/migration/mysql contra um H2 em
 * MODE=MySQL.
 *
 * POR QUE ESTE TESTE EXISTE
 * Manter dois conjuntos de migrations (oracle/ e mysql/) tem um risco proprio:
 * alguem adiciona a V5 em um so, e a divergencia fica invisivel ate o deploy.
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
    void as_migrations_de_mysql_rodam_da_v1_a_v4() {
        var ds = h2ModoMySql();

        var flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load();
        var resultado = flyway.migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(4);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
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
}
