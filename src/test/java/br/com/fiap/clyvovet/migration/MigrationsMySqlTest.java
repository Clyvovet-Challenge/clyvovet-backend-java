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

    // Ids do seed da V2. Repetidos como literal, viravam numero magico
    // em cada teste novo.
    private static final String CLINICA_VETCARE   = "11111111-1111-1111-1111-000000000001";
    private static final String TUTOR_LUCAS       = "22222222-2222-2222-2222-000000000001";
    private static final String VET_CAMILA        = "33333333-3333-3333-3333-000000000001";
    private static final String ANIMAL_BOLINHA    = "44444444-4444-4444-4444-000000000001";

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
    void as_migrations_de_mysql_rodam_da_v1_a_v15() {
        var ds = h2ModoMySql();

        var flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load();
        var resultado = flyway.migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(15);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("15");
    }

    /**
     * A V14 promete o que nenhuma coluna nova prova sozinha: que o texto que ja
     * estava gravado FOI RECONCILIADO com o catalogo.
     *
     * Criar a tabela e facil; o risco esta na reconciliacao, que casa grafias
     * diferentes ("Siames" sem acento contra "Siames" com) e sinonimos ("Vira",
     * "Vira-lata" e "SRD" para a mesma ausencia de raca). Se ela falhasse em
     * silencio, o catalogo existiria vazio de ligacoes e ninguem notaria -- os
     * animais continuariam com o texto antigo e o app cairia no caso "sem arte"
     * para todo mundo.
     */
    @Test
    void a_v14_liga_os_animais_do_seed_ao_catalogo() {
        var ds = h2ModoMySql();
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();

        var jdbc = new JdbcTemplate(ds);

        // o catalogo entrou inteiro, e cada chave e unica
        Integer racas = jdbc.queryForObject("SELECT COUNT(*) FROM t_clyvo_raca", Integer.class);
        Integer chaves = jdbc.queryForObject("SELECT COUNT(DISTINCT chave) FROM t_clyvo_raca", Integer.class);
        assertThat(racas).isGreaterThan(40);
        assertThat(chaves).isEqualTo(racas);

        // todo animal do seed encontrou a sua raca -- nenhum sobrou sem ligacao
        Integer semLigacao = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_clyvo_animal WHERE raca_id IS NULL", Integer.class);
        assertThat(semLigacao).isZero();

        // e o acento foi reescrito: o seed gravou "Siames", o catalogo diz "Siames"
        // com acento, e depois da V14 o que esta na tabela e o do catalogo
        Integer semAcento = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_clyvo_animal WHERE raca = 'Siames'", Integer.class);
        assertThat(semAcento).isZero();

        // a especie parou de ter tres grafias para a mesma coisa
        Integer especies = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT especie) FROM t_clyvo_animal", Integer.class);
        assertThat(especies).isLessThanOrEqualTo(5);
    }

    /**
     * A V15 promete tres coisas, e as tres precisam de prova de CONTEUDO, nao
     * so de schema: o widget quebrado da .NET foi consertado (a coluna que a V8
     * esqueceu existe E a tabela deixou de estar vazia), a base agregada de
     * doencas entrou com linhas ligadas ao catalogo da V14, e o cache de
     * parecer aceita exatamente um parecer por animal.
     */
    @Test
    void a_v15_conserta_o_widget_e_semeia_a_base_de_doencas() {
        var ds = h2ModoMySql();
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(ds);

        // o conserto: criado_em existe (a consulta nao explode) e o seed de 42
        // predisposicoes do arquivo original da .NET finalmente foi portado
        Integer predisposicoes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_clyvo_predisposicao_saude WHERE criado_em IS NOT NULL",
                Integer.class);
        assertThat(predisposicoes).isEqualTo(42);

        // a base agregada tem as quatro especies dos datasets...
        Integer especiesNaBase = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT especie) FROM t_clyvo_base_doencas", Integer.class);
        assertThat(especiesNaBase).isEqualTo(4);

        // ...e toda raca_chave preenchida aponta para uma chave REAL do
        // catalogo da V14 -- e o que permite a .NET casar por igualdade
        Integer chavesOrfas = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM t_clyvo_base_doencas b
                 WHERE b.raca_chave IS NOT NULL
                   AND NOT EXISTS (SELECT 1 FROM t_clyvo_raca r WHERE r.chave = b.raca_chave)
                """, Integer.class);
        assertThat(chavesOrfas).isZero();

        // linha sem caso nenhum nao afirma predisposicao -- nao pode existir
        Integer soControles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_clyvo_base_doencas WHERE casos = 0", Integer.class);
        assertThat(soControles).isZero();

        // o cache: um parecer por animal, o segundo INSERT do mesmo animal morre
        jdbc.update("""
                INSERT INTO t_clyvo_parecer_ia (id, animal_id, origem, conteudo, gerado_em, valido_ate)
                VALUES ('aaaa0000-0000-0000-0000-000000000001', ?, 'REGRAS', '{}',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ANIMAL_BOLINHA);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO t_clyvo_parecer_ia (id, animal_id, origem, conteudo, gerado_em, valido_ate)
                VALUES ('aaaa0000-0000-0000-0000-000000000002', ?, 'IA', '{}',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, ANIMAL_BOLINHA))
                .isInstanceOf(DataAccessException.class);
    }

    /**
     * A V13 promete duas coisas: as colunas existem, e as cinco clinicas do
     * seed sairam com coordenada. A segunda parte e a que importa -- coluna
     * criada e seed vazio deixaria o mapa em branco sem erro nenhum, que e
     * exatamente o tipo de falha silenciosa que este projeto persegue.
     */
    @Test
    void a_v13_da_coordenada_as_cinco_clinicas_do_seed() {
        var ds = h2ModoMySql();
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(ds);

        assertThat(jdbc.queryForObject(
                "select count(*) from t_clyvo_clinica where latitude is not null and longitude is not null",
                Integer.class)).isEqualTo(5);

        // Sao Paulo fica no terceiro quadrante: latitude negativa, longitude
        // negativa. Se um par entrar trocado de lugar, a clinica vai para o
        // Oriente Medio e este limite pega.
        assertThat(jdbc.queryForObject(
                "select count(*) from t_clyvo_clinica "
                        + "where latitude between -24 and -23 and longitude between -47 and -46",
                Integer.class)).isEqualTo(5);
    }

    @Test
    void o_seed_carrega_os_mesmos_registros_do_conjunto_oracle() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O seed da V2 e identico nos dois conjuntos; se alguem mexer em um so,
        // estas contagens deixam de bater. Os numeros vem do proprio arquivo --
        // a disciplina exige no MINIMO 5 por tabela, e algumas tem mais.
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_clinica", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_tutor", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_animal", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_veterinario", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_evento_clinico", Integer.class)).isEqualTo(11);
        assertThat(jdbc.queryForObject("select count(*) from t_clyvo_pagamento", Integer.class)).isEqualTo(8);
    }

    @Test
    void a_v4_deixa_o_check_de_status_alinhado_ao_enum() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O ponto da V4: REEMBOLSADO passa a ser aceito. Antes dela o INSERT
        // estourava violacao de check e virava 500 na API.
        jdbc.update("""
                insert into t_clyvo_pagamento (id, metodo_pagamento, valor, data_pagamento, status_pagamento)
                values (?, 'PIX', 10.00, DATE '2026-01-01', 'REEMBOLSADO')
                """, UUID.randomUUID().toString());

        assertThat(jdbc.queryForObject(
                "select count(*) from t_clyvo_pagamento where status_pagamento = 'REEMBOLSADO'", Integer.class))
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
                "select count(*) from t_clyvo_evento_clinico where status_evento = 'REALIZADO'", Integer.class))
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
                insert into t_clyvo_evento_clinico (id, data_evento, tipo_evento, status_evento)
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
                insert into t_clyvo_evento_clinico (id, data_evento, tipo_evento, status_evento,
                                            evento_origem_id, peso_kg, data_retorno_previsto)
                values (?, DATE '2026-02-01', 'RETORNO', 'AGENDADO', ?, 12.500, DATE '2026-02-01')
                """, retorno, consulta);

        assertThat(jdbc.queryForObject(
                "select evento_origem_id from t_clyvo_evento_clinico where id = ?", String.class, retorno))
                .isEqualTo(consulta);
    }

    @Test
    void a_v5_recusa_um_evento_que_aponta_para_si_mesmo() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        var id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_evento_clinico (id, data_evento, tipo_evento, status_evento, evento_origem_id)
                values (?, DATE '2026-01-01', 'RETORNO', 'AGENDADO', ?)
                """, id, id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_aceita_um_servico_no_catalogo_da_clinica() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        var id = UUID.randomUUID().toString();
        // O nome carrega o id porque uk_servico_clinica_nome e UNIQUE (clinica_id,
        // nome), e a V11 semeia catalogo. Um literal fixo aqui faria este teste --
        // que verifica a DDL da V6, e nao o conteudo do catalogo -- passar a
        // depender de nenhuma migracao futura escolher o mesmo nome.
        jdbc.update("""
                insert into t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos)
                values (?, ?, ?, 'CONSULTA', 180.00, 30)
                """, id, CLINICA_VETCARE, "Servico de teste " + id);

        assertThat(jdbc.queryForObject(
                "select duracao_minutos from t_clyvo_servico where id = ?", Integer.class, id))
                .isEqualTo(30);
        // O DEFAULT de ativo precisa valer nos dois dialetos: e ele que decide
        // se um servico recem-criado aparece para agendamento.
        assertThat(jdbc.queryForObject(
                "select ativo from t_clyvo_servico where id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void a_v6_recusa_duracao_de_servico_fora_da_faixa() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // Duracao zero produziria colisao de agenda impossivel de resolver: dois
        // atendimentos ocupando o mesmo instante sem se sobrepor.
        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_servico (id, clinica_id, nome, tipo_evento, preco, duracao_minutos)
                values (?, ?, 'Servico invalido', 'CONSULTA', 100.00, 0)
                """, UUID.randomUUID().toString(), CLINICA_VETCARE))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_recusa_faixa_de_disponibilidade_invertida() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // A comparacao de 'HH:mm' como texto so funciona porque o formato e de
        // largura fixa com zero a esquerda. Se alguem gravar '9:00' em vez de
        // '09:00', a ordenacao lexicografica mente -- e este check para de
        // proteger. O @Pattern no DTO e o que garante o formato na entrada.
        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_disponibilidade_vet
                    (id, veterinario_id, dia_semana, hora_inicio, hora_fim, vigencia_inicio)
                values (?, ?, 'SEGUNDA', '18:00', '08:00', DATE '2026-01-01')
                """, UUID.randomUUID().toString(), VET_CAMILA))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_exige_as_duas_horas_do_bloqueio_ou_nenhuma() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // Ferias sao dias inteiros (as duas horas nulas); almoco e uma faixa
        // (as duas preenchidas). Meia hora preenchida nao significa nada, e o
        // codigo que le a agenda teria de adivinhar o que fazer com ela.
        jdbc.update("""
                insert into t_clyvo_bloqueio (id, veterinario_id, data_inicio, data_fim, motivo)
                values (?, ?, DATE '2026-07-01', DATE '2026-07-15', 'Ferias')
                """, UUID.randomUUID().toString(), VET_CAMILA);

        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_bloqueio (id, veterinario_id, data_inicio, data_fim, hora_inicio, motivo)
                values (?, ?, DATE '2026-07-01', DATE '2026-07-01', '12:00', 'Almoco pela metade')
                """, UUID.randomUUID().toString(), VET_CAMILA))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_deixa_varios_animais_sem_microchip_conviverem() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // O comportamento contra-intuitivo que o cabecalho da migration
        // registra: um indice UNIQUE ignora as linhas com NULL, nos dois
        // bancos. Sem isso, o segundo animal sem chip seria recusado -- e
        // chip e opcional.
        jdbc.update("insert into t_clyvo_animal (id, nome, tutor_id) values (?, 'Sem chip 1', ?)",
                UUID.randomUUID().toString(), TUTOR_LUCAS);
        jdbc.update("insert into t_clyvo_animal (id, nome, tutor_id) values (?, 'Sem chip 2', ?)",
                UUID.randomUUID().toString(), TUTOR_LUCAS);

        assertThat(jdbc.queryForObject(
                "select count(*) from t_clyvo_animal where microchip is null", Integer.class))
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void a_v6_recusa_dois_animais_com_o_mesmo_microchip() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        jdbc.update("insert into t_clyvo_animal (id, nome, tutor_id, microchip) values (?, 'Thor', ?, '900000000000001')",
                UUID.randomUUID().toString(), TUTOR_LUCAS);

        // Chip duplicado significaria dois animais com a mesma identidade no
        // balcao -- e o resumo de seguranca do errado.
        assertThatThrownBy(() -> jdbc.update(
                "insert into t_clyvo_animal (id, nome, tutor_id, microchip) values (?, 'Clone', ?, '900000000000001')",
                UUID.randomUUID().toString(), TUTOR_LUCAS))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_recusa_desfecho_fora_do_enum_Desfecho() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_evento_clinico (id, data_evento, tipo_evento, status_evento, desfecho)
                values (?, DATE '2026-01-01', 'CONSULTA', 'REALIZADO', 'CURADO')
                """, UUID.randomUUID().toString()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void a_v6_aceita_evento_sem_desfecho() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // Nulo e o estado de todo atendimento em aberto. Se o check exigisse
        // valor, nenhum evento poderia ser agendado -- so concluido.
        var id = UUID.randomUUID().toString();
        jdbc.update("""
                insert into t_clyvo_evento_clinico (id, data_evento, tipo_evento, status_evento)
                values (?, DATE '2026-12-01', 'CONSULTA', 'AGENDADO')
                """, id);

        assertThat(jdbc.queryForObject(
                "select desfecho from t_clyvo_evento_clinico where id = ?", String.class, id))
                .isNull();
    }

    @Test
    void a_v6_distingue_a_origem_do_alerta_clinico() {
        var ds = h2ModoMySql();
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load().migrate();
        var jdbc = new JdbcTemplate(ds);

        // "o tutor disse que tem alergia" e "o veterinario registrou anafilaxia"
        // pesam diferente na decisao clinica, e quem le o resumo de seguranca
        // precisa saber qual dos dois esta lendo.
        var id = UUID.randomUUID().toString();
        jdbc.update("""
                insert into t_clyvo_alerta_clinico (id, animal_id, tipo, descricao, origem)
                values (?, ?, 'ALERGIA', 'Anafilaxia a dipirona', 'VETERINARIO')
                """, id, ANIMAL_BOLINHA);

        assertThat(jdbc.queryForObject(
                "select origem from t_clyvo_alerta_clinico where id = ?", String.class, id))
                .isEqualTo("VETERINARIO");

        assertThatThrownBy(() -> jdbc.update("""
                insert into t_clyvo_alerta_clinico (id, animal_id, tipo, descricao, origem)
                values (?, ?, 'ALERGIA', 'Origem inventada', 'RECEPCIONISTA')
                """, UUID.randomUUID().toString(), ANIMAL_BOLINHA))
                .isInstanceOf(DataAccessException.class);
    }
}
