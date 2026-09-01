package br.com.fiap.clyvovet.model;

/**
 * Ciclo de vida de um atendimento, junto das transicoes que cada estado admite.
 *
 * AGENDADO e o estado em que o evento nasce quando o TUTOR marca a consulta;
 * REALIZADO, quando o veterinario conclui o atendimento; FALTOU sai da varredura
 * de vencidos; CANCELADO, do cancelamento. Regras R1 a R5 e R11 da spec 08.
 *
 * A tabela mora aqui, e nao espalhada nos ifs de cada service, porque a mesma
 * pergunta e feita em tres lugares: a conclusao (RetornoService), o cancelamento
 * (AgendamentoService) e os links HATEOAS (LinksDoEvento). Enquanto cada um
 * decidia por conta propria, FALTOU aceitava conclusao no service enquanto o
 * HATEOAS ja o tratava como estado terminal — e concluir uma falta registrava no
 * historico clinico uma vacina que nao foi aplicada.
 *
 * Estado novo entra como uma linha, e a linha nao compila sem responder as tres
 * colunas. E esse o ponto: a celula esquecida deixa de ser possivel.
 */
public enum StatusEvento {

    //         concluir  cancelar  retorno   impedimento para concluir
    AGENDADO ( true,     true,     false,    null),
    REALIZADO( false,    false,    true,     "Este atendimento já foi concluído"),
    FALTOU   ( false,    false,    false,    "Um atendimento marcado como falta não pode ser concluído"),
    CANCELADO( false,    false,    false,    "Um atendimento cancelado não pode ser concluído");

    private final boolean podeConcluir;
    private final boolean podeCancelar;
    private final boolean podeGerarRetorno;
    private final String impedimentoParaConcluir;

    StatusEvento(boolean podeConcluir, boolean podeCancelar,
                 boolean podeGerarRetorno, String impedimentoParaConcluir) {
        this.podeConcluir = podeConcluir;
        this.podeCancelar = podeCancelar;
        this.podeGerarRetorno = podeGerarRetorno;
        this.impedimentoParaConcluir = impedimentoParaConcluir;
    }

    /** AGENDADO -> REALIZADO, unica porta de entrada do registro clinico. */
    public boolean podeConcluir() {
        return podeConcluir;
    }

    /** AGENDADO -> CANCELADO: so se desmarca o que ainda nao aconteceu. */
    public boolean podeCancelar() {
        return podeCancelar;
    }

    /** REALIZADO gera um novo evento de RETORNO (R11): so se volta do que houve. */
    public boolean podeGerarRetorno() {
        return podeGerarRetorno;
    }

    /** Mensagem do 400 quando {@link #podeConcluir()} e falso; null quando e verdadeiro. */
    public String impedimentoParaConcluir() {
        return impedimentoParaConcluir;
    }
}
