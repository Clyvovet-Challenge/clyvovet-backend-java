package br.com.fiap.clyvovet.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda o documentos/script_bd.sql contra a defasagem.
 *
 * A disciplina de DevOps pede o DDL num arquivo separado, mas a fonte da
 * verdade são as migrations. Sem este teste, o script envelheceria no primeiro
 * ALTER que ninguém regerasse — e um DDL desatualizado é pior que nenhum:
 * quem o executa acha que provisionou o banco certo.
 */
class ScriptDoBancoTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration/oracle");
    private static final Path SCRIPT = Path.of("documentos/script_bd.sql");
    private static final Pattern VERSAO = Pattern.compile("V(\\d+)__");

    @Test
    @DisplayName("o script_bd.sql contém todas as migrations, na ordem")
    void scriptCobreTodasAsMigrations() throws IOException {
        String script = Files.readString(SCRIPT);

        List<Path> migrations = migrationsEmOrdem();
        assertThat(migrations).isNotEmpty();

        int posicaoAnterior = -1;
        for (Path migration : migrations) {
            String nome = migration.getFileName().toString().replace(".sql", "");
            int posicao = script.indexOf(nome);

            assertThat(posicao)
                    .as("%s não está no script_bd.sql. Rode: python scripts/gerar-script-bd.py", nome)
                    .isGreaterThan(-1);
            assertThat(posicao)
                    .as("%s está fora de ordem no script_bd.sql", nome)
                    .isGreaterThan(posicaoAnterior);
            posicaoAnterior = posicao;
        }
    }

    @Test
    @DisplayName("o conteúdo de cada migration está no script, não só o nome")
    void scriptTrazOConteudoEnaoSoOsTitulos() throws IOException {
        String script = Files.readString(SCRIPT);

        for (Path migration : migrationsEmOrdem()) {
            // Uma linha de DDL de cada arquivo. Comparar o conteúdo inteiro
            // seria frágil por causa de espaço em branco; achar um comando real
            // prova que o corpo foi copiado, e não apenas o cabeçalho.
            String corpo = Files.readString(migration);
            String comando = primeiraLinhaDeDdl(corpo);
            if (comando == null) {
                continue;
            }
            assertThat(script)
                    .as("o corpo de %s não está no script_bd.sql", migration.getFileName())
                    .contains(comando);
        }
    }

    private List<Path> migrationsEmOrdem() throws IOException {
        try (var arquivos = Files.list(MIGRATIONS)) {
            return arquivos
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparingInt(ScriptDoBancoTest::versaoDe))
                    .toList();
        }
    }

    private static int versaoDe(Path caminho) {
        Matcher m = VERSAO.matcher(caminho.getFileName().toString());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String primeiraLinhaDeDdl(String corpo) {
        return corpo.lines()
                .map(String::strip)
                .filter(l -> l.startsWith("CREATE TABLE") || l.startsWith("ALTER TABLE")
                        || l.startsWith("INSERT INTO"))
                .findFirst()
                .orElse(null);
    }
}
