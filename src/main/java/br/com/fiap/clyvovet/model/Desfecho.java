package br.com.fiap.clyvovet.model;

/**
 * Resultado clinico de um atendimento.
 *
 * Sem ele nao existe a pergunta "onde a clinica tem mais sucesso e mais
 * fracasso" — que e o que a visao de produto pede do painel. INDEFINIDO nao e
 * o mesmo que nulo: nulo significa "atendimento ainda nao concluido", e
 * INDEFINIDO significa "concluido, e o veterinario nao soube classificar".
 *
 * O rotulo e o que a tela mostra. O nome da constante continua sendo o que vai
 * para a API e para o banco: "ESTAVEL" sem acento e contrato, "Estável" e texto.
 */
public enum Desfecho {
    MELHORA("Melhora"),
    ESTAVEL("Estável"),
    PIORA("Piora"),
    OBITO("Óbito"),
    INDEFINIDO("Indefinido");

    private final String rotulo;

    Desfecho(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
