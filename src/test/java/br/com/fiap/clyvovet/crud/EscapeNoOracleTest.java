package br.com.fiap.clyvovet.crud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere no Oracle de verdade a semantica que motivou o {@code ESCAPE '\'} das
 * queries de filtro.
 *
 * O defeito era este: o Hibernate emite {@code LIKE ... ESCAPE ''} e, na
 * semantica do Oracle, string vazia E nulo — o predicado vira {@code ESCAPE
 * NULL}, avalia como desconhecido e nunca e verdadeiro. O H2 com MODE=Oracle
 * reproduz isso, e e onde a correcao foi verificada; falta o banco real, que e
 * o alvo de entrega.
 *
 * Por que JDBC puro, e nao um @SpringBootTest com o perfil oracle:
 *
 *   - subir o contexto rodaria o Flyway (V3 e V4) e o ddl-auto=validate contra
 *     um banco compartilhado de sala de aula — alterar schema alheio para
 *     conferir um comportamento de SQL e desproporcional;
 *   - a suite inteira GRAVA (cadastros, eventos, usuarios) e depende do
 *     DevDataSeeder, que so existe nos perfis dev e h2. Sem ele nao ha usuario
 *     para logar, e quase todo teste falharia por 401 — por um motivo que nao
 *     tem nada a ver com o ESCAPE.
 *
 * As consultas aqui rodam sobre {@code dual}: nao leem nem escrevem uma linha
 * sequer das tabelas do projeto, e por isso independem de o seed estar aplicado.
 *
 * So executa quando DB_USERNAME esta no ambiente; sem isso o JUnit pula, e o
 * {@code mvn test} de sempre continua rodando sem banco externo.
 */
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class EscapeNoOracleTest {

    private static final String URL_PADRAO = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl";

    /** Resultado da avaliacao de um predicado: casou, ou falhou com este erro. */
    private record Avaliacao(boolean casou, String erro) {

        static Avaliacao recusada(String erro) {
            return new Avaliacao(false, erro);
        }
    }

    private Connection conectar() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", URL_PADRAO);
        return DriverManager.getConnection(url, System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
    }

    /**
     * Avalia um predicado LIKE isolado. Erro do banco conta como "nao casou":
     * para o efeito que se investiga — filtro que nao encontra nada — recusar a
     * consulta e nunca ser verdadeiro dao no mesmo.
     */
    private Avaliacao avaliar(String predicado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dual WHERE " + predicado;
        try (Connection conexao = conectar();
             Statement statement = conexao.createStatement()) {
            try (ResultSet resultado = statement.executeQuery(sql)) {
                resultado.next();
                return new Avaliacao(resultado.getInt(1) > 0, null);
            } catch (SQLException e) {
                return Avaliacao.recusada(e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("com ESCAPE '\\' explicito, o LIKE casa — e a correcao aplicada")
    void escapeExplicitoCasa() throws SQLException {
        Avaliacao avaliacao = avaliar("LOWER('Camila Ferreira') LIKE LOWER('%' || 'camila' || '%') ESCAPE '\\'");

        assertThat(avaliacao.casou())
                .as("erro do banco: %s", avaliacao.erro())
                .isTrue();
    }

    @Test
    @DisplayName("com ESCAPE '' — o que o Hibernate emitia — o LIKE nunca casa")
    void escapeVazioNaoCasa() throws SQLException {
        Avaliacao avaliacao = avaliar("LOWER('Camila Ferreira') LIKE LOWER('%' || 'camila' || '%') ESCAPE ''");

        assertThat(avaliacao.casou()).isFalse();
    }

    @Test
    @DisplayName("sem clausula ESCAPE o LIKE casa, o que isola a causa no ESCAPE vazio")
    void semClausulaEscapeCasa() throws SQLException {
        Avaliacao avaliacao = avaliar("LOWER('Camila Ferreira') LIKE LOWER('%' || 'camila' || '%')");

        assertThat(avaliacao.casou())
                .as("erro do banco: %s", avaliacao.erro())
                .isTrue();
    }

    @Test
    @DisplayName("o escape continua escapando: '%' literal nao vira curinga")
    void escapeSegueEscapando() throws SQLException {
        // Com a barra na frente, o % e texto: '100%' casa, '100 reais' nao.
        assertThat(avaliar("'100%' LIKE '%100\\%%' ESCAPE '\\'").casou()).isTrue();
        assertThat(avaliar("'100 reais' LIKE '%100\\%%' ESCAPE '\\'").casou()).isFalse();
    }
}
