package br.com.fiap.clyvovet.repository;

/**
 * O texto que a pessoa digita num filtro, pronto para o {@code LIKE ... ESCAPE '\'}
 * das consultas {@code buscarPorFiltros}.
 *
 * <p>O {@code ESCAPE} so declara qual e o caractere de escape; quem tem de usa-lo
 * e quem monta o parametro. Sem isto, "%" e "_" digitados iam crus para o LIKE e
 * viravam curinga — buscar "%" devolvia a tabela inteira. A barra vem primeiro:
 * escapada depois, ela escaparia as barras que acabaram de ser postas.</p>
 */
public final class TextoDeBusca {

    private TextoDeBusca() {
    }

    /** O texto como literal do LIKE; nulo continua nulo, porque nulo desliga o filtro. */
    public static String literal(String digitado) {
        if (digitado == null) {
            return null;
        }
        return digitado.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
